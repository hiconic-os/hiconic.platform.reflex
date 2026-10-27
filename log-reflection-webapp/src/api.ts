import {
  CreateLogBundle,
  GetLogReflectionCapabilities,
  GetLogTopology,
  ListLogStreams,
  LogBundleFormat,
  LogBundleLayout,
  LogBundleScope,
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
import { GetLogLevelState, UpdateRuntimeLogLevels, type LogLevelState } from "@dev.hiconic/gm_log-level-api-model";
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

export type RemoteLogScope = "current" | "all" | "range";
export type RemoteLogLayout = "separate" | "merged";
export type RemoteLogFormat = "natural" | "canonical" | "jsonl";

export interface RemoteLogDownload {
  target: TargetSelection;
  streamIds: readonly string[];
  scope: RemoteLogScope;
  layouts: readonly RemoteLogLayout[];
  formats: readonly RemoteLogFormat[];
  from?: string;
  to?: string;
}

export interface LogLevelStateView {
  effectiveLevels: ReadonlyMap<string, string>;
  runtimeLevels: ReadonlyMap<string, string>;
  packagedLevels: ReadonlyMap<string, string>;
  knownLoggerNames: readonly string[];
}

export interface LogReflectionClient {
  capabilities(): Promise<LogReflectionCapabilities>;
  streams(target: TargetSelection): Promise<LogStreams>;
  topology(): Promise<LogTopology>;
  logLevelState(applicationId: string): Promise<LogLevelStateView>;
  setRuntimeLogLevel(applicationId: string, loggerName: string, level: string): Promise<void>;
  resetRuntimeLogLevel(applicationId: string, loggerName: string): Promise<void>;
  clearRuntimeLogLevels(applicationId: string): Promise<void>;
  records(
    streamId: string,
    target: TargetSelection,
    filter: RecordFilter,
    cursor: string | undefined,
    beforeCursor: string | undefined,
    waitMillis: number,
    includeRotated?: boolean
  ): Promise<LogRecordPage>;
  bundle(download: RemoteLogDownload): Promise<{ blob: Blob; name: string }>;
}

export class LogReflectionApi implements LogReflectionClient {
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

  async logLevelState(applicationId: string): Promise<LogLevelStateView> {
    const request = GetLogLevelState.create();
    request.applicationId = applicationId;
    const state = await this.evaluate<LogLevelState>(request, this.loggingEvaluator);
    return {
      effectiveLevels: new Map(state.effectiveLevels?.entries() ?? []),
      runtimeLevels: new Map(state.runtimeLevels?.entries() ?? []),
      packagedLevels: new Map(state.packagedLevels?.entries() ?? []),
      knownLoggerNames: (Array.from(state.knownLoggerNames ?? []) as string[]).sort()
    };
  }

  async setRuntimeLogLevel(applicationId: string, loggerName: string, level: string): Promise<void> {
    const request = UpdateRuntimeLogLevels.create();
    request.applicationId = applicationId;
    request.levels = stringMap([[loggerName, level]]);
    request.namesToRemove = stringSet([]);
    await this.evaluate(request, this.loggingEvaluator);
  }

  async resetRuntimeLogLevel(applicationId: string, loggerName: string): Promise<void> {
    const request = UpdateRuntimeLogLevels.create();
    request.applicationId = applicationId;
    request.levels = stringMap([]);
    request.namesToRemove = stringSet([loggerName]);
    await this.evaluate(request, this.loggingEvaluator);
  }

  async clearRuntimeLogLevels(applicationId: string): Promise<void> {
    const request = UpdateRuntimeLogLevels.create();
    request.applicationId = applicationId;
    request.clearAll = true;
    request.levels = stringMap([]);
    request.namesToRemove = stringSet([]);
    await this.evaluate(request, this.loggingEvaluator);
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
    request.waitMillis = BigInt(waitMillis);
    request.includeRotated = includeRotated;
    return this.evaluate(request, this.loggingEvaluator);
  }

  async bundle(download: RemoteLogDownload): Promise<{
    blob: Blob;
    name: string;
  }> {
    const request = CreateLogBundle.create();
    request.target = toTarget(download.target);
    const ids = new T.Set(hc.reflection.STRING);
    download.streamIds.forEach(id => ids.add(id));
    request.streamIds = ids;
    request.scope = download.scope === "current"
      ? LogBundleScope.CURRENT
      : download.scope === "range" ? LogBundleScope.CUSTOM_RANGE : LogBundleScope.ALL;

    const layouts = new T.Set(LogBundleLayout[hc.Symbol.enumType]);
    download.layouts.forEach(layout => layouts.add(layout === "merged" ? LogBundleLayout.MERGED : LogBundleLayout.SEPARATE));
    request.layouts = layouts;

    const formats = new T.Set(LogBundleFormat[hc.Symbol.enumType]);
    download.formats.forEach(format => formats.add(format === "jsonl"
      ? LogBundleFormat.CANONICAL_JSONL
      : format === "canonical" ? LogBundleFormat.CANONICAL_TEXT : LogBundleFormat.NATURAL));
    request.formats = formats;
    request.from = download.scope === "range" && download.from ? new Date(download.from) : null;
    request.to = download.scope === "range" && download.to ? new Date(download.to) : null;
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

function stringMap(entries: readonly (readonly [string, string])[]) {
  const result = new T.Map(hc.reflection.STRING, hc.reflection.STRING);
  entries.forEach(([key, value]) => result.set(key, value));
  return result;
}

function stringSet(values: readonly string[]) {
  const result = new T.Set(hc.reflection.STRING);
  values.forEach(value => result.add(value));
  return result;
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
