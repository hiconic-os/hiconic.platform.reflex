import { For, Show, createEffect, createMemo, createSignal, onCleanup, onMount } from "solid-js";
import {
  LogReflectionApi,
  type LogReflectionClient,
  type LogLevelStateView,
  type LogRecord,
  type LogRecordPage,
  type LogSegmentDescriptor,
  type LogStreamDescriptor,
  type RecordFilter,
  type RemoteLogFormat,
  type RemoteLogLayout,
  type RemoteLogScope,
  type RuntimeConfig,
  type TargetSelection
} from "./api";
import { createTabExport, type TabExportColumnKey, type TabExportFormat } from "./tab-export";

type StreamTab = {
  kind: "stream";
  id: string;
  stream: LogStreamDescriptor;
  target: TargetSelection;
  records: LogRecord[];
  cursor?: string;
  previousCursor?: string;
  moreBefore: boolean;
  continuityGaps: Record<string, string>;
  tailing: boolean;
  filter: RecordFilter;
  filterRevision: number;
  includeRotated: boolean;
  tailBufferSize: number;
  followTail: boolean;
};

type LogLevelsTab = {
  kind: "log-levels";
  id: string;
  applicationId: string;
};

type SnapshotTab = {
  kind: "snapshot";
  id: string;
  name: string;
  sourceName: string;
  capturedAt: Date;
  sourceRecords: readonly LogRecord[];
  records: LogRecord[];
  continuityGaps: Record<string, string>;
  filter: RecordFilter;
  filterRevision: number;
  filtering: boolean;
  processedRecords: number;
};

type RecordTab = StreamTab | SnapshotTab;
type Tab = RecordTab | LogLevelsTab;

type TopologyInstance = {
  applicationId: string;
  nodeId: string;
};

type ColumnKey = TabExportColumnKey;

type ColumnDefinition = { key: ColumnKey; label: string; width: string };
type ViewMode = "structured" | "text";
type AccessState = "checking" | "authenticated" | "unauthenticated" | "denied" | "offline" | "error";

const columns: readonly ColumnDefinition[] = [
  { key: "timestamp", label: "Timestamp", width: "184px" },
  { key: "level", label: "Level", width: "64px" },
  { key: "application", label: "Application", width: "130px" },
  { key: "stream", label: "Stream", width: "140px" },
  { key: "position", label: "Position", width: "105px" },
  { key: "logger", label: "Logger", width: "230px" },
  { key: "thread", label: "Thread", width: "170px" },
  { key: "source", label: "Source", width: "220px" },
  { key: "message", label: "Message", width: "minmax(280px, 1fr)" },
  { key: "node", label: "Node", width: "110px" },
  { key: "properties", label: "Properties", width: "260px" },
  { key: "raw", label: "Raw text", width: "minmax(280px, 1fr)" }
];

const levels = ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"];
const newDefaultFilter = (): RecordFilter => ({ levels: [] });
const defaultTailPollIntervalMillis = 1_000;
const minimumTailPollIntervalMillis = 100;
const localScanSliceMillis = 7;
const continuationBatchRecords = 200;
type LoadRecordsOutcome = "failed" | "complete" | "more";

export interface AppProps {
  apiFactory?: (config: RuntimeConfig) => LogReflectionClient;
  configLoader?: () => Promise<RuntimeConfig>;
}

function LogLevelValue(props: { level?: string; effective?: boolean; emptyLabel?: string }) {
  const normalizedLevel = () => props.level?.toLowerCase() || "unset";
  return <span class={`log-level-value ${normalizedLevel()}${props.effective ? " effective" : ""}`}>
    {props.level || props.emptyLabel || "—"}
  </span>;
}

export function App(props: AppProps = {}) {
  let runtimeConfig: RuntimeConfig | undefined;
  let accessRefreshInFlight: Promise<void> | undefined;
  const [api, setApi] = createSignal<LogReflectionClient>();
  const [canRead, setCanRead] = createSignal(false);
  const [canManageLevels, setCanManageLevels] = createSignal(false);
  const [accessState, setAccessState] = createSignal<AccessState>("checking");
  const [accessDetail, setAccessDetail] = createSignal("");
  const [applicationId, setApplicationId] = createSignal("");
  const [nodeId, setNodeId] = createSignal("");
  const [topology, setTopology] = createSignal<TopologyInstance[]>([]);
  const [streams, setStreams] = createSignal<LogStreamDescriptor[]>([]);
  const [streamErrors, setStreamErrors] = createSignal<string[]>([]);
  const [tabs, setTabs] = createSignal<Tab[]>([]);
  const [activeTabId, setActiveTabId] = createSignal<string>();
  const [selectedRecord, setSelectedRecord] = createSignal<LogRecord>();
  const [visibleColumns, setVisibleColumns] = createSignal<ColumnKey[]>(["timestamp", "level", "node", "logger", "message"]);
  const [viewMode, setViewMode] = createSignal<ViewMode>("structured");
  const [bundleOpen, setBundleOpen] = createSignal(false);
  const [bundleApplication, setBundleApplication] = createSignal("");
  const [bundleNode, setBundleNode] = createSignal("");
  const [bundleStreams, setBundleStreams] = createSignal<string[]>([]);
  const [bundleScope, setBundleScope] = createSignal<RemoteLogScope>("all");
  const [bundleLayouts, setBundleLayouts] = createSignal<RemoteLogLayout[]>(["separate", "merged"]);
  const [bundleFormats, setBundleFormats] = createSignal<RemoteLogFormat[]>(["natural"]);
  const [bundleFrom, setBundleFrom] = createSignal("");
  const [bundleTo, setBundleTo] = createSignal("");
  const [downloadActivity, setDownloadActivity] = createSignal<"view" | "bundle">();
  const [loading, setLoading] = createSignal(false);
  const [error, setError] = createSignal<string>();
  let recordsViewport: HTMLDivElement | undefined;
  const tabScrollPositions = new Map<string, number>();
  const tailGenerations = new Map<string, number>();
  const filterReloadTimers = new Map<string, ReturnType<typeof setTimeout>>();
  const filterReloadPending = new Set<string>();
  const localFilterGenerations = new Map<string, number>();
  let snapshotSequence = 0;
  let disposed = false;

  const activeTab = createMemo(() => tabs().find(tab => tab.id === activeTabId()));
  const activeStreamTab = createMemo(() => {
    const tab = activeTab();
    return tab?.kind === "stream" ? tab : undefined;
  });
  const activeRecordTab = createMemo(() => {
    const tab = activeTab();
    return tab?.kind === "stream" || tab?.kind === "snapshot" ? tab : undefined;
  });
  const activeLogLevelsTab = createMemo(() => {
    const tab = activeTab();
    return tab?.kind === "log-levels" ? tab : undefined;
  });
  const logLevelTabs = createMemo(() => tabs().filter((tab): tab is LogLevelsTab => tab.kind === "log-levels"));
  const applications = createMemo(() => [...new Set(topology().map(instance => instance.applicationId))]);
  const nodeAliases = createMemo(() => createNodeAliases(topology()));
  const nodes = createMemo(() => [...new Set(topology()
    .filter(instance => applicationId() === "*" || instance.applicationId === applicationId())
    .map(instance => instance.nodeId)
    .filter(Boolean))]);
  const bundleNodes = createMemo(() => [...new Set(topology()
    .filter(instance => instance.applicationId === bundleApplication())
    .map(instance => instance.nodeId)
    .filter(Boolean))]);
  const fileStreams = createMemo(() => streams().filter(stream => enumName(stream.kind) === "FILE"));
  const visibleColumnDefinitions = createMemo(() => columns.filter(column => visibleColumns().includes(column.key)));
  const columnTemplate = createMemo(() => visibleColumnDefinitions().map(column => column.width).join(" "));
  const target = (): TargetSelection => ({
    applicationId: applicationId().trim() || undefined,
    nodeId: nodeId().trim() || undefined
  });

  function streamTabById(id: string) {
    return tabs().find((candidate): candidate is StreamTab => candidate.kind === "stream" && candidate.id === id);
  }

  function recordTabById(id: string) {
    return tabs().find((candidate): candidate is RecordTab =>
      (candidate.kind === "stream" || candidate.kind === "snapshot") && candidate.id === id);
  }

  function snapshotTabById(id: string) {
    return tabs().find((candidate): candidate is SnapshotTab => candidate.kind === "snapshot" && candidate.id === id);
  }

  function updateStreamTab(id: string, update: (tab: StreamTab) => StreamTab) {
    setTabs(current => current.map(candidate => candidate.kind === "stream" && candidate.id === id ? update(candidate) : candidate));
  }

  function updateRecordTab(id: string, update: (tab: RecordTab) => RecordTab) {
    setTabs(current => current.map(candidate =>
      (candidate.kind === "stream" || candidate.kind === "snapshot") && candidate.id === id
        ? update(candidate)
        : candidate));
  }

  function updateSnapshotTab(id: string, update: (tab: SnapshotTab) => SnapshotTab) {
    setTabs(current => current.map(candidate => candidate.kind === "snapshot" && candidate.id === id ? update(candidate) : candidate));
  }

  function nextTailGeneration(id: string) {
    const generation = (tailGenerations.get(id) ?? 0) + 1;
    tailGenerations.set(id, generation);
    return generation;
  }

  function stopTail(id: string) {
    nextTailGeneration(id);
    updateStreamTab(id, tab => tab.tailing ? { ...tab, tailing: false } : tab);
  }

  function stopAllTails() {
    const streamTabs = tabs().filter((tab): tab is StreamTab => tab.kind === "stream");
    streamTabs.forEach(tab => nextTailGeneration(tab.id));
    setTabs(current => current.map(tab => tab.kind === "stream" && tab.tailing ? { ...tab, tailing: false } : tab));
  }

  onMount(() => void initialize());

  async function initialize() {
    setAccessState("checking");
    try {
      runtimeConfig = await (props.configLoader?.() ?? loadConfig());
      const client = props.apiFactory?.(runtimeConfig) ?? new LogReflectionApi(runtimeConfig);
      setApi(client);
      await refreshAccess(true);
    } catch (caught) {
      applyAccessFailure(caught);
    }
  }

  function retryAccess() {
    if (api()) void refreshAccess(topology().length === 0);
    else void initialize();
  }

  const recheckAfterReturn = () => {
    if (document.visibilityState === "visible") void refreshAccess(topology().length === 0);
  };

  onMount(() => {
    window.addEventListener("focus", recheckAfterReturn);
    document.addEventListener("visibilitychange", recheckAfterReturn);
  });

  const closePopupMenus = (event: PointerEvent) => {
    const target = event.target as Node | null;
    document.querySelectorAll<HTMLDetailsElement>("details[data-popup][open]").forEach(popup => {
      if (!target || !popup.contains(target))
        popup.open = false;
    });
  };

  onMount(() => document.addEventListener("pointerdown", closePopupMenus));
  onCleanup(() => {
    disposed = true;
    tailGenerations.clear();
	localFilterGenerations.clear();
    filterReloadTimers.forEach(timer => clearTimeout(timer));
    filterReloadTimers.clear();
    filterReloadPending.clear();
    document.removeEventListener("pointerdown", closePopupMenus);
    window.removeEventListener("focus", recheckAfterReturn);
    document.removeEventListener("visibilitychange", recheckAfterReturn);
  });

  async function refreshAccess(loadWorkspace: boolean) {
    if (accessRefreshInFlight)
      return accessRefreshInFlight;

    accessRefreshInFlight = doRefreshAccess(loadWorkspace);
    try {
      await accessRefreshInFlight;
    } finally {
      accessRefreshInFlight = undefined;
    }
  }

  async function doRefreshAccess(loadWorkspace: boolean) {
    const client = api();
    if (!client) return;
    setAccessState("checking");
    setAccessDetail("");
    try {
      const capabilities = await client.capabilities();
      setCanRead(capabilities.canRead);
      setCanManageLevels(capabilities.canManageLevels);
      if (!capabilities.authenticated) {
        setAccessState("unauthenticated");
        stopAllTails();
        return;
      }
      if (!capabilities.canRead) {
        setAccessState("denied");
        stopAllTails();
        return;
      }
      setAccessState("authenticated");
      setError(undefined);
      if (loadWorkspace || topology().length === 0)
        await loadInitialWorkspace(client);
    } catch (caught) {
      applyAccessFailure(caught);
    }
  }

  async function loadInitialWorkspace(client: LogReflectionClient) {
    const reflectedTopology = await client.topology();
    const reflectedInstances = Array.from(reflectedTopology.instances ?? []) as Array<{
      applicationId?: string | null;
      nodeId?: string | null;
    }>;
    const uniqueInstances = new Map<string, TopologyInstance>();
    reflectedInstances.forEach(instance => {
      const reflected = { applicationId: instance.applicationId ?? "", nodeId: instance.nodeId ?? "" };
      if (reflected.applicationId)
        uniqueInstances.set(`${reflected.applicationId}\u0000${reflected.nodeId}`, reflected);
    });
    const instances = Array.from(uniqueInstances.values());
    setTopology(instances);
    const localApplicationId = reflectedTopology.localInstance?.applicationId ?? instances[0]?.applicationId ?? "";
    setApplicationId(localApplicationId);
    await refreshStreams(client, { applicationId: localApplicationId || undefined });
  }

  function applyAccessFailure(caught: unknown) {
    const message = messageOf(caught);
    setAccessDetail("");
    setCanRead(false);
    setCanManageLevels(false);
    stopAllTails();
    if (isConnectivityFailure(message)) setAccessState("offline");
    else if (isAuthorizationFailure(message)) setAccessState("denied");
    else if (isAuthenticationFailure(message)) setAccessState("unauthenticated");
    else {
      const tracebackId = reportConsoleTraceback("Log Reflection access check failed", caught);
      setAccessDetail(`Console traceback ID: ${tracebackId}`);
      setAccessState("error");
    }
  }

  function handleOperationalFailure(caught: unknown) {
    const message = messageOf(caught);
    if (isAuthenticationFailure(message) || isAuthorizationFailure(message) || isConnectivityFailure(message))
      applyAccessFailure(caught);
    else
      setError(message);
  }

  async function refreshStreams(client = api(), selectedTarget = target()) {
    if (!client) return;
    setLoading(true);
    setError(undefined);
    try {
      const result = await client.streams(selectedTarget);
      const reflectedStreams = Array.from(result.streams ?? []) as LogStreamDescriptor[];
      setStreams(selectedTarget.applicationId === "*"
        ? commonApplicationStreams(reflectedStreams, topology(), selectedTarget.nodeId)
        : uniqueStreams(reflectedStreams));
      setStreamErrors(mapEntries(result.errors).map(([origin, message]) => `${origin}: ${message}`));
    } catch (caught) {
      console.error("Discovering log streams failed", caught);
      handleOperationalFailure(caught);
    } finally {
      setLoading(false);
    }
  }

  async function selectApplication(value: string) {
    setApplicationId(value);
    setNodeId("");
    await refreshStreams(api(), { applicationId: value || undefined });
  }

  async function selectNode(value: string) {
    setNodeId(value);
    await refreshStreams(api(), {
      applicationId: applicationId().trim() || undefined,
      nodeId: value || undefined
    });
  }

  function toggleColumn(column: ColumnKey) {
    setVisibleColumns(current => current.includes(column)
      ? current.length > 1 ? current.filter(candidate => candidate !== column) : current
      : columns.filter(candidate => [...current, column].includes(candidate.key)).map(candidate => candidate.key));
  }

  async function openStream(stream: LogStreamDescriptor) {
    const selectedTarget = target();
    const id = `stream:${selectedTarget.applicationId ?? "local"}:${selectedTarget.nodeId ?? "all"}:${stream.streamId}`;
    const existing = tabs().find(tab => tab.id === id);
    if (existing) {
      activateTab(id);
      return;
    }
    const tab: StreamTab = {
      kind: "stream",
      id,
      stream,
      target: selectedTarget,
      records: [],
      moreBefore: false,
      continuityGaps: {},
      tailing: false,
      filter: newDefaultFilter(),
      filterRevision: 0,
      includeRotated: false,
      tailBufferSize: 2000,
      followTail: true
    };
    setTabs(current => [...current, tab]);
    activateTab(id);
    await loadRecords(tab, "replace");
  }

  function activateTab(id: string | undefined) {
    const currentId = activeTabId();
    if (currentId && recordsViewport)
      tabScrollPositions.set(currentId, recordsViewport.scrollTop);
    setActiveTabId(id);
    requestAnimationFrame(() => {
      if (!id || !recordsViewport)
        return;
      const streamTab = streamTabById(id);
      recordsViewport.scrollTop = streamTab?.tailing && streamTab.followTail
        ? recordsViewport.scrollHeight
        : tabScrollPositions.get(id) ?? 0;
    });
  }

  function openLogLevels() {
    const selectedApplication = applicationId();
    if (!selectedApplication || selectedApplication === "*") return;
    const id = `log-levels:${selectedApplication}`;
    if (!tabs().some(tab => tab.id === id))
      setTabs(current => [...current, { kind: "log-levels", id, applicationId: selectedApplication }]);
    setActiveTabId(id);
  }

  async function loadRecords(
    tab: StreamTab | undefined = activeStreamTab(),
    mode: "replace" | "newer" | "older" = "replace",
    expectedTailGeneration?: number
  ): Promise<LoadRecordsOutcome> {
    const client = api();
    if (!client || !tab) return "failed";
    const streamId = tab.stream.streamId;
    if (!streamId) {
      setError("The selected log stream has no stream ID.");
      return "failed";
    }
    const requestedFilterRevision = tab.filterRevision;
    const scrollHeightBefore = mode === "older" ? recordsViewport?.scrollHeight : undefined;
    const backgroundTail = expectedTailGeneration != null;
    if (!backgroundTail)
      setLoading(true);
    setError(undefined);
    try {
      const page = await client.records(
        streamId,
        tab.target,
        tab.filter,
        mode === "newer" ? tab.cursor : undefined,
        mode === "older" ? tab.previousCursor : undefined,
        tab.includeRotated
      );
      const latestTab = streamTabById(tab.id);
      if (!latestTab || requestedFilterRevision !== latestTab.filterRevision)
        return "failed";
      if (expectedTailGeneration != null && (
        expectedTailGeneration !== tailGenerations.get(tab.id) || !latestTab.tailing
      ))
        return "failed";
      const incoming = Array.from(page.records ?? []) as LogRecord[];
	  const incomingGaps = Object.fromEntries(mapEntries((page as LogRecordPage & { continuityGaps?: unknown }).continuityGaps));
	  if (!await integrateServerPage(tab, mode, incoming, page, incomingGaps, requestedFilterRevision, expectedTailGeneration))
		return "failed";
      if (mode === "newer" && incoming.length && tab.id === activeTabId() && streamTabById(tab.id)?.followTail)
        requestAnimationFrame(() => scrollToLatest());
      if (mode === "older" && incoming.length && scrollHeightBefore != null)
        requestAnimationFrame(() => {
          if (recordsViewport)
            recordsViewport.scrollTop += recordsViewport.scrollHeight - scrollHeightBefore;
        });
      const partialErrors = mapEntries(page.errors);
      if (partialErrors.length)
        setError(partialErrors.map(([origin, message]) => `${origin}: ${message}`).join("\n"));
      return page.moreAvailable ? "more" : "complete";
    } catch (caught) {
      console.error("Loading log records failed", caught);
      handleOperationalFailure(caught);
      return "failed";
    } finally {
      if (!backgroundTail)
        setLoading(false);
    }
  }

  async function integrateServerPage(
	tab: StreamTab,
	mode: "replace" | "newer" | "older",
	incoming: readonly LogRecord[],
	page: LogRecordPage,
	incomingGaps: Record<string, string>,
	requestedFilterRevision: number,
	expectedTailGeneration?: number
  ) {
	const recordsBefore = streamTabById(tab.id)?.records ?? [];
	let index = 0;
	do {
	  const end = Math.min(incoming.length, index + continuationBatchRecords);
	  const batch = incoming.slice(index, end);
	  index = end;
	  const latest = streamTabById(tab.id);
	  if (!latest || latest.filterRevision !== requestedFilterRevision || (expectedTailGeneration != null && (
		expectedTailGeneration !== tailGenerations.get(tab.id) || !latest.tailing
	  )))
		return false;

	  updateStreamTab(tab.id, candidate => ({
		...candidate,
		records: mode === "newer"
		  ? retainRecords([...candidate.records, ...batch], candidate.tailing, candidate.tailBufferSize)
		  : mode === "older" ? [...incoming.slice(0, index), ...recordsBefore] : incoming.slice(0, index),
		cursor: mode === "older" ? candidate.cursor : page.nextCursor ?? undefined,
		previousCursor: page.previousCursor ?? undefined,
		moreBefore: Boolean(page.moreBeforeAvailable),
		continuityGaps: mode === "replace"
		  ? incomingGaps
		  : { ...candidate.continuityGaps, ...incomingGaps }
	  }));
	  if (index < incoming.length)
		await continuationYield();
	} while (index < incoming.length);
	return true;
  }

  async function toggleTail() {
    const tab = activeStreamTab();
    if (!tab) return;
    const tailing = !tab.tailing;
	if (tailing && tailEndReached(tab.filter.to))
	  return;
    setTabs(current => current.map(candidate => candidate.kind === "stream" && candidate.id === tab.id ? { ...candidate, tailing } : candidate));
    const generation = nextTailGeneration(tab.id);
    if (!tailing)
      return;
    updateStreamTab(tab.id, current => ({ ...current, followTail: true }));
    requestAnimationFrame(() => scrollToLatest(true));
    while (!disposed && generation === tailGenerations.get(tab.id)) {
      if (document.visibilityState !== "visible" || accessState() !== "authenticated") {
        await new Promise(resolve => setTimeout(resolve, 500));
        continue;
      }
      if (filterReloadPending.has(tab.id)) {
        await new Promise(resolve => setTimeout(resolve, 75));
        continue;
      }
      const current = streamTabById(tab.id);
      if (!current?.tailing) break;
	  if (tailEndReached(current.filter.to)) {
		stopTail(tab.id);
		break;
	  }
      const started = performance.now();
      const outcome = await loadRecords(current, "newer", generation);
      if (generation !== tailGenerations.get(tab.id))
        break;
      const elapsed = performance.now() - started;
      const interval = tailPollIntervalMillis();
      const delay = outcome === "more"
        ? 0
        : outcome === "complete" ? Math.max(0, interval - elapsed) : Math.max(1_000, interval);
      if (delay > 0)
        await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  function tailPollIntervalMillis() {
    const configured = runtimeConfig?.tailPollIntervalMillis;
    return Number.isFinite(configured)
      ? Math.max(minimumTailPollIntervalMillis, Math.trunc(configured!))
      : defaultTailPollIntervalMillis;
  }

  function closeTab(id: string) {
    stopTail(id);
	localFilterGenerations.set(id, (localFilterGenerations.get(id) ?? 0) + 1);
    const reloadTimer = filterReloadTimers.get(id);
    if (reloadTimer)
      clearTimeout(reloadTimer);
    filterReloadTimers.delete(id);
    filterReloadPending.delete(id);
    const remaining = tabs().filter(tab => tab.id !== id);
    setTabs(remaining);
    if (activeTabId() === id)
      activateTab(remaining.at(-1)?.id);
    tabScrollPositions.delete(id);
  }

  function toggleLevel(level: string) {
    changeFilter(current => ({
        ...current,
        levels: current.levels.includes(level)
          ? current.levels.filter(item => item !== level)
          : [...current.levels, level]
      }),
      80
    );
  }

  function changeFilter(update: (current: RecordFilter) => RecordFilter, delay = 280) {
    const tab = activeRecordTab();
    if (!tab) return;
	updateRecordTab(tab.id, current => {
	  const changed = {
		...current,
		filter: update(current.filter),
		filterRevision: current.filterRevision + 1
	  };
	  return current.kind === "snapshot"
		? { ...changed, records: [], filtering: true, processedRecords: 0 }
		: changed;
	});
    scheduleFilterReload(tab.id, delay);
  }

  function scheduleFilterReload(tabId: string, delay: number) {
    filterReloadPending.add(tabId);
    const previousTimer = filterReloadTimers.get(tabId);
    if (previousTimer)
      clearTimeout(previousTimer);
    const timer = setTimeout(async () => {
      filterReloadTimers.delete(tabId);
	  const tab = recordTabById(tabId);
	  if (tab?.kind === "stream")
		await loadRecords(tab, "replace");
	  else if (tab?.kind === "snapshot")
		await filterSnapshot(tab.id);
      filterReloadPending.delete(tabId);
    }, delay);
    filterReloadTimers.set(tabId, timer);
  }

  function levelRange(): string {
	const selected = levels.filter(level => activeRecordTab()?.filter.levels.includes(level));
    if (selected.length === 0)
      return "all";
    const threshold = levels.slice(levels.length - selected.length);
    return threshold.every((level, index) => level === selected[index])
      ? `from-${selected[0].toLowerCase()}`
      : "custom";
  }

  function applyLevelRange(value: string) {
    if (value === "custom")
      return;
    const selected = value === "all"
      ? []
      : levels.slice(levels.indexOf(value.substring("from-".length).toUpperCase()));
    changeFilter(current => ({ ...current, levels: selected }), 0);
  }

  function trackTailScroll(event: Event) {
    const viewport = event.currentTarget as HTMLDivElement;
    const tabId = activeTabId();
    if (tabId)
      tabScrollPositions.set(tabId, viewport.scrollTop);
    const distanceFromBottom = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight;
    const tab = activeStreamTab();
    if (tab)
      updateStreamTab(tab.id, current => ({ ...current, followTail: distanceFromBottom < 48 }));
  }

  function scrollToLatest(force = false) {
    if (!recordsViewport || (!force && !activeStreamTab()?.followTail)) return;
    recordsViewport.scrollTop = recordsViewport.scrollHeight;
  }

  function resumeFollowing() {
    const tab = activeStreamTab();
    if (tab)
      updateStreamTab(tab.id, current => ({ ...current, followTail: true }));
    requestAnimationFrame(() => scrollToLatest(true));
  }

  function updateTailBuffer(value: string) {
    const parsed = Number.parseInt(value, 10);
    if (!Number.isFinite(parsed)) return;
    const bounded = Math.max(100, Math.min(20_000, parsed));
    const tab = activeStreamTab();
    if (tab)
      updateStreamTab(tab.id, current => ({
        ...current,
        tailBufferSize: bounded,
        records: retainRecords(current.records, current.tailing, bounded)
      }));
  }

  function createSnapshot() {
	const sourceTab = activeRecordTab();
	if (!sourceTab || (sourceTab.kind === "snapshot" && sourceTab.filtering))
	  return;

	const sourceRecords = Array.from(sourceTab.records);
	const sourceName = recordTabName(sourceTab);
	const id = `snapshot:${Date.now()}:${++snapshotSequence}`;
	const snapshot: SnapshotTab = {
	  kind: "snapshot",
	  id,
	  name: `Snapshot ${snapshotSequence}`,
	  sourceName,
	  capturedAt: new Date(),
	  sourceRecords,
	  records: [],
	  continuityGaps: { ...sourceTab.continuityGaps },
	  filter: newDefaultFilter(),
	  filterRevision: 0,
	  filtering: true,
	  processedRecords: 0
	};
	setTabs(current => [...current, snapshot]);
	activateTab(id);
	void filterSnapshot(id);
  }

  async function filterSnapshot(id: string) {
	const tab = snapshotTabById(id);
	if (!tab)
	  return;
	const generation = (localFilterGenerations.get(id) ?? 0) + 1;
	localFilterGenerations.set(id, generation);
	const revision = tab.filterRevision;
	const filter = { ...tab.filter, levels: [...tab.filter.levels] };
	const source = tab.sourceRecords;
	const matches: LogRecord[] = [];
	let index = 0;
	updateSnapshotTab(id, current => ({ ...current, records: [], filtering: true, processedRecords: 0 }));

	while (index < source.length) {
	  const deadline = performance.now() + localScanSliceMillis;
	  const sliceStart = index;
	  do {
		const record = source[index++];
		if (matchesLocalFilter(record, filter))
		  matches.push(record);
	  } while (index < source.length && index - sliceStart < continuationBatchRecords && performance.now() < deadline);

	  const current = snapshotTabById(id);
	  if (!current || current.filterRevision !== revision || localFilterGenerations.get(id) !== generation)
		return;
	  updateSnapshotTab(id, candidate => ({
		...candidate,
		records: matches.slice(),
		filtering: index < source.length,
		processedRecords: index
	  }));
	  if (index < source.length)
		await continuationYield();
	}

	if (source.length === 0)
	  updateSnapshotTab(id, current => ({ ...current, records: [], filtering: false, processedRecords: 0 }));
  }

  async function exportView(format: TabExportFormat, menu?: HTMLElement) {
    if (downloadActivity()) return;
	const tab = activeRecordTab();
    if (!tab) return;
    menu?.closest("details")?.removeAttribute("open");
    setDownloadActivity("view");
    try {
      await nextPaint();
      const selectedColumns = visibleColumnDefinitions().map(({ key, label }) => ({ key, label }));
	  const displayName = recordTabName(tab);
	  const exported = createTabExport(tab.records, selectedColumns, format, displayName);
      const blob = new Blob([exported.content], { type: exported.mimeType });
      triggerDownload(blob,
		`${safeName(displayName)}_${safeName(tab.kind === "stream" ? tab.target.nodeId || "merged" : "local")}_view.${exported.extension}`);
    } catch (caught) {
      console.error("Exporting the current log view failed", caught);
      handleOperationalFailure(caught);
    } finally {
      setDownloadActivity();
    }
  }

  function openBundle() {
    const application = applicationId() === "*" ? applications()[0] ?? "" : applicationId();
    const availableNodes = topology().filter(instance => instance.applicationId === application).map(instance => instance.nodeId);
    setBundleApplication(application);
    setBundleNode(nodeId() || "");
    setBundleStreams(fileStreams().map(stream => stream.streamId).filter((id): id is string => Boolean(id)));
    setBundleOpen(true);
  }

  function selectBundleApplication(value: string) {
    setBundleApplication(value);
    setBundleNode("");
  }

  function toggleBundleStream(streamId: string) {
    setBundleStreams(current => current.includes(streamId)
      ? current.filter(candidate => candidate !== streamId)
      : [...current, streamId]);
  }

  function toggleBundleLayout(layout: RemoteLogLayout) {
    setBundleLayouts(current => current.includes(layout)
      ? current.filter(candidate => candidate !== layout)
      : [...current, layout]);
  }

  function toggleBundleFormat(format: RemoteLogFormat) {
    setBundleFormats(current => current.includes(format)
      ? current.filter(candidate => candidate !== format)
      : [...current, format]);
  }

  async function createBundle() {
    if (downloadActivity()) return;
    const client = api();
    if (!client || !bundleApplication() || bundleStreams().length === 0)
      return;
    if (bundleLayouts().length === 0 || bundleFormats().length === 0) {
      setError("Select at least one layout and one format.");
      return;
    }
    if (bundleScope() === "range") {
      if (!bundleFrom() || !bundleTo()) {
        setError("A custom range needs both a start and an end.");
        return;
      }
      if (new Date(bundleFrom()).getTime() > new Date(bundleTo()).getTime()) {
        setError("The custom range starts after it ends.");
        return;
      }
    }
    setDownloadActivity("bundle");
    setError(undefined);
    try {
      const result = await client.bundle({
        target: {
          applicationId: bundleApplication(),
          nodeId: bundleNode()
        },
        streamIds: bundleStreams(),
        scope: bundleScope(),
        layouts: bundleLayouts(),
        formats: bundleFormats(),
        from: bundleFrom(),
        to: bundleTo()
      });
      triggerDownload(result.blob, result.name);
      setBundleOpen(false);
    } catch (caught) {
      console.error("Creating log bundle failed", caught);
      handleOperationalFailure(caught);
    } finally {
      setDownloadActivity();
    }
  }

  createEffect(() => {
    activeTabId();
    setSelectedRecord(undefined);
  });

  return (
    <div class="shell">
      <main classList={{ masked: accessState() !== "authenticated" }}>
        <section class="workspace">
          <aside class="streams-panel">
            <div class="sidebar-brand">
              <div class="brand">
                <span class="brand-mark"><i></i><i></i><i></i></span>
                <div><strong>Log Reflection</strong><small>hiconic observability</small></div>
              </div>
              <div class="identity">
                <span class="status-dot"></span>
                <div><strong>{canRead() ? "Log access" : "Checking access"}</strong><small>{canManageLevels() ? "Read · Manage levels" : "Read only"}</small></div>
              </div>
            </div>
            <div class="scope-panel">
              <div class="section-title"><span>Application</span><small>Selects the application and its available log streams.</small></div>
              <label>Application
                <select value={applicationId()} onChange={event => selectApplication(event.currentTarget.value)}>
                  <option value="*">All applications</option>
                  <For each={applications()}>{application => <option value={application}>{application}</option>}</For>
                </select>
              </label>
              <label>Node
                <select value={nodeId()} onChange={event => selectNode(event.currentTarget.value)}>
                  <option value="">All nodes</option>
                  <For each={nodes()}>{node => <option value={node}>{nodeLegendLabel(node, nodeAliases())}</option>}</For>
                </select>
              </label>
            </div>
            <div class="panel-heading">
              <div><strong>Streams</strong><small>{streams().length} available</small></div>
              <Show when={applicationId() && applicationId() !== "*"}>
                <button class="levels-action" title={`Configure log levels for ${applicationId()}`} onClick={openLogLevels}>
                  <span class="tab-kind-icon log-levels">L</span> Levels
                </button>
              </Show>
            </div>
            <div class="stream-list">
              <For each={streams()} fallback={<div class="empty">{loading() ? "Discovering log topology…" : "No streams exposed."}</div>}>
                {stream => (
                  <button class="stream" onClick={() => openStream(stream)}>
                    <span class={`stream-glyph ${enumName(stream.kind).toLowerCase()}`}></span>
                    <span>
                      <strong>{stream.displayName || stream.streamId}</strong>
                      <small>{streamDetails(stream)}</small>
                    </span>
                    <em>{enumName(stream.parsingQuality).replace("_", " ")}</em>
                  </button>
                )}
              </For>
            </div>
            <Show when={streamErrors().length}><div class="warning">{streamErrors().join("\n")}</div></Show>
          </aside>

          <section class="log-panel">
            <div class="tabs">
              <For each={tabs()} fallback={<div class="tab-placeholder">Open a stream to begin</div>}>
                {tab => <button classList={{ tab: true, snapshot: tab.kind === "snapshot", active: tab.id === activeTabId(), tailing: tab.kind === "stream" && tab.tailing }} onClick={() => activateTab(tab.id)}>
				  <span class={`tab-kind-icon ${tab.kind}`}>{tab.kind === "stream" ? "≋" : tab.kind === "snapshot" ? "▣" : "L"}</span>
				  <span>{tab.kind === "stream" ? tab.stream.displayName || tab.stream.streamId : tab.kind === "snapshot" ? tab.name : "Log levels"}</span>
                  <small>{tab.kind === "stream"
                    ? `${tab.target.applicationId || "local"} · ${tab.target.nodeId || "all nodes"}`
					: tab.kind === "snapshot" ? `local snapshot · ${tab.sourceRecords.length.toLocaleString()} records` : tab.applicationId}</small>
                  <i onClick={event => { event.stopPropagation(); closeTab(tab.id); }}>×</i>
                </button>}
              </For>
            </div>

            <For each={logLevelTabs()}>{tab =>
              <div classList={{ "tab-workspace": true, hidden: tab.id !== activeTabId() }}>
                <Show when={api()}>{client =>
                  <LogLevelEditor client={client()} applicationId={tab.applicationId} />
                }</Show>
              </div>
            }</For>

			<Show when={activeRecordTab()} fallback={
              <Show when={!activeLogLevelsTab()}><Welcome /></Show>
            }>
              {tab => <>
                <div class="filters">
                  <div class="level-filter"><For each={levels}>{level =>
                    <button classList={{ [level.toLowerCase()]: true, selected: tab().filter.levels.includes(level) }} onClick={() => toggleLevel(level)}>{level}</button>
                  }</For></div>
                  <select
                    class={`level-range ${levelRange().replace("from-", "")}`}
                    aria-label="Level range"
                    title="Select a minimum severity while retaining free individual selection"
                    value={levelRange()}
                    onChange={event => applyLevelRange(event.currentTarget.value)}
                  >
                    <option value="custom">Custom</option>
                    <option value="all">All levels</option>
                    <option class="trace" value="from-trace">TRACE+</option>
                    <option class="debug" value="from-debug">DEBUG+</option>
                    <option class="info" value="from-info">INFO+</option>
                    <option class="warn" value="from-warn">WARN+</option>
                    <option class="error" value="from-error">ERROR only</option>
                  </select>
                  <label class="search"><span>⌕</span><input placeholder="Full text" value={tab().filter.fulltext ?? ""} onInput={e => changeFilter(f => ({ ...f, fulltext: e.currentTarget.value }))} /></label>
                  <label><span>Logger</span><input placeholder="contains logger" value={tab().filter.logger ?? ""} onInput={e => changeFilter(f => ({ ...f, logger: e.currentTarget.value }))} /></label>
                  <label><span>From</span><input type="datetime-local" value={tab().filter.from ?? ""} onChange={e => changeFilter(f => ({ ...f, from: e.currentTarget.value }), 0)} /></label>
                  <label><span>To</span><input type="datetime-local" value={tab().filter.to ?? ""} onChange={e => changeFilter(f => ({ ...f, to: e.currentTarget.value }), 0)} /></label>
                </div>
                <div class="toolbar">
				  <span>
					<strong>{tab().records.length.toLocaleString()}</strong> records
					<Show when={tab().kind === "snapshot" && (tab() as SnapshotTab).filtering}>
					  {` · scanning ${(tab() as SnapshotTab).processedRecords.toLocaleString()}/${(tab() as SnapshotTab).sourceRecords.length.toLocaleString()}`}
					</Show>
				  </span>
                  <div>
                    <div class="view-switch" role="group" aria-label="Log presentation">
                      <button classList={{ active: viewMode() === "structured" }} onClick={() => setViewMode("structured")}>Table</button>
                      <button classList={{ active: viewMode() === "text" }} onClick={() => setViewMode("text")}>Text</button>
                    </div>
                    <details class="column-picker" data-popup>
                      <summary>Columns</summary>
                      <div>
                        <For each={columns}>{column =>
                          <label>
                            <input type="checkbox" checked={visibleColumns().includes(column.key)} onChange={() => toggleColumn(column.key)} />
                            {column.label}
                          </label>
                        }</For>
                      </div>
                    </details>
					<Show when={tab().kind === "stream" ? tab() as StreamTab : undefined}>{streamTab => <>
					<label class="tail-buffer" title="Maximum records retained while tailing">
                      Buffer
                      <input
                        type="number"
                        min="100"
                        max="20000"
                        step="100"
						value={streamTab().tailBufferSize}
                        onChange={event => updateTailBuffer(event.currentTarget.value)}
                      />
                    </label>
                    <label class="history-scope" title="Include files discovered from the appender rolling policy">
                      History
					  <select value={streamTab().includeRotated ? "rotated" : "active"} onChange={event => {
						const tabId = streamTab().id;
                        const includeRotatedHistory = event.currentTarget.value === "rotated";
                        updateStreamTab(tabId, current => ({
                          ...current,
                          includeRotated: includeRotatedHistory,
                          filterRevision: current.filterRevision + 1
                        }));
                        scheduleFilterReload(tabId, 0);
                      }}>
                        <option value="active">Current</option>
                        <option value="rotated">Current + rotated</option>
                      </select>
                    </label>
                    <button
					  classList={{ tail: true, active: streamTab().tailing }}
					  disabled={!streamTab().tailing && tailEndReached(streamTab().filter.to)}
					  title={tailEndReached(streamTab().filter.to) ? "The selected time interval has already ended" : "Continuously observe new records"}
					  onClick={toggleTail}
					><i></i>{streamTab().tailing ? "Tailing" : "Tail"}</button>
					<Show when={streamTab().tailing}>
					  <button classList={{ follow: true, active: streamTab().followTail }} onClick={resumeFollowing}>
						{streamTab().followTail ? "Following" : "Resume"}
                      </button>
                    </Show>
                    <button
					  disabled={!streamTab().moreBefore || !streamTab().previousCursor}
					  title={streamTab().moreBefore ? "Load the preceding log page" : "The beginning of the available history is visible"}
					  onClick={() => loadRecords(streamTab(), "older")}
                    >Load older</button>
					<button onClick={() => loadRecords(streamTab(), "replace")}>Refresh</button>
					</>}</Show>
					<button
					  title={tab().kind === "snapshot" && (tab() as SnapshotTab).filtering
						? "Wait until the local filter has completely scanned its source"
						: "Freeze the records represented by this tab into a local snapshot"}
					  disabled={tab().kind === "snapshot" && (tab() as SnapshotTab).filtering}
					  onClick={createSnapshot}
					>Snapshot</button>
                    <details class="view-export" data-popup>
                      <summary>Export view ↓</summary>
                      <div role="menu" aria-label="Export current view">
                        <button type="button" role="menuitem" onClick={event => exportView("text", event.currentTarget)}>
                          <strong>Plain text</strong><small>Visible columns, preserving multiline values</small>
                        </button>
                        <button type="button" role="menuitem" onClick={event => exportView("jsonl", event.currentTarget)}>
                          <strong>JSONL</strong><small>One structured record per physical line</small>
                        </button>
                        <button type="button" role="menuitem" onClick={event => exportView("spreadsheet", event.currentTarget)}>
                          <strong>Excel XML</strong><small>Visible columns in an Excel-compatible table</small>
                        </button>
                      </div>
                    </details>
					<Show when={tab().kind === "stream"}><button onClick={openBundle}>Download logs ↓</button></Show>
                  </div>
                </div>
				<Show when={tab().kind === "snapshot"}>
				  <div class="snapshot-notice">
					<strong>Local snapshot</strong>
					<span>{`Captured ${(tab() as SnapshotTab).sourceRecords.length.toLocaleString()} records from ${(tab() as SnapshotTab).sourceName} at ${(tab() as SnapshotTab).capturedAt.toLocaleString()}. Filters run only in this browser.`}</span>
				  </div>
				</Show>
				<Show when={Object.entries(tab().continuityGaps)}>{gaps =>
				  <Show when={gaps().length > 0}>
					<div class="warning">
					  <strong>{tab().kind === "snapshot" ? "The snapshot source was already incomplete." : "Live continuity was lost."}</strong>{"\n"}
					  The records shown after the gap are valid, but this view is not a complete continuation.{"\n"}
					  {gaps().map(([origin, message]) => `${origin}: ${message}`).join("\n")}
					</div>
				  </Show>
				}</Show>
                <div classList={{ "records-surface": true, "detail-open": Boolean(selectedRecord()) }}>
                  <Show when={viewMode() === "structured"} fallback={
                    <div
                      class="text-log-view"
                      ref={element => { recordsViewport = element; }}
                      onScroll={trackTailScroll}
                    >
                      <For each={tab().records} fallback={<span class="empty-text-log">No records match this view.</span>}>
                        {record => <TextLogRecord
                          record={record}
                          columns={visibleColumnDefinitions()}
                          nodeAliases={nodeAliases()}
                          onSelect={() => setSelectedRecord(record)}
                        />}
                      </For>
                    </div>
                  }>
                    <div
                      class="log-table"
                      ref={element => { recordsViewport = element; }}
                      onScroll={trackTailScroll}
                    >
                      <div class="log-header" style={`grid-template-columns: ${columnTemplate()}`}>
                        <For each={visibleColumnDefinitions()}>{column => <span>{column.label}</span>}</For>
                      </div>
                      <For each={tab().records} fallback={<div class="empty records">No records match this view.</div>}>
                        {record => <LogRow
                          record={record}
                          columns={visibleColumnDefinitions()}
                          template={columnTemplate()}
                          nodeAliases={nodeAliases()}
                          selected={record === selectedRecord()}
                          onSelect={() => setSelectedRecord(record)}
                        />}
                      </For>
                    </div>
                  </Show>
                  <Show when={selectedRecord()}>
                    {record => <LogRecordDetail record={record()} nodeAliases={nodeAliases()} onClose={() => setSelectedRecord(undefined)} />}
                  </Show>
                </div>
              </>}
            </Show>
          </section>
        </section>
      </main>

      <Show when={error()}><div class="error-toast"><strong>Request failed</strong><span>{error()}</span><button onClick={() => setError()}>×</button></div></Show>
      <Show when={loading()}><div class="progress"></div></Show>
      <Show when={bundleOpen()}>
        <div class="modal-backdrop" onPointerDown={event => {
          if (event.target === event.currentTarget) setBundleOpen(false);
        }}>
          <section class="bundle-dialog" role="dialog" aria-modal="true" aria-label="Download remote logs">
            <header>
              <div><strong>Download remote logs</strong><small>Create a memory-safe ZIP from the selected server-side log data.</small></div>
              <button onClick={() => setBundleOpen(false)}>×</button>
            </header>
            <div class="bundle-scope">
              <label>Application
                <select value={bundleApplication()} onChange={event => selectBundleApplication(event.currentTarget.value)}>
                  <For each={applications()}>{application => <option value={application}>{application}</option>}</For>
                </select>
              </label>
              <label>Node
                <select value={bundleNode()} onChange={event => setBundleNode(event.currentTarget.value)}>
                  <option value="">All nodes</option>
                  <For each={bundleNodes()}>{node => <option value={node}>{nodeLegendLabel(node, nodeAliases())}</option>}</For>
                </select>
              </label>
            </div>
            <div class="bundle-options">
              <fieldset>
                <legend>Scope</legend>
                <label><input type="radio" name="bundle-scope" checked={bundleScope() === "current"} onChange={() => setBundleScope("current")} />Current log</label>
                <label><input type="radio" name="bundle-scope" checked={bundleScope() === "all"} onChange={() => setBundleScope("all")} />Current and rotated logs</label>
                <label><input type="radio" name="bundle-scope" checked={bundleScope() === "range"} onChange={() => setBundleScope("range")} />Custom datetime range</label>
                <Show when={bundleScope() === "range"}>
                  <div class="bundle-range">
                    <label>From<input type="datetime-local" value={bundleFrom()} onChange={event => setBundleFrom(event.currentTarget.value)} /></label>
                    <label>To<input type="datetime-local" value={bundleTo()} onChange={event => setBundleTo(event.currentTarget.value)} /></label>
                  </div>
                </Show>
              </fieldset>
              <fieldset>
                <legend>Layout</legend>
                <label><input type="checkbox" checked={bundleLayouts().includes("separate")} onChange={() => toggleBundleLayout("separate")} />Separate per node</label>
                <label><input type="checkbox" checked={bundleLayouts().includes("merged")} onChange={() => toggleBundleLayout("merged")} />Merged across nodes</label>
              </fieldset>
              <fieldset>
                <legend>Format</legend>
                <label><input type="checkbox" checked={bundleFormats().includes("natural")} onChange={() => toggleBundleFormat("natural")} />Original text</label>
                <label><input type="checkbox" checked={bundleFormats().includes("canonical")} onChange={() => toggleBundleFormat("canonical")} />Canonical text</label>
                <label><input type="checkbox" checked={bundleFormats().includes("jsonl")} onChange={() => toggleBundleFormat("jsonl")} />JSONL</label>
              </fieldset>
            </div>
            <div class="bundle-streams">
              <strong>File streams</strong>
              <For each={fileStreams()} fallback={<span>No file streams are available in this application scope.</span>}>
                {stream => <label>
                  <input type="checkbox" checked={Boolean(stream.streamId) && bundleStreams().includes(stream.streamId!)}
                    disabled={!stream.streamId} onChange={() => stream.streamId && toggleBundleStream(stream.streamId)} />
                  <span>{stream.displayName || stream.streamId}<small>{streamDetails(stream)}</small></span>
                </label>}
              </For>
            </div>
            <footer>
              <span>{bundleStreams().length} stream{bundleStreams().length === 1 ? "" : "s"} selected</span>
              <div><button disabled={Boolean(downloadActivity())} onClick={() => setBundleOpen(false)}>Cancel</button><button class="primary" disabled={Boolean(downloadActivity())} onClick={createBundle}>Download ZIP</button></div>
            </footer>
          </section>
        </div>
      </Show>
      <Show when={downloadActivity()}>{activity =>
        <div class="operation-mask" role="status" aria-live="polite" aria-busy="true">
          <div class="operation-card"><i></i><strong>{activity() === "bundle" ? "Creating remote log archive…" : "Preparing view export…"}</strong><small>Please keep this page open.</small></div>
        </div>
      }</Show>
      <Show when={accessState() !== "authenticated"}><AccessOverlay state={accessState()} detail={accessDetail()}
        loginUrl={runtimeConfig ? loginUrl(runtimeConfig) : undefined} retry={retryAccess}/></Show>
    </div>
  );
}

function LogLevelEditor(props: { client: LogReflectionClient; applicationId: string }) {
  const [state, setState] = createSignal<LogLevelStateView>();
  const [search, setSearch] = createSignal("");
  const [customLogger, setCustomLogger] = createSignal("");
  const [customLevel, setCustomLevel] = createSignal("DEBUG");
  const [loggerSuggestionsOpen, setLoggerSuggestionsOpen] = createSignal(false);
  const [busy, setBusy] = createSignal(false);
  const [failure, setFailure] = createSignal<string>();
  const loggerSuggestions = createMemo(() => {
    const current = state();
    if (!current) return [];
    return [...new Set([
      ...current.knownLoggerNames,
      ...current.runtimeLevels.keys(),
      ...current.packagedLevels.keys()
    ])].sort();
  });
  const matchingLoggerSuggestions = createMemo(() => {
    const value = customLogger().trim();
    const needle = value.toLocaleLowerCase();

    return loggerSuggestions()
      .filter(name => name !== value && (!needle || name.toLocaleLowerCase().includes(needle)))
      .sort((left, right) => {
        const leftStartsWith = left.toLocaleLowerCase().startsWith(needle);
        const rightStartsWith = right.toLocaleLowerCase().startsWith(needle);
        if (leftStartsWith !== rightStartsWith) return leftStartsWith ? -1 : 1;
        return left.localeCompare(right);
      })
      .slice(0, 80);
  });
  const loggerNames = createMemo(() => {
    const current = state();
    if (!current) return [];
    const names = new Set([
      ...current.runtimeLevels.keys(),
      ...current.packagedLevels.keys()
    ]);
    const needle = search().trim().toLowerCase();
    return [...names].filter(name => !needle || name.toLowerCase().includes(needle)).sort();
  });

  onMount(refresh);

  async function refresh() {
    setBusy(true);
    setFailure(undefined);
    try {
      setState(await props.client.logLevelState(props.applicationId));
    } catch (caught) {
      console.error("Loading log-level state failed", caught);
      setFailure(messageOf(caught));
    } finally {
      setBusy(false);
    }
  }

  async function updateLogger(name: string, level: string) {
    setBusy(true);
    setFailure(undefined);
    try {
      if (level)
        await props.client.setRuntimeLogLevel(props.applicationId, name, level);
      else
        await props.client.resetRuntimeLogLevel(props.applicationId, name);
      await refresh();
    } catch (caught) {
      console.error("Updating runtime log level failed", caught);
      setFailure(messageOf(caught));
      setBusy(false);
    }
  }

  async function addCustomLogger() {
    const name = customLogger().trim();
    if (!name) return;
    await updateLogger(name, customLevel());
    setCustomLogger("");
    setLoggerSuggestionsOpen(false);
  }

  async function clearRuntimeLevels() {
    if (!window.confirm(`Remove every runtime log-level override from ${props.applicationId}?`)) return;
    setBusy(true);
    setFailure(undefined);
    try {
      await props.client.clearRuntimeLogLevels(props.applicationId);
      await refresh();
    } catch (caught) {
      console.error("Clearing runtime log levels failed", caught);
      setFailure(messageOf(caught));
      setBusy(false);
    }
  }

  return <section class="log-level-workspace">
    <header>
      <div>
        <small>Application configuration</small>
        <h2>Log levels</h2>
        <p>Explicit log-level mappings for <strong>{props.applicationId}</strong>. Resetting a logger restores its packaged or inherited level.</p>
      </div>
      <div class="log-level-actions">
        <button onClick={refresh} disabled={busy()}>Refresh</button>
        <button class="danger-subtle" onClick={clearRuntimeLevels} disabled={busy() || !state()?.runtimeLevels.size}>Clear overrides</button>
      </div>
    </header>
    <div class="log-level-controls">
      <label class="search"><span>⌕</span><input placeholder="Filter logger names" value={search()} onInput={event => setSearch(event.currentTarget.value)} /></label>
      <div class="custom-level">
        <div class="logger-completion">
          <input
            placeholder="com.example.Logger"
            spellcheck={false}
            autocomplete="off"
            value={customLogger()}
            onFocus={() => setLoggerSuggestionsOpen(true)}
            onInput={event => {
              setCustomLogger(event.currentTarget.value);
              setLoggerSuggestionsOpen(true);
            }}
            onKeyDown={event => {
              if (event.key === "Escape") setLoggerSuggestionsOpen(false);
              if (event.key === "Enter" && matchingLoggerSuggestions().length === 1) {
                event.preventDefault();
                setCustomLogger(matchingLoggerSuggestions()[0]);
                setLoggerSuggestionsOpen(false);
              }
            }}
            onBlur={() => window.setTimeout(() => setLoggerSuggestionsOpen(false), 120)}
          />
          <Show when={loggerSuggestionsOpen() && matchingLoggerSuggestions().length > 0}>
            <div class="logger-suggestions" role="listbox" aria-label="Known logger names">
              <For each={matchingLoggerSuggestions()}>{name =>
                <button
                  type="button"
                  role="option"
                  onPointerDown={event => event.preventDefault()}
                  onClick={() => {
                    setCustomLogger(name);
                    setLoggerSuggestionsOpen(false);
                  }}
                >{name}</button>
              }</For>
            </div>
          </Show>
        </div>
        <select value={customLevel()} onChange={event => setCustomLevel(event.currentTarget.value)}>
          <For each={["ALL", ...levels, "OFF"]}>{level => <option value={level}>{level}</option>}</For>
        </select>
        <button class="primary" disabled={busy() || !customLogger().trim()} onClick={addCustomLogger}>Add override</button>
      </div>
    </div>
    <Show when={failure()}>{message => <div class="log-level-error">{message()}</div>}</Show>
    <div class="log-level-table">
      <div class="log-level-header"><span>Logger</span><span>Packaged</span><span>Runtime</span><span>Effective</span></div>
      <Show when={state()} fallback={<div class="empty records">{busy() ? "Loading log levels…" : "No log-level state available."}</div>}>
        {current => <For each={loggerNames()} fallback={<div class="empty records">
          {search().trim() ? "No explicitly configured logger matches this filter." : "No explicit log-level mappings configured."}
        </div>}>
          {name => <div class="log-level-row">
            <code title={name}>{name || "<root>"}</code>
            <LogLevelValue level={current().packagedLevels.get(name)} emptyLabel="inherited" />
            <details class="runtime-level-menu" data-popup>
              <summary aria-label={`Change runtime log level for ${name || "root logger"}`}>
                <LogLevelValue level={current().runtimeLevels.get(name)} emptyLabel="no override" />
                <svg class="runtime-menu-indicator" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 8.75A3.25 3.25 0 1 0 12 15.25 3.25 3.25 0 0 0 12 8.75Zm8.1 4.65-1.77 1.02c-.12.36-.27.71-.44 1.05l.53 1.98-1.97 1.97-1.98-.53c-.34.17-.69.32-1.05.44L12.4 21.1H9.6l-1.02-1.77a8.2 8.2 0 0 1-1.05-.44l-1.98.53-1.97-1.97.53-1.98a8.2 8.2 0 0 1-.44-1.05L1.9 13.4v-2.8l1.77-1.02c.12-.36.27-.71.44-1.05l-.53-1.98 1.97-1.97 1.98.53c.34-.17.69-.32 1.05-.44L9.6 1.9h2.8l1.02 1.77c.36.12.71.27 1.05.44l1.98-.53 1.97 1.97-.53 1.98c.17.34.32.69.44 1.05l1.77 1.02v2.8Z" />
                </svg>
              </summary>
              <div class="runtime-level-options">
                <For each={["ALL", ...levels, "OFF"]}>{level =>
                  <button
                    type="button"
                    class={current().runtimeLevels.get(name) === level ? "selected" : ""}
                    disabled={busy()}
                    onClick={event => {
                      event.currentTarget.closest("details")?.removeAttribute("open");
                      updateLogger(name, level);
                    }}
                  ><LogLevelValue level={level} /></button>
                }</For>
                <button
                  type="button"
                  class="reset-runtime-level"
                  disabled={busy() || !current().runtimeLevels.has(name)}
                  onClick={event => {
                    event.currentTarget.closest("details")?.removeAttribute("open");
                    updateLogger(name, "");
                  }}
                >Reset to packaged</button>
              </div>
            </details>
            <LogLevelValue level={current().effectiveLevels.get(name)} effective emptyLabel="inherited" />
          </div>}
        </For>}
      </Show>
    </div>
    <Show when={busy()}><div class="log-level-progress"></div></Show>
  </section>;
}

function LogRow(props: {
  record: LogRecord;
  columns: readonly ColumnDefinition[];
  template: string;
  nodeAliases: ReadonlyMap<string, number>;
  selected: boolean;
  onSelect: () => void;
}) {
  return <div
    classList={{ "log-row": true, selected: props.selected }}
    style={`grid-template-columns: ${props.template}`}
    onClick={props.onSelect}
  >
    <For each={props.columns}>{column => <LogCell record={props.record} column={column.key} nodeAliases={props.nodeAliases} />}</For>
  </div>;
}

function LogCell(props: { record: LogRecord; column: ColumnKey; nodeAliases: ReadonlyMap<string, number> }) {
  const level = () => enumName(props.record.level);
  if (props.column === "level")
    return <span><b class={`level ${level().toLowerCase()}`}>{level()}</b></span>;

  const value = columnValue(props.record, props.column, props.nodeAliases);
  const title = props.column === "node" ? props.record.origin?.nodeId || value : value;
  return <span class={props.column} title={title}>{value}</span>;
}

function TextLogRecord(props: {
  record: LogRecord;
  columns: readonly ColumnDefinition[];
  nodeAliases: ReadonlyMap<string, number>;
  onSelect: () => void;
}) {
  const level = () => enumName(props.record.level).toLowerCase();
  const fields = () => props.columns
    .map(column => ({ column, value: textColumnValue(props.record, column.key, props.nodeAliases) }))
    .filter(field => field.value !== "");
  return <span class="text-log-record" onClick={props.onSelect}><For each={fields()}>{(field, index) => <><Show when={index() > 0}><span class="text-separator">{"  "}</span></Show><span class={`text-field ${field.column.key} ${field.column.key === "level" ? level() : ""}`}>{field.value}</span></>}</For>{"\n"}</span>;
}

function textColumnValue(record: LogRecord, column: ColumnKey, nodeAliases: ReadonlyMap<string, number>): string {
  switch (column) {
    case "message":
      return record.message || "—";
    case "raw":
      return record.rawText || "—";
    default:
      return columnValue(record, column, nodeAliases);
  }
}

function columnValue(record: LogRecord, column: ColumnKey, nodeAliases: ReadonlyMap<string, number>): string {
  switch (column) {
    case "timestamp":
      return formatTimestamp(record.timestamp);
    case "level":
      return enumName(record.level) || "—";
    case "application":
      return record.origin?.applicationId || "—";
    case "node":
      return nodeDisplay(record.origin?.nodeId, nodeAliases);
    case "stream":
      return record.streamId || "—";
    case "position":
      return formatPosition(record);
    case "logger":
      return shortLogger(record.loggerName);
    case "thread":
      return record.threadName || "—";
    case "source":
      return formatSourceLocation(record) || "—";
    case "message":
      return oneLine(record.message) || "—";
    case "properties":
      return formatProperties(record.properties);
    case "raw":
      return oneLine(record.rawText) || "—";
  }
}

function LogRecordDetail(props: { record: LogRecord; nodeAliases: ReadonlyMap<string, number>; onClose: () => void }) {
  const properties = () => mapEntries(props.record.properties);

  return <aside class="record-detail">
    <header>
      <div><small>Log record</small><strong>{enumName(props.record.level)} · {formatTimestamp(props.record.timestamp)}</strong></div>
      <button title="Close details" onClick={props.onClose}>×</button>
    </header>
    <div class="detail-properties">
      <DetailProperty name="Application" value={props.record.origin?.applicationId} />
      <DetailProperty name="Node" value={nodeLegendLabel(props.record.origin?.nodeId, props.nodeAliases)} />
      <DetailProperty name="Stream" value={props.record.streamId} />
      <DetailProperty name="Logger" value={props.record.loggerName} />
      <DetailProperty name="Thread" value={props.record.threadName} />
      <DetailProperty name="Source" value={formatSourceLocation(props.record)} />
      <DetailProperty name="Sequence" value={props.record.position?.sequence?.toString()} />
      <For each={properties()}>{([name, value]) => <DetailProperty name={name} value={value} />}</For>
    </div>
    <section class="detail-content">
      <DetailHeading label="Message" value={props.record.message || "—"} />
      <pre>{props.record.message || "—"}</pre>
      <Show when={!props.record.rawText && props.record.throwable}>
        {throwable => <>
          <DetailHeading label="Throwable" value={throwable()} />
          <pre class="throwable">{throwable()}</pre>
        </>}
      </Show>
      <Show when={props.record.rawText && props.record.rawText !== props.record.message}>
        <DetailHeading label="Raw record" value={props.record.rawText!} />
        <pre class="raw">{props.record.rawText}</pre>
      </Show>
    </section>
  </aside>;
}

function DetailHeading(props: { label: string; value: string }) {
  return <div class="detail-heading">
    <small>{props.label}</small>
    <button title={`Copy ${props.label.toLowerCase()} to clipboard`} onClick={() => void copyText(props.value)}>Copy</button>
  </div>;
}

async function copyText(value: string) {
  await navigator.clipboard.writeText(value);
}

function DetailProperty(props: { name: string; value: unknown }) {
  return <Show when={props.value != null && props.value !== ""}>
    <div><small>{props.name}</small><span title={String(props.value)}>{String(props.value)}</span></div>
  </Show>;
}

function Welcome() {
  return <div class="welcome"><div class="radar"><i></i><i></i><i></i></div><strong>Observe the system as structured data.</strong><p>Select a live collector or a log file. Every record retains its node, application, source and raw representation.</p></div>;
}

async function loadConfig(): Promise<RuntimeConfig> {
  const response = await fetch("./runtime-config.json", { cache: "no-store" });
  if (!response.ok) throw new Error(`Runtime configuration unavailable (${response.status})`);
  return response.json();
}

function loginUrl(config: RuntimeConfig) {
  const url = new URL(config.loginUrl ?? `${config.servicesUrl.replace(/\/$/, "")}/login`, window.location.origin);
  url.searchParams.set("continue", `${window.location.pathname}${window.location.search}${window.location.hash}`);
  return url.toString();
}

function isAuthenticationFailure(message: string) {
  return /MissingSession|MissingCredentials|AuthenticationFailure|SessionNotFound|No authenticated web session|Unauthorized|401/i.test(message);
}

function isAuthorizationFailure(message: string) {
  return /Forbidden|Insufficient.*role|required role|403/i.test(message);
}

function isConnectivityFailure(message: string) {
  return /Failed to fetch|NetworkError|Network request failed|Load failed|timeout|HTTP 50[234]/i.test(message);
}

function AccessOverlay(props: { state: AccessState; detail: string; loginUrl?: string; retry: () => void }) {
  const title = () => props.state === "checking" ? "Checking access…"
    : props.state === "offline" ? "Connection interrupted"
    : props.state === "denied" ? "Access denied"
    : props.state === "error" ? "Access check failed"
    : "Session required";
  const explanation = () => props.state === "offline" ? "The server cannot currently be reached. Filters and selections remain in this browser."
    : props.state === "denied" ? "Your session is valid, but it has no Log Reflection reader role."
    : props.state === "error" ? "A technical problem occurred while checking access."
    : props.state === "unauthenticated" ? "Sign in to inspect logs. The current workspace remains available."
    : "Verifying session and capabilities.";
  return <div class="access-overlay" role="dialog" aria-live="polite"><div class="access-card"><strong>{title()}</strong><p>{explanation()}</p>
    <Show when={props.detail && props.state !== "checking"}><small>{props.detail}</small></Show>
    <div class="access-actions"><Show when={props.state === "offline" || props.state === "error"}><button onClick={props.retry}>Try again</button></Show>
      <Show when={props.state === "unauthenticated" && props.loginUrl}><a class="login" href={props.loginUrl}>Sign in</a></Show></div>
  </div></div>;
}

function messageOf(caught: unknown) {
  return caught instanceof Error ? caught.message : String(caught);
}

function reportConsoleTraceback(context: string, caught: unknown) {
  // The local reference keeps implementation details out of the UI while linking it to the complete browser-console error. The original
  // error remains intact and may itself contain a server-side traceback ID.
  const tracebackId = `LR-${crypto.randomUUID().slice(0, 8)}`;
  console.error(`[${tracebackId}] ${context}`, caught);
  return tracebackId;
}

function enumName(value: unknown): string {
  if (!value) return "";
  const candidate = value as {
    name?: string | (() => string);
    Name?: () => string;
    toString?: () => string;
  };
  if (typeof candidate.name === "function")
    return candidate.name();
  return candidate.name ?? candidate.Name?.() ?? candidate.toString?.() ?? String(value);
}

function streamDetails(stream: LogStreamDescriptor) {
  const segments = Array.from(stream.segments as unknown as Iterable<LogSegmentDescriptor>);
  const fileName = (segments.find(segment => segment.active) ?? segments[0])?.fileName;
  return [fileName, enumName(stream.format)].filter(Boolean).join(" · ");
}

function createNodeAliases(topology: readonly TopologyInstance[]): ReadonlyMap<string, number> {
  const nodeIds = [...new Set(topology.map(instance => instance.nodeId).filter(Boolean))]
    .sort((left, right) => left.localeCompare(right));
  return new Map(nodeIds.map((nodeId, index) => [nodeId, index + 1]));
}

function nodeDisplay(nodeId: string | null | undefined, aliases: ReadonlyMap<string, number>): string {
  if (!nodeId)
    return "local";
  const alias = aliases.get(nodeId);
  return alias == null ? nodeId : `#${alias}`;
}

function nodeLegendLabel(nodeId: string | null | undefined, aliases: ReadonlyMap<string, number>): string {
  if (!nodeId)
    return "local";
  const alias = aliases.get(nodeId);
  return alias == null ? nodeId : `#${alias} · ${nodeId}`;
}

function uniqueStreams(streams: readonly LogStreamDescriptor[]): LogStreamDescriptor[] {
  const unique = new Map<string, LogStreamDescriptor>();
  streams.forEach(stream => {
    const streamId = stream.streamId;
    if (streamId && !unique.has(streamId))
      unique.set(streamId, stream);
  });
  return Array.from(unique.values());
}

function commonApplicationStreams(
  streams: readonly LogStreamDescriptor[],
  topology: readonly TopologyInstance[],
  nodeId: string | undefined
): LogStreamDescriptor[] {
  const instances = topology.filter(instance => !nodeId || instance.nodeId === nodeId);
  const expectedOrigins = new Set(instances.map(instance => `${instance.applicationId}\u0000${instance.nodeId}`));
  const grouped = new Map<string, LogStreamDescriptor[]>();
  streams.forEach(stream => {
    const streamId = stream.streamId;
    if (!streamId) return;
    const group = grouped.get(streamId);
    if (group)
      group.push(stream);
    else
      grouped.set(streamId, [stream]);
  });

  const result: LogStreamDescriptor[] = [];
  grouped.forEach(group => {
    const representative = group[0];
    if (enumName(representative.kind) === "STRUCTURED_LIVE") {
      result.push(representative);
      return;
    }
    const origins = new Set(group.map(stream =>
      `${stream.origin?.applicationId ?? ""}\u0000${stream.origin?.nodeId ?? ""}`
    ));
    if (expectedOrigins.size > 0 && [...expectedOrigins].every(origin => origins.has(origin)))
      result.push(representative);
  });
  return result;
}

function formatTimestamp(value: unknown) {
  const date = nativeDate(value);
  if (!date) return "—";
  const local = new Intl.DateTimeFormat(undefined, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  }).format(date);
  return `${local}.${String(date.getMilliseconds()).padStart(3, "0")}`;
}

function nativeDate(value: unknown): Date | undefined {
  if (value == null) return undefined;
  if (value instanceof Date)
    return Number.isNaN(value.getTime()) ? undefined : value;
  if (typeof value === "number" || typeof value === "string") {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? undefined : date;
  }

  const candidate = value as {
    getTime?: () => number;
    valueOf?: () => unknown;
    toISOString?: () => string;
  };
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

function formatSourceLocation(record: LogRecord) {
  const source = record.sourceLocation;
  if (!source) return undefined;
  const member = [source.className, source.methodName].filter(Boolean).join(".");
  const file = source.fileName ? `${source.fileName}${source.lineNumber != null ? `:${source.lineNumber}` : ""}` : undefined;
  return [member, file].filter(Boolean).join(" · ");
}

function formatPosition(record: LogRecord) {
  const sequence = record.position?.sequence;
  const byteOffset = record.position?.byteOffset;
  if (sequence != null) return `seq:${sequence}`;
  if (byteOffset != null) return `byte:${byteOffset}`;
  return "—";
}

function formatProperties(value: unknown) {
  const entries = mapEntries(value);
  return entries.length ? entries.map(([name, item]) => `${name}=${item}`).join(" ") : "—";
}

function oneLine(value: string | null | undefined) {
  return value?.replace(/\s*\r?\n\s*/g, " ").trim();
}

function retainRecords(records: LogRecord[], tailing: boolean, capacity: number) {
  return tailing && records.length > capacity ? records.slice(-capacity) : records;
}

function recordTabName(tab: RecordTab) {
  return tab.kind === "snapshot" ? tab.name : tab.stream.displayName || tab.stream.streamId || "Logs";
}

function matchesLocalFilter(record: LogRecord, filter: RecordFilter) {
  const timestamp = record.timestamp == null ? undefined : new Date(record.timestamp as unknown as string | number | Date).getTime();
  const from = filter.from ? Date.parse(filter.from) : undefined;
  const to = filter.to ? Date.parse(filter.to) : undefined;
  if (from != null && (!Number.isFinite(timestamp) || timestamp! < from)) return false;
  if (to != null && (!Number.isFinite(timestamp) || timestamp! > to)) return false;
  const level = enumName(record.level).toUpperCase();
  if (filter.levels.length > 0 && !filter.levels.includes(level)) return false;
  if (filter.logger && !containsIgnoreCase(record.loggerName, filter.logger)) return false;
  if (filter.fulltext) {
	const needle = filter.fulltext.trim().toLowerCase();
	if (needle && ![record.message, record.throwable, record.rawText, record.loggerName]
		.some(value => typeof value === "string" && value.toLowerCase().includes(needle)))
	  return false;
  }
  return true;
}

function containsIgnoreCase(value: string | null | undefined, query: string) {
  return !query.trim() || Boolean(value?.toLowerCase().includes(query.trim().toLowerCase()));
}

function continuationYield() {
  if (document.visibilityState !== "visible")
	return new Promise<void>(resolve => setTimeout(resolve, 0));

  return new Promise<void>(resolve => {
	requestAnimationFrame(() => setTimeout(resolve, 0));
  });
}

function tailEndReached(to: string | undefined) {
  if (!to) return false;
  const end = Date.parse(to);
  return Number.isFinite(end) && end <= Date.now();
}

function shortLogger(value: string | null | undefined) {
  if (!value) return "—";
  const parts = value.split(".");
  return parts.length > 3 ? `…${parts.slice(-3).join(".")}` : value;
}

function mapEntries(value: unknown): [string, string][] {
  if (!value) return [];

  const modeledMap = value as {
    entries?: () => Iterable<[string | null, string | null]>;
  };
  if (typeof modeledMap.entries === "function")
    return Array.from(modeledMap.entries(), ([name, item]) => [name ?? "", item ?? ""]);

  return Object.entries(value as Record<string, unknown>).map(([name, item]) => [name, item == null ? "" : String(item)]);
}

function safeName(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9._-]+/g, "-");
}

function nextPaint() {
  return new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve())));
}

function triggerDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.hidden = true;
  document.body.append(link);
  try {
    link.click();
  } finally {
    link.remove();
    // Revoking immediately races browser download managers, especially on a repeated export.
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
