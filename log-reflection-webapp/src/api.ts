import {
  CreateLogBundle,
  GetLogReflectionCapabilities,
  GetLogTopology,
  ListLogStreams,
  LogBundleFormat,
  LogFilter,
  LogPropertyFilter,
  LogTarget,
  QueryLogRecords,
  type LogRecord,
  type LogReflectionCapabilities,
  type LogRecordPage,
  type LogTopology,
  type LogSegmentDescriptor,
  type LogStreamDescriptor,
  type LogStreams
} from "@dev.hiconic/platform.reflex_log-reflection-api-model";
import { LogLevel } from "@dev.hiconic/gm_logging-model";
import type { Resource } from "@dev.hiconic/gm_resource-model";
import { TransientSource } from "@dev.hiconic/gm_transient-resource-model";
import { T, hc, remote } from "@dev.hiconic/tf.js_hc-js-api";

export interface RuntimeConfig {
  servicesUrl: string;
  loginUrl?: string;
  readerRoles?: readonly string[];
  managerRoles?: readonly string[];
}

export interface TargetSelection {
  applicationId?: string;
  nodeId?: string;
}

export interface RecordFilter {
  from?: string;
  to?: string;
  levels: readonly string[];
  logger?: string;
  fulltext?: string;
}

export class LogReflectionApi {
  private readonly loggingEvaluator;

  constructor(readonly config: RuntimeConfig) {
    const servicesUrl = new URL(config.servicesUrl, window.location.origin).toString().replace(/\/$/, "");
    const connection = remote.connect(servicesUrl);
    this.loggingEvaluator = connection.evaluatorBuilder().setDefaultDomain("logging").build();
  }

  async capabilities(): Promise<LogReflectionCapabilities> {
    return this.evaluate(GetLogReflectionCapabilities.create(), this.loggingEvaluator);
  }

  async streams(target: TargetSelection): Promise<LogStreams> {
    const request = ListLogStreams.create();
    request.target = toTarget(target);
    return this.evaluate(request, this.loggingEvaluator);
  }

  async topology(): Promise<LogTopology> {
    return this.evaluate(GetLogTopology.create(), this.loggingEvaluator);
  }

  async records(
    streamId: string,
    target: TargetSelection,
    filter: RecordFilter,
    cursor: string | undefined,
    beforeCursor: string | undefined,
    waitMillis: number,
    includeRotated = false
  ): Promise<LogRecordPage> {
    const request = QueryLogRecords.create();
    request.streamId = streamId;
    request.target = toTarget(target);
    request.filter = toFilter(filter);
    request.cursor = cursor ?? null;
    request.beforeCursor = beforeCursor ?? null;
    request.limit = 300;
    request.waitMillis = waitMillis;
    request.includeRotated = includeRotated;
    return this.evaluate(request, this.loggingEvaluator);
  }

  async bundle(
    target: TargetSelection,
    streamIds: readonly string[],
    includeRotated: boolean,
    format: "raw" | "text" | "jsonl"
  ): Promise<{
    blob: Blob;
    name: string;
  }> {
    const request = CreateLogBundle.create();
    request.target = toTarget(target);
    const ids = new T.Set(hc.reflection.STRING);
    streamIds.forEach(id => ids.add(id));
    request.streamIds = ids;
    request.includeRotated = includeRotated;
    request.format = format === "jsonl"
      ? LogBundleFormat.CANONICAL_JSONL
      : format === "text" ? LogBundleFormat.CANONICAL_TEXT : LogBundleFormat.RAW_FILES;
    request.filter = null;

    const resource = await this.evaluate<Resource>(request, this.loggingEvaluator);
    const source = resource.resourceSource;
    if (!source || !TransientSource.isInstance(source))
      throw new Error("The log bundle response does not contain a transient Web-RPC resource.");
    return {
      blob: hc.resources.hasBlob(source as TransientSource),
      name: resource.name || "logs.zip"
    };
  }

  private async evaluate<T>(request: { EvalAndGetReasoned(evaluator: unknown): Promise<any> }, evaluator: unknown): Promise<T> {
    const result = await request.EvalAndGetReasoned(evaluator);
    if (result.isUnsatisfied()) {
      const reason = result.whyUnsatisfied();
      throw new Error(reason?.Stringify?.() ?? reason?.asString?.() ?? String(reason));
    }
    return result.get() as T;
  }
}

function toTarget(selection: TargetSelection) {
  if (!selection.applicationId && !selection.nodeId)
    return null;

  const target = LogTarget.create();
  target.applicationId = selection.applicationId ?? null;
  target.nodeId = selection.nodeId ?? null;
  return target;
}

function toFilter(source: RecordFilter) {
  const filter = LogFilter.create();
  filter.from = source.from ? new Date(source.from) : null;
  filter.to = source.to ? new Date(source.to) : null;

  const levels = new T.Set(LogLevel[hc.Symbol.enumType]);
  const levelsByName: Readonly<Record<string, LogLevel>> = {
    TRACE: LogLevel.TRACE,
    DEBUG: LogLevel.DEBUG,
    INFO: LogLevel.INFO,
    WARN: LogLevel.WARN,
    ERROR: LogLevel.ERROR
  };
  source.levels.forEach(level => levels.add(levelsByName[level]));
  filter.levels = levels;

  const loggerNames = new T.Set(hc.reflection.STRING);
  filter.loggerNames = loggerNames;
  filter.loggerNameContains = source.logger ?? null;
  filter.threadNames = new T.Set(hc.reflection.STRING);

  filter.fulltext = source.fulltext ?? null;
  filter.propertyFilters = new T.Array(LogPropertyFilter);
  return filter;
}

export type { LogRecord, LogRecordPage, LogSegmentDescriptor, LogStreamDescriptor };
