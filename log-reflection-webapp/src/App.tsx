import { For, Show, createEffect, createMemo, createSignal, onCleanup, onMount } from "solid-js";
import {
  LogReflectionApi,
  type LogRecord,
  type LogSegmentDescriptor,
  type LogStreamDescriptor,
  type RecordFilter,
  type RuntimeConfig,
  type TargetSelection
} from "./api";

type Tab = {
  id: string;
  stream: LogStreamDescriptor;
  target: TargetSelection;
  records: LogRecord[];
  cursor?: string;
  tailing: boolean;
};

type TopologyInstance = {
  applicationId: string;
  nodeId: string;
};

type ColumnKey =
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
  | "throwable"
  | "properties"
  | "raw";

type ColumnDefinition = { key: ColumnKey; label: string; width: string };
type ViewMode = "structured" | "text";

const columns: readonly ColumnDefinition[] = [
  { key: "timestamp", label: "Timestamp", width: "184px" },
  { key: "level", label: "Level", width: "64px" },
  { key: "application", label: "Application", width: "130px" },
  { key: "node", label: "Node", width: "110px" },
  { key: "stream", label: "Stream", width: "140px" },
  { key: "position", label: "Position", width: "105px" },
  { key: "logger", label: "Logger", width: "230px" },
  { key: "thread", label: "Thread", width: "170px" },
  { key: "source", label: "Source", width: "220px" },
  { key: "message", label: "Message", width: "minmax(280px, 1fr)" },
  { key: "throwable", label: "Throwable", width: "minmax(260px, 1fr)" },
  { key: "properties", label: "Properties", width: "260px" },
  { key: "raw", label: "Raw text", width: "minmax(280px, 1fr)" }
];

const levels = ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"];
const defaultFilter: RecordFilter = { levels: [] };

export function App() {
  const [api, setApi] = createSignal<LogReflectionApi>();
  const [user, setUser] = createSignal("Connecting");
  const [roles, setRoles] = createSignal<string[]>([]);
  const [applicationId, setApplicationId] = createSignal("");
  const [nodeId, setNodeId] = createSignal("");
  const [topology, setTopology] = createSignal<TopologyInstance[]>([]);
  const [streams, setStreams] = createSignal<LogStreamDescriptor[]>([]);
  const [streamErrors, setStreamErrors] = createSignal<string[]>([]);
  const [tabs, setTabs] = createSignal<Tab[]>([]);
  const [activeTabId, setActiveTabId] = createSignal<string>();
  const [filter, setFilter] = createSignal<RecordFilter>(defaultFilter);
  const [selectedRecord, setSelectedRecord] = createSignal<LogRecord>();
  const [visibleColumns, setVisibleColumns] = createSignal<ColumnKey[]>(["timestamp", "level", "node", "logger", "message"]);
  const [viewMode, setViewMode] = createSignal<ViewMode>("structured");
  const [tailBufferSize, setTailBufferSize] = createSignal(2000);
  const [followTail, setFollowTail] = createSignal(true);
  const [includeRotated, setIncludeRotated] = createSignal(false);
  const [bundleOpen, setBundleOpen] = createSignal(false);
  const [bundleApplication, setBundleApplication] = createSignal("");
  const [bundleNode, setBundleNode] = createSignal("");
  const [bundleStreams, setBundleStreams] = createSignal<string[]>([]);
  const [bundleFormat, setBundleFormat] = createSignal<"raw" | "jsonl">("raw");
  const [loading, setLoading] = createSignal(false);
  const [error, setError] = createSignal<string>();
  let recordsViewport: HTMLDivElement | undefined;
  let tailGeneration = 0;
  let filterRevision = 0;
  let filterReloadPending = false;
  let filterReloadTimer: ReturnType<typeof setTimeout> | undefined;

  const activeTab = createMemo(() => tabs().find(tab => tab.id === activeTabId()));
  const applications = createMemo(() => [...new Set(topology().map(instance => instance.applicationId))]);
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

  onMount(async () => {
    let config: RuntimeConfig | undefined;
    try {
      config = await loadConfig();
      const client = new LogReflectionApi(config);
      setApi(client);
      const authorization = await client.authorization();
      const authUser = authorization.user as { name?: string; id?: string } | undefined;
      const effectiveRoles = Array.from(authorization.effectiveRoles ?? []);
      setUser(authUser?.name ?? authUser?.id ?? "Authenticated user");
      setRoles(effectiveRoles);
      const adminRoles = Array.from(config.adminRoles ?? []);
      if (adminRoles.length && !effectiveRoles.some(role => adminRoles.includes(role))) {
        setError(`Administrative role required (${adminRoles.join(", ")}).`);
        return;
      }
      const reflectedTopology = await client.topology();
      const reflectedInstances = Array.from(reflectedTopology.instances ?? []) as Array<{
        applicationId?: string | null;
        nodeId?: string | null;
      }>;
      const uniqueInstances = new Map<string, TopologyInstance>();
      reflectedInstances.forEach(instance => {
        const reflected = {
          applicationId: instance.applicationId ?? "",
          nodeId: instance.nodeId ?? ""
        };
        if (reflected.applicationId)
          uniqueInstances.set(`${reflected.applicationId}\u0000${reflected.nodeId}`, reflected);
      });
      const instances = Array.from(uniqueInstances.values());
      setTopology(instances);
      const localApplicationId = reflectedTopology.localInstance?.applicationId ?? instances[0]?.applicationId ?? "";
      setApplicationId(localApplicationId);
      await refreshStreams(client, { applicationId: localApplicationId || undefined });
    } catch (caught) {
      console.error("Log Reflection initialization failed", caught);
      const message = messageOf(caught);
      if (config && isAuthenticationFailure(message)) redirectToLogin(config, caught);
      else {
        setUser("Unavailable");
        setError(message);
      }
    }
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
    tailGeneration++;
    if (filterReloadTimer)
      clearTimeout(filterReloadTimer);
    document.removeEventListener("pointerdown", closePopupMenus);
  });

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
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }

  async function selectApplication(value: string) {
    setApplicationId(value);
    setNodeId("");
    setTabs([]);
    setActiveTabId(undefined);
    await refreshStreams(api(), { applicationId: value || undefined });
  }

  async function selectNode(value: string) {
    tailGeneration++;
    filterRevision++;
    setNodeId(value);
    setTabs(current => current.map(tab => ({
      ...tab,
      target: {
        applicationId: tab.target.applicationId,
        nodeId: value || undefined
      },
      records: [],
      cursor: undefined,
      tailing: false
    })));
    const tab = activeTab();
    if (tab)
      await loadRecords(tab, false);
  }

  function toggleColumn(column: ColumnKey) {
    setVisibleColumns(current => current.includes(column)
      ? current.length > 1 ? current.filter(candidate => candidate !== column) : current
      : columns.filter(candidate => [...current, column].includes(candidate.key)).map(candidate => candidate.key));
  }

  async function openStream(stream: LogStreamDescriptor) {
    const selectedTarget = target();
    const id = `${stream.streamId}:${selectedTarget.applicationId ?? "local"}`;
    const existing = tabs().find(tab => tab.id === id);
    if (existing) {
      setActiveTabId(id);
      return;
    }
    const tab: Tab = { id, stream, target: selectedTarget, records: [], tailing: false };
    setTabs(current => [...current, tab]);
    setActiveTabId(id);
    await loadRecords(tab, false);
  }

  async function loadRecords(tab = activeTab(), append = false, waitMillis = 0) {
    const client = api();
    if (!client || !tab) return;
    const requestedFilterRevision = filterRevision;
    const backgroundTail = waitMillis > 0;
    if (!backgroundTail)
      setLoading(true);
    setError(undefined);
    try {
      const page = await client.records(tab.stream.streamId, tab.target, filter(), append ? tab.cursor : undefined, waitMillis,
        includeRotated());
      if (requestedFilterRevision !== filterRevision)
        return;
      const incoming = Array.from(page.records ?? []) as LogRecord[];
      setTabs(current => current.map(candidate => candidate.id === tab.id ? {
        ...candidate,
        records: append
          ? retainRecords([...candidate.records, ...incoming], candidate.tailing, tailBufferSize())
          : incoming,
        cursor: page.nextCursor ?? undefined
      } : candidate));
      if (append && incoming.length && tab.id === activeTabId() && followTail())
        requestAnimationFrame(() => scrollToLatest());
      const partialErrors = mapEntries(page.errors);
      if (partialErrors.length)
        setError(partialErrors.map(([origin, message]) => `${origin}: ${message}`).join("\n"));
    } catch (caught) {
      console.error("Loading log records failed", caught);
      const message = messageOf(caught);
      if (isAuthenticationFailure(message)) redirectToLogin(client.config, caught);
      else setError(message);
    } finally {
      if (!backgroundTail)
        setLoading(false);
    }
  }

  async function toggleTail() {
    const tab = activeTab();
    if (!tab) return;
    const tailing = !tab.tailing;
    setTabs(current => current.map(candidate => candidate.id === tab.id ? { ...candidate, tailing } : candidate));
    const generation = ++tailGeneration;
    if (!tailing) return;
    setFollowTail(true);
    requestAnimationFrame(() => scrollToLatest(true));
    while (generation === tailGeneration) {
      if (filterReloadPending) {
        await new Promise(resolve => setTimeout(resolve, 75));
        continue;
      }
      const current = tabs().find(candidate => candidate.id === tab.id);
      if (!current?.tailing) break;
      await loadRecords(current, true, 15_000);
    }
  }

  function closeTab(id: string) {
    tailGeneration++;
    const remaining = tabs().filter(tab => tab.id !== id);
    setTabs(remaining);
    if (activeTabId() === id)
      setActiveTabId(remaining.at(-1)?.id);
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
    setFilter(update);
    scheduleFilterReload(delay);
  }

  function scheduleFilterReload(delay: number) {
    filterRevision++;
    filterReloadPending = true;
    if (filterReloadTimer)
      clearTimeout(filterReloadTimer);
    filterReloadTimer = setTimeout(async () => {
      filterReloadTimer = undefined;
      const tab = activeTab();
      if (tab)
        await loadRecords(tab, false);
      filterReloadPending = false;
    }, delay);
  }

  function levelRange(): string {
    const selected = levels.filter(level => filter().levels.includes(level));
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
    const distanceFromBottom = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight;
    setFollowTail(distanceFromBottom < 48);
  }

  function scrollToLatest(force = false) {
    if (!recordsViewport || (!force && !followTail())) return;
    recordsViewport.scrollTop = recordsViewport.scrollHeight;
  }

  function resumeFollowing() {
    setFollowTail(true);
    requestAnimationFrame(() => scrollToLatest(true));
  }

  function updateTailBuffer(value: string) {
    const parsed = Number.parseInt(value, 10);
    if (!Number.isFinite(parsed)) return;
    const bounded = Math.max(100, Math.min(20_000, parsed));
    setTailBufferSize(bounded);
    setTabs(current => current.map(tab => tab.tailing ? {
      ...tab,
      records: retainRecords(tab.records, true, bounded)
    } : tab));
  }

  function download(format: "raw" | "json") {
    const tab = activeTab();
    if (!tab) return;
    const content = format === "json"
      ? JSON.stringify(tab.records.map(recordToJson), null, 2)
      : tab.records.map(record => record.rawText || `${formatTimestamp(record.timestamp)} ${enumName(record.level)} ${record.loggerName} - ${record.message}`).join("\n");
    const blob = new Blob([content], { type: format === "json" ? "application/json" : "text/plain" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = `${safeName(tab.stream.displayName || tab.stream.streamId)}.${format === "json" ? "json" : "log"}`;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  function openBundle() {
    const application = applicationId() === "*" ? applications()[0] ?? "" : applicationId();
    const availableNodes = topology().filter(instance => instance.applicationId === application).map(instance => instance.nodeId);
    setBundleApplication(application);
    setBundleNode(nodeId() || "");
    setBundleStreams(fileStreams().map(stream => stream.streamId));
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

  async function createBundle() {
    const client = api();
    if (!client || !bundleApplication() || bundleStreams().length === 0)
      return;
    setLoading(true);
    setError(undefined);
    try {
      const result = await client.bundle({
        applicationId: bundleApplication(),
        nodeId: bundleNode()
      }, bundleStreams(), includeRotated(), bundleFormat(), filter());
      const url = URL.createObjectURL(result.blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = result.name;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 0);
      setBundleOpen(false);
    } catch (caught) {
      console.error("Creating log bundle failed", caught);
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }

  createEffect(() => {
    activeTabId();
    tailGeneration++;
    setSelectedRecord(undefined);
  });

  return (
    <div class="shell">
      <main>
        <section class="workspace">
          <aside class="streams-panel">
            <div class="sidebar-brand">
              <div class="brand">
                <span class="brand-mark"><i></i><i></i><i></i></span>
                <div><strong>Log Reflection</strong><small>hiconic observability</small></div>
              </div>
              <div class="identity">
                <span class="status-dot"></span>
                <div><strong>{user()}</strong><small>{roles().slice(0, 3).join(" · ") || "web session"}</small></div>
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
            </div>
            <div class="panel-heading"><div><strong>Streams</strong><small>{streams().length} available</small></div></div>
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
                {tab => <button classList={{ tab: true, active: tab.id === activeTabId() }} onClick={() => setActiveTabId(tab.id)}>
                  <span>{tab.stream.displayName || tab.stream.streamId}</span><small>{tab.target.nodeId || "all nodes"}</small>
                  <i onClick={event => { event.stopPropagation(); closeTab(tab.id); }}>×</i>
                </button>}
              </For>
            </div>

            <Show when={activeTab()} fallback={<Welcome />}>
              {tab => <>
                <div class="filters">
                  <div class="level-filter"><For each={levels}>{level =>
                    <button classList={{ [level.toLowerCase()]: true, selected: filter().levels.includes(level) }} onClick={() => toggleLevel(level)}>{level}</button>
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
                  <label class="node-filter"><span>Node</span>
                    <select value={nodeId()} onChange={event => selectNode(event.currentTarget.value)}>
                      <option value="">All nodes</option>
                      <For each={nodes()}>{node => <option value={node}>{node}</option>}</For>
                    </select>
                  </label>
                  <label class="search"><span>⌕</span><input placeholder="Full text" value={filter().fulltext ?? ""} onInput={e => changeFilter(f => ({ ...f, fulltext: e.currentTarget.value }))} /></label>
                  <label><span>Logger</span><input placeholder="contains logger" value={filter().logger ?? ""} onInput={e => changeFilter(f => ({ ...f, logger: e.currentTarget.value }))} /></label>
                  <label><span>From</span><input type="datetime-local" value={filter().from ?? ""} onChange={e => changeFilter(f => ({ ...f, from: e.currentTarget.value }), 0)} /></label>
                  <label><span>To</span><input type="datetime-local" value={filter().to ?? ""} onChange={e => changeFilter(f => ({ ...f, to: e.currentTarget.value }), 0)} /></label>
                </div>
                <div class="toolbar">
                  <span><strong>{tab().records.length.toLocaleString()}</strong> records</span>
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
                    <label class="tail-buffer" title="Maximum records retained while tailing">
                      Buffer
                      <input
                        type="number"
                        min="100"
                        max="20000"
                        step="100"
                        value={tailBufferSize()}
                        onChange={event => updateTailBuffer(event.currentTarget.value)}
                      />
                    </label>
                    <label class="history-scope" title="Include files discovered from the appender rolling policy">
                      History
                      <select value={includeRotated() ? "rotated" : "active"} onChange={event => {
                        setIncludeRotated(event.currentTarget.value === "rotated");
                        scheduleFilterReload(0);
                      }}>
                        <option value="active">Current</option>
                        <option value="rotated">Current + rotated</option>
                      </select>
                    </label>
                    <button classList={{ tail: true, active: tab().tailing }} onClick={toggleTail}><i></i>{tab().tailing ? "Tailing" : "Tail"}</button>
                    <Show when={tab().tailing}>
                      <button classList={{ follow: true, active: followTail() }} onClick={resumeFollowing}>
                        {followTail() ? "Following" : "Resume"}
                      </button>
                    </Show>
                    <button onClick={() => loadRecords(tab(), true)}>Load more</button>
                    <button onClick={() => download("raw")}>Raw ↓</button>
                    <button onClick={() => download("json")}>JSON ↓</button>
                    <button onClick={openBundle}>Bundle ↓</button>
                  </div>
                </div>
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
                          selected={record === selectedRecord()}
                          onSelect={() => setSelectedRecord(record)}
                        />}
                      </For>
                    </div>
                  </Show>
                  <Show when={selectedRecord()}>
                    {record => <LogRecordDetail record={record()} onClose={() => setSelectedRecord(undefined)} />}
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
          <section class="bundle-dialog" role="dialog" aria-modal="true" aria-label="Download log bundle">
            <header>
              <div><strong>Download log bundle</strong><small>Stream original files or canonical filtered JSONL without buffering the bundle in heap.</small></div>
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
                  <For each={bundleNodes()}>{node => <option value={node}>{node}</option>}</For>
                </select>
              </label>
              <label>Segments
                <select value={includeRotated() ? "rotated" : "active"} onChange={event => setIncludeRotated(event.currentTarget.value === "rotated")}>
                  <option value="active">Current files</option>
                  <option value="rotated">Current + rotated files</option>
                </select>
              </label>
              <label>Representation
                <select value={bundleFormat()} onChange={event => setBundleFormat(event.currentTarget.value as "raw" | "jsonl")}>
                  <option value="raw">Original files</option>
                  <option value="jsonl">Canonical JSONL</option>
                </select>
              </label>
            </div>
            <div class="bundle-streams">
              <strong>File streams</strong>
              <For each={fileStreams()} fallback={<span>No file streams are available in this application scope.</span>}>
                {stream => <label>
                  <input type="checkbox" checked={bundleStreams().includes(stream.streamId)}
                    onChange={() => toggleBundleStream(stream.streamId)} />
                  <span>{stream.displayName || stream.streamId}<small>{streamDetails(stream)}</small></span>
                </label>}
              </For>
            </div>
            <footer>
              <span>{bundleStreams().length} stream{bundleStreams().length === 1 ? "" : "s"} selected</span>
              <div><button onClick={() => setBundleOpen(false)}>Cancel</button><button class="primary" onClick={createBundle}>Create ZIP</button></div>
            </footer>
          </section>
        </div>
      </Show>
    </div>
  );
}

function LogRow(props: {
  record: LogRecord;
  columns: readonly ColumnDefinition[];
  template: string;
  selected: boolean;
  onSelect: () => void;
}) {
  return <div
    classList={{ "log-row": true, selected: props.selected }}
    style={`grid-template-columns: ${props.template}`}
    onClick={props.onSelect}
  >
    <For each={props.columns}>{column => <LogCell record={props.record} column={column.key} />}</For>
  </div>;
}

function LogCell(props: { record: LogRecord; column: ColumnKey }) {
  const level = () => enumName(props.record.level);
  if (props.column === "level")
    return <span><b class={`level ${level().toLowerCase()}`}>{level()}</b></span>;

  const value = columnValue(props.record, props.column);
  return <span class={props.column} title={value}>{value}</span>;
}

function TextLogRecord(props: {
  record: LogRecord;
  columns: readonly ColumnDefinition[];
  onSelect: () => void;
}) {
  const level = () => enumName(props.record.level).toLowerCase();
  const fields = () => props.columns
    .map(column => ({ column, value: textColumnValue(props.record, column.key) }))
    .filter(field => field.value !== "");
  return <span class="text-log-record" onClick={props.onSelect}><For each={fields()}>{(field, index) => <><Show when={index() > 0}><span class="text-separator">{"  "}</span></Show><span class={`text-field ${field.column.key} ${field.column.key === "level" ? level() : ""}`}>{field.value}</span></>}</For>{"\n"}</span>;
}

function textColumnValue(record: LogRecord, column: ColumnKey): string {
  switch (column) {
    case "message":
      return record.message || "—";
    case "throwable":
      return record.throwable || "";
    case "raw":
      return record.rawText || "—";
    default:
      return columnValue(record, column);
  }
}

function columnValue(record: LogRecord, column: ColumnKey): string {
  switch (column) {
    case "timestamp":
      return formatTimestamp(record.timestamp);
    case "level":
      return enumName(record.level) || "—";
    case "application":
      return record.origin?.applicationId || "—";
    case "node":
      return record.origin?.nodeId || "local";
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
    case "throwable":
      return oneLine(record.throwable) || "";
    case "properties":
      return formatProperties(record.properties);
    case "raw":
      return oneLine(record.rawText) || "—";
  }
}

function LogRecordDetail(props: { record: LogRecord; onClose: () => void }) {
  const properties = () => mapEntries(props.record.properties);

  return <aside class="record-detail">
    <header>
      <div><small>Log record</small><strong>{enumName(props.record.level)} · {formatTimestamp(props.record.timestamp)}</strong></div>
      <button title="Close details" onClick={props.onClose}>×</button>
    </header>
    <div class="detail-properties">
      <DetailProperty name="Application" value={props.record.origin?.applicationId} />
      <DetailProperty name="Node" value={props.record.origin?.nodeId || "local"} />
      <DetailProperty name="Stream" value={props.record.streamId} />
      <DetailProperty name="Logger" value={props.record.loggerName} />
      <DetailProperty name="Thread" value={props.record.threadName} />
      <DetailProperty name="Source" value={formatSourceLocation(props.record)} />
      <DetailProperty name="Sequence" value={props.record.position?.sequence?.toString()} />
      <For each={properties()}>{([name, value]) => <DetailProperty name={name} value={value} />}</For>
    </div>
    <section class="detail-content">
      <small>Message</small>
      <pre>{props.record.message || "—"}</pre>
      <Show when={props.record.throwable}>
        <small>Exception</small>
        <pre class="throwable">{props.record.throwable}</pre>
      </Show>
      <Show when={props.record.rawText && props.record.rawText !== props.record.message}>
        <small>Raw record</small>
        <pre class="raw">{props.record.rawText}</pre>
      </Show>
    </section>
  </aside>;
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

function redirectToLogin(config: RuntimeConfig, caught: unknown): never {
  const loginUrl = new URL(`${config.servicesUrl.replace(/\/$/, "")}/login`, window.location.origin);
  loginUrl.searchParams.set("continue", `${window.location.pathname}${window.location.search}${window.location.hash}`);
  window.location.replace(loginUrl);
  throw caught;
}

function isAuthenticationFailure(message: string) {
  return /MissingSession|MissingCredentials|AuthenticationFailure|SessionNotFound|No authenticated web session|Unauthorized|401/i.test(message);
}

function messageOf(caught: unknown) {
  return caught instanceof Error ? caught.message : String(caught);
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

function uniqueStreams(streams: readonly LogStreamDescriptor[]): LogStreamDescriptor[] {
  const unique = new Map<string, LogStreamDescriptor>();
  streams.forEach(stream => {
    if (!unique.has(stream.streamId))
      unique.set(stream.streamId, stream);
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
    const group = grouped.get(stream.streamId);
    if (group)
      group.push(stream);
    else
      grouped.set(stream.streamId, [stream]);
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

function formatProperties(value: Map<string, string> | Record<string, string> | null | undefined) {
  const entries = mapEntries(value);
  return entries.length ? entries.map(([name, item]) => `${name}=${item}`).join(" ") : "—";
}

function oneLine(value: string | null | undefined) {
  return value?.replace(/\s*\r?\n\s*/g, " ").trim();
}

function retainRecords(records: LogRecord[], tailing: boolean, capacity: number) {
  return tailing && records.length > capacity ? records.slice(-capacity) : records;
}

function shortLogger(value: string | null | undefined) {
  if (!value) return "—";
  const parts = value.split(".");
  return parts.length > 3 ? `…${parts.slice(-3).join(".")}` : value;
}

function mapEntries(value: Map<string, string> | Record<string, string> | null | undefined): [string, string][] {
  if (!value) return [];

  const modeledMap = value as {
    entries?: () => IterableIterator<[string, string]>;
  };
  if (typeof modeledMap.entries === "function")
    return Array.from(modeledMap.entries());

  return Object.entries(value);
}

function recordToJson(record: LogRecord) {
  return modeledValueToJson(record);
}

/**
 * Converts Generic Model values through their public model/collection APIs.
 * Looking at JavaScript object keys would expose obfuscated implementation state
 * instead of the modeled properties.
 */
function modeledValueToJson(value: unknown): unknown {
  if (value == null || typeof value !== "object")
    return value;
  if (value instanceof Date)
    return value.toISOString();

  const enumValue = value as { name?: () => string; EnumType?: () => unknown };
  if (typeof enumValue.name === "function" && typeof enumValue.EnumType === "function")
    return enumValue.name();

  const entity = value as {
    PropertyNames?: () => readonly string[];
    EntityType?: () => {
      getProperty: (name: string) => { get: (entity: unknown) => unknown };
    };
  };
  if (typeof entity.PropertyNames === "function" && typeof entity.EntityType === "function") {
    const entityType = entity.EntityType();
    return Object.fromEntries(entity.PropertyNames().map(name => [
      name,
      modeledValueToJson(entityType.getProperty(name).get(entity))
    ]));
  }

  const map = value as {
    get?: (key: unknown) => unknown;
    set?: (key: unknown, item: unknown) => unknown;
    entries?: () => IterableIterator<[unknown, unknown]>;
  };
  if (typeof map.get === "function" && typeof map.set === "function" && typeof map.entries === "function")
    return Object.fromEntries(Array.from(map.entries(), ([key, item]) => [String(key), modeledValueToJson(item)]));

  const set = value as {
    add?: (item: unknown) => unknown;
    values?: () => IterableIterator<unknown>;
  };
  if (typeof set.add === "function" && typeof set.values === "function")
    return Array.from(set.values(), modeledValueToJson);

  if (Symbol.iterator in value)
    return Array.from(value as Iterable<unknown>, modeledValueToJson);

  return String(value);
}

function safeName(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9._-]+/g, "-");
}
