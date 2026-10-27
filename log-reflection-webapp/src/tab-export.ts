import type { LogRecord } from "./api";

export type TabExportFormat = "text" | "jsonl" | "spreadsheet";

export type TabExportColumnKey =
  | "timestamp"
  | "level"
  | "application"
  | "node"
  | "stream"
  | "position"
  | "logger"
  | "thread"
  | "source"
  | "message"
  | "properties"
  | "raw";

export interface TabExportColumn {
  key: TabExportColumnKey;
  label: string;
}

export interface TabExport {
  content: string;
  mimeType: string;
  extension: string;
}

export function createTabExport(
  records: readonly LogRecord[],
  columns: readonly TabExportColumn[],
  format: TabExportFormat,
  worksheetName: string
): TabExport {
  switch (format) {
    case "text":
      return {
        content: records.map(record => columns.map(column => textValue(record, column.key)).join("  ")).join("\n"),
        mimeType: "text/plain;charset=utf-8",
        extension: "txt"
      };
    case "jsonl":
      return {
        content: records.map(record => JSON.stringify(Object.fromEntries(columns.map(column => [
          column.key,
          jsonValue(record, column.key)
        ])))).join("\n"),
        mimeType: "application/x-ndjson;charset=utf-8",
        extension: "jsonl"
      };
    case "spreadsheet":
      return {
        content: spreadsheetXml(records, columns, worksheetName),
        mimeType: "application/vnd.ms-excel;charset=utf-8",
        extension: "xml"
      };
  }
}

function spreadsheetXml(records: readonly LogRecord[], columns: readonly TabExportColumn[], worksheetName: string) {
  const header = row(columns.map(column => cell(column.label, "String")), true);
  const rows = records.map(record => row(columns.map(column => {
    const value = jsonValue(record, column.key);
    if (column.key === "timestamp" && typeof value === "string" && value)
      return cell(value.replace(/Z$/, ""), "DateTime", "Date");
    return cell(textValue(record, column.key), "String");
  }), false)).join("");

  return `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<?mso-application progid="Excel.Sheet"?>\n` +
    `<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" ` +
    `xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet" xmlns:x="urn:schemas-microsoft-com:office:excel">` +
    `<Styles>` +
    `<Style ss:ID="Header"><Font ss:Bold="1"/><Interior ss:Color="#DCE6F1" ss:Pattern="Solid"/></Style>` +
    `<Style ss:ID="Wrapped"><Alignment ss:Vertical="Top" ss:WrapText="1"/></Style>` +
    `<Style ss:ID="Date"><NumberFormat ss:Format="yyyy-mm-dd hh:mm:ss.000"/></Style>` +
    `</Styles>` +
    `<Worksheet ss:Name="${xmlAttribute(safeWorksheetName(worksheetName))}">` +
    `<Table ss:ExpandedColumnCount="${columns.length}" ss:ExpandedRowCount="${records.length + 1}" x:FullColumns="1" x:FullRows="1">` +
    header + rows +
    `</Table></Worksheet></Workbook>`;
}

function row(cells: readonly string[], header: boolean) {
  return `<Row${header ? ' ss:StyleID="Header"' : ' ss:StyleID="Wrapped"'}>${cells.join("")}</Row>`;
}

function cell(value: string, type: "String" | "DateTime", style?: string) {
  return `<Cell${style ? ` ss:StyleID="${style}"` : ""}><Data ss:Type="${type}">${xmlText(value)}</Data></Cell>`;
}

function jsonValue(record: LogRecord, column: TabExportColumnKey): unknown {
  switch (column) {
    case "timestamp": return nativeDate(record.timestamp)?.toISOString() ?? null;
    case "level": return enumName(record.level) || null;
    case "application": return record.origin?.applicationId ?? null;
    case "node": return record.origin?.nodeId ?? null;
    case "stream": return record.streamId ?? null;
    case "position": return position(record);
    case "logger": return record.loggerName ?? null;
    case "thread": return record.threadName ?? null;
    case "source": return sourceLocation(record);
    case "message": return record.message ?? null;
    case "properties": return Object.fromEntries(mapEntries(record.properties));
    case "raw": return record.rawText ?? null;
  }
}

function textValue(record: LogRecord, column: TabExportColumnKey) {
  const value = jsonValue(record, column);
  if (value == null) return "";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function position(record: LogRecord) {
  const sequence = record.position?.sequence;
  const byteOffset = record.position?.byteOffset;
  if (sequence != null) return `seq:${sequence}`;
  if (byteOffset != null) return `byte:${byteOffset}`;
  return null;
}

function sourceLocation(record: LogRecord) {
  const source = record.sourceLocation;
  if (!source) return null;
  const member = [source.className, source.methodName].filter(Boolean).join(".");
  const file = source.fileName ? `${source.fileName}${source.lineNumber != null ? `:${source.lineNumber}` : ""}` : undefined;
  return [member, file].filter(Boolean).join(" · ") || null;
}

function mapEntries(value: unknown): [string, string][] {
  if (!value) return [];
  const modeledMap = value as { entries?: () => Iterable<[string | null, string | null]> };
  if (typeof modeledMap.entries === "function")
    return Array.from(modeledMap.entries(), ([name, item]) => [name ?? "", item ?? ""]);
  return Object.entries(value as Record<string, unknown>).map(([name, item]) => [name, item == null ? "" : String(item)]);
}

function enumName(value: unknown) {
  if (value == null) return "";
  if (typeof value === "string") return value;
  const candidate = value as { name?: () => string };
  return typeof candidate.name === "function" ? candidate.name() : String(value);
}

function nativeDate(value: unknown): Date | undefined {
  if (value == null) return undefined;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? undefined : value;
  if (typeof value === "number" || typeof value === "string") {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? undefined : date;
  }
  const candidate = value as { getTime?: () => number; valueOf?: () => unknown; toISOString?: () => string };
  if (typeof candidate.getTime === "function") {
    const date = new Date(candidate.getTime());
    return Number.isNaN(date.getTime()) ? undefined : date;
  }
  const primitive = candidate.valueOf?.();
  if (typeof primitive === "number" || typeof primitive === "string") {
    const date = new Date(primitive);
    return Number.isNaN(date.getTime()) ? undefined : date;
  }
  if (typeof candidate.toISOString === "function") {
    const date = new Date(candidate.toISOString());
    return Number.isNaN(date.getTime()) ? undefined : date;
  }
  return undefined;
}

function safeWorksheetName(value: string) {
  const safe = value.replace(/[\\/?*\[\]:]/g, " ").trim() || "Logs";
  return safe.substring(0, 31);
}

function xmlText(value: string) {
  return escapeXml(value.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, "�"));
}

function xmlAttribute(value: string) {
  return xmlText(value);
}

function escapeXml(value: string) {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&apos;");
}
