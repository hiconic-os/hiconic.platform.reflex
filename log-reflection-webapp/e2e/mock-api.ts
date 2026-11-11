import type {
  LogLevelStateView,
  LogRecord,
  LogRecordPage,
  LogReflectionClient,
  RecordFilter,
  RemoteLogDownload,
  TargetSelection
} from "../src/api";

type PendingTail = {
  filter: RecordFilter;
  includeRotated: boolean;
  resolve: (page: LogRecordPage) => void;
};

type QuerySnapshot = {
  streamId: string;
  filter: RecordFilter;
  includeRotated: boolean;
  waitMillis: number;
};

class MockLogReflectionApi implements LogReflectionClient {
  private authenticated = true;
  private manager = true;
  private readonly packagedProfiles = new Map<string, Map<string, string>>([
    ["packaged-default", new Map([["example.Logger", "INFO"], ["example.service", "INFO"]])],
    ["platform-debug", new Map([["example.Logger", "DEBUG"], ["example.Other", "WARN"]])]
  ]);
  private readonly runtimeProfiles = new Map<string, Map<string, string>>();
  private runtimeCascade: string[] = [];
  private runtimeCascadeActive = false;
  private sequence = 0;
  private immediateTailResponses = false;
  private tailResponseDelayMillis = 0;
  private backlogResponses = 0;
  private concurrentTailRequests = 0;
  private maximumConcurrentTailRequests = 0;
  private bundleDelayMillis = 0;
  private bundleRequestCount = 0;
  private readonly pending = new Map<string, PendingTail[]>();
  private readonly queries: QuerySnapshot[] = [];

  async capabilities() {
    return {
      authenticated: this.authenticated,
      canRead: this.authenticated,
      canManageLevels: this.authenticated && this.manager
    } as any;
  }

  async topology() {
    return {
      localInstance: { applicationId: "test-app", nodeId: "node-a" },
      instances: [{ applicationId: "test-app", nodeId: "node-a" }]
    } as any;
  }

  async streams(_target: TargetSelection) {
    return {
      streams: [
        stream("application", "Application log", "application.log"),
        stream("security", "Security log", "security.log")
      ],
      errors: new Map()
    } as any;
  }

  async records(
    streamId: string,
    _target: TargetSelection,
    filter: RecordFilter,
    _cursor: string | undefined,
    _beforeCursor: string | undefined,
    includeRotated = false
  ): Promise<LogRecordPage> {
    const snapshot = { ...filter, levels: [...filter.levels] };
    this.queries.push({ streamId, filter: snapshot, includeRotated, waitMillis: 0 });
    if (!_cursor)
      return page(streamId, [], this.sequence);
    this.concurrentTailRequests++;
    this.maximumConcurrentTailRequests = Math.max(this.maximumConcurrentTailRequests, this.concurrentTailRequests);
    try {
      if (this.tailResponseDelayMillis > 0)
        await new Promise(resolve => setTimeout(resolve, this.tailResponseDelayMillis));
      if (this.backlogResponses > 0) {
        this.backlogResponses--;
        return page(streamId, [], this.sequence, true);
      }
      if (this.immediateTailResponses)
        return page(streamId, [], this.sequence);

      return await new Promise(resolve => {
        const queue = this.pending.get(streamId) ?? [];
        queue.push({ filter: snapshot, includeRotated, resolve });
        this.pending.set(streamId, queue);
      });
    } finally {
      this.concurrentTailRequests--;
    }
  }

  async logLevelState(_applicationId: string): Promise<LogLevelStateView> {
    const packaged = ["packaged-default"];
    const cascade = this.runtimeCascadeActive ? this.runtimeCascade : packaged;
    const effective = new Map<string, string>();
    for (const profile of cascade)
      for (const [name, level] of this.runtimeProfiles.get(profile) ?? this.packagedProfiles.get(profile) ?? [])
        effective.set(name, level);
    effective.set("ROOT", "INFO");
    return {
      effectiveLevels: effective,
      runtimeLevels: this.runtimeProfiles.get("runtime-default") ?? new Map(),
      packagedLevels: this.packagedProfiles.get("packaged-default") ?? new Map(),
      knownLoggerNames: ["example.Logger", "example.Other"],
      packagedProfiles: new Map(this.packagedProfiles),
      runtimeProfiles: new Map(this.runtimeProfiles),
      packagedProfileCascade: packaged,
      runtimeProfileCascade: [...this.runtimeCascade],
      effectiveProfileCascade: [...cascade],
      profileSupport: true
    };
  }

  async setRuntimeLogLevel(_applicationId: string, logger: string, level: string, profile = "runtime-default") {
    const values = this.runtimeProfiles.get(profile) ?? new Map<string, string>();
    values.set(logger, level);
    this.runtimeProfiles.set(profile, values);
    if (!this.runtimeCascadeActive) { this.runtimeCascade = ["packaged-default"]; this.runtimeCascadeActive = true; }
    if (!this.runtimeCascade.includes(profile)) this.runtimeCascade.push(profile);
  }
  async resetRuntimeLogLevel(_applicationId: string, logger: string, profile = "runtime-default") {
    const values = this.runtimeProfiles.get(profile);
    values?.delete(logger);
    if (values && !values.size) this.runtimeProfiles.delete(profile);
  }
  async clearRuntimeLogLevels(_applicationId: string, profile = "runtime-default") {
    this.runtimeProfiles.delete(profile);
  }
  async setLogLevelProfileCascade(_applicationId: string, profiles: readonly string[]) {
    this.runtimeCascade = [...profiles];
    this.runtimeCascadeActive = true;
  }
  async resetLogLevelProfileCascade() {
    this.runtimeCascade = [];
    this.runtimeCascadeActive = false;
  }

  async bundle(_download: RemoteLogDownload) {
    this.bundleRequestCount++;
    if (this.bundleDelayMillis > 0)
      await new Promise(resolve => setTimeout(resolve, this.bundleDelayMillis));
    return { blob: new Blob(), name: "logs.zip" };
  }

  setImmediateTailResponses(value: boolean) {
    this.immediateTailResponses = value;
  }

  setTailResponseDelay(milliseconds: number) {
    this.tailResponseDelayMillis = milliseconds;
  }

  setBacklogResponses(count: number) {
    this.backlogResponses = count;
  }

  maxConcurrentTailRequests() {
    return this.maximumConcurrentTailRequests;
  }

  setBundleDelay(milliseconds: number) {
    this.bundleDelayMillis = milliseconds;
  }

  bundleRequests() {
    return this.bundleRequestCount;
  }

  setAuthenticated(value: boolean) {
    this.authenticated = value;
  }

  setManager(value: boolean) {
    this.manager = value;
  }

  pendingCount(streamId: string) {
    return this.pending.get(streamId)?.length ?? 0;
  }

  pendingFilters(streamId: string) {
    return (this.pending.get(streamId) ?? []).map(item => item.filter.logger ?? "");
  }

  querySnapshots(streamId: string) {
    return this.queries.filter(query => query.streamId === streamId);
  }

  emit(streamId: string, messages: string | readonly string[]) {
    const queue = this.pending.get(streamId);
    const pending = queue?.shift();
    if (!pending)
      return false;
    const values = typeof messages === "string" ? [messages] : messages;
    const records = values.map(message => this.record(streamId, message));
    pending.resolve(page(streamId, records, this.sequence));
    return true;
  }

  emitContinuityGap(streamId: string, message: string) {
    const queue = this.pending.get(streamId);
    const pending = queue?.shift();
    if (!pending)
      return false;
    pending.resolve(page(streamId, [], this.sequence, false, new Map([["test-app@node-a", message]])));
    return true;
  }

  private record(streamId: string, message: string): LogRecord {
    this.sequence++;
    return {
      timestamp: new Date(),
      level: "INFO",
      streamId,
      loggerName: `test.${streamId}`,
      threadName: "playwright",
      message,
      rawText: `${new Date().toISOString()} INFO test.${streamId} - ${message}`,
      origin: { applicationId: "test-app", nodeId: "node-a" },
      position: { sequence: BigInt(this.sequence) },
      properties: new Map()
    } as any;
  }
}

function stream(streamId: string, displayName: string, fileName: string) {
  return {
    streamId,
    displayName,
    kind: "FILE",
    format: "LOGBACK_PATTERN",
    parsingQuality: "STRUCTURED",
    segments: [{ active: true, fileName }]
  };
}

function page(
  streamId: string,
  records: readonly LogRecord[],
  sequence: number,
  moreAvailable = false,
  continuityGaps: ReadonlyMap<string, string> = new Map()
) {
  return {
    records,
    nextCursor: `${streamId}:${sequence}`,
    previousCursor: null,
    moreBeforeAvailable: false,
    moreAvailable,
    errors: new Map(),
    continuityGaps
  } as any as LogRecordPage;
}

export const mockLogApi = new MockLogReflectionApi();

declare global {
  interface Window {
    logReflectionTestApi: MockLogReflectionApi;
  }
}
