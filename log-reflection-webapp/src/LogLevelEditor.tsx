import { For, Show, createMemo, createSignal, onCleanup, onMount } from "solid-js";
import type { LogLevelStateView, LogReflectionClient } from "./api";

const levels = ["ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF"];
const newProfileChoice = "__new__";

function LevelValue(props: { level?: string; empty?: string; effective?: boolean }) {
  return <span class={`log-level-value ${props.level?.toLowerCase() || "unset"}${props.effective ? " effective" : ""}`}>
    {props.level || props.empty || "—"}
  </span>;
}

function LoggerName(props: { name: string }) {
  const lastDot = props.name.lastIndexOf(".");
  const shortName = props.name.slice(lastDot + 1);
  return <code class="logger-name" title={props.name}>
    {lastDot >= 0 && <><span class="logger-prefix"><bdi dir="ltr">{props.name.slice(0, lastDot)}</bdi></span><span>.</span></>}
    <strong>{shortName || "<root>"}</strong>
  </code>;
}

function resolvedLevel(name: string, configured: ReadonlyMap<string, string>): string | undefined {
  let candidate = name;
  while (candidate) {
    const level = configured.get(candidate);
    if (level) return level;
    const dot = candidate.lastIndexOf(".");
    if (dot < 0) break;
    candidate = candidate.slice(0, dot);
  }
  return configured.get("ROOT");
}

export function LogLevelEditor(props: { client: LogReflectionClient; applicationId: string; canManage: boolean }) {
  const [state, setState] = createSignal<LogLevelStateView>();
  const [search, setSearch] = createSignal("");
  const [busy, setBusy] = createSignal(false);
  const [failure, setFailure] = createSignal("");
  const [dropIndex, setDropIndex] = createSignal<number | undefined>();
  const [dropGuideLeft, setDropGuideLeft] = createSignal(0);
  const [draggedProfile, setDraggedProfile] = createSignal("");
  const [showAdd, setShowAdd] = createSignal(false);
  const [deleteCandidate, setDeleteCandidate] = createSignal("");
  const [profileChoice, setProfileChoice] = createSignal(newProfileChoice);
  const [newProfile, setNewProfile] = createSignal("");
  const [newLogger, setNewLogger] = createSignal("");
  const [newLevel, setNewLevel] = createSignal("DEBUG");
  let stopResizing: (() => void) | undefined;

  const cascade = createMemo(() => state()?.effectiveProfileCascade ?? []);
  const usingRuntimeCascade = createMemo(() => {
    const current = state();
    return Boolean(current && (current.runtimeProfileCascade.length > 0 ||
      (current.effectiveProfileCascade.length === 0 && current.packagedProfileCascade.length > 0)));
  });
  const packagedNames = createMemo(() => [...(state()?.packagedProfiles.keys() ?? [])].sort());
  const runtimeNames = createMemo(() => [...(state()?.runtimeProfiles.keys() ?? [])].sort());
  const allProfileNames = createMemo(() => new Set([...packagedNames(), ...runtimeNames()]));
  const allLoggerNames = createMemo(() => {
    const names = new Set<string>();
    const current = state();
    if (current) {
      for (const levels of current.packagedProfiles.values()) for (const name of levels.keys()) names.add(name);
      for (const levels of current.runtimeProfiles.values()) for (const name of levels.keys()) names.add(name);
    }
    return [...names].sort();
  });
  const loggerNames = createMemo(() => {
    const needle = search().trim().toLocaleLowerCase();
    return allLoggerNames().filter(name => !needle || name.toLocaleLowerCase().includes(needle));
  });
  const loggerSuggestions = createMemo(() => {
    const names = new Set([...(state()?.knownLoggerNames ?? []), ...loggerNames()]);
    return [...names].sort();
  });
  const [loggerWidth, setLoggerWidth] = createSignal(420);
  const profileWidth = (profile: string) => Math.max(112, Math.ceil(profile.length * 7 + (props.canManage ? 38 : 16)));
  const gridColumns = createMemo(() => `${loggerWidth()}px ${cascade().map(profile => `${profileWidth(profile)}px`).join(" ")} 102px`);
  const gridWidth = createMemo(() => `${loggerWidth() + cascade().reduce((width, profile) => width + profileWidth(profile), 0) + 102 + (cascade().length + 1) * 6 + 28}px`);

  onMount(() => {
    void refresh();
    const onEscape = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (deleteCandidate()) setDeleteCandidate("");
      else if (showAdd()) setShowAdd(false);
      else return;
      event.preventDefault();
      event.stopPropagation();
    };
    window.addEventListener("keydown", onEscape, true);
    onCleanup(() => { window.removeEventListener("keydown", onEscape, true); stopResizing?.(); });
  });

  async function refresh() {
    setBusy(true);
    try {
      setState(await props.client.logLevelState(props.applicationId));
      setFailure("");
    } catch (error) {
      fail("Loading log levels", error);
    } finally {
      setBusy(false);
    }
  }

  function fail(action: string, error: unknown) {
    console.error(`${action} failed`, error);
    setFailure(error instanceof Error ? error.message : String(error));
  }

  async function change(action: string, operation: () => Promise<void>, optimisticCascade?: string[]): Promise<boolean> {
    if (!props.canManage || busy()) return false;
    const previous = state();
    setBusy(true);
    setFailure("");
    if (previous && optimisticCascade)
      setState({ ...previous, runtimeProfileCascade: optimisticCascade, effectiveProfileCascade: optimisticCascade });
    try {
      await operation();
      setState(await props.client.logLevelState(props.applicationId));
      return true;
    } catch (error) {
      if (previous && optimisticCascade) setState(previous);
      fail(action, error);
      return false;
    } finally {
      setBusy(false);
    }
  }

  function isRuntime(profile: string) {
    return Boolean(state()?.runtimeProfiles.has(profile));
  }

  function profileLevels(profile: string) {
    const current = state();
    return current?.runtimeProfiles.get(profile) ?? current?.packagedProfiles.get(profile);
  }

  async function insertProfile(profile: string, rawIndex: number) {
    if (!allProfileNames().has(profile)) return;
    const order = [...cascade()];
    const oldIndex = order.indexOf(profile);
    if (oldIndex >= 0) order.splice(oldIndex, 1);
    const index = Math.max(0, Math.min(order.length, rawIndex - (oldIndex >= 0 && oldIndex < rawIndex ? 1 : 0)));
    order.splice(index, 0, profile);
    if (order.join("\u0000") === cascade().join("\u0000")) return;
    await change("Updating profiles", () => props.client.setLogLevelProfileCascade(props.applicationId, order), order);
  }

  async function removeProfile(profile: string) {
    const remaining = cascade().filter(name => name !== profile);
    await change("Removing profile", () => props.client.setLogLevelProfileCascade(props.applicationId, remaining), remaining);
  }

  function startDrag(event: DragEvent, profile: string) {
    if (!props.canManage || busy()) { event.preventDefault(); return; }
    setDraggedProfile(profile);
    event.dataTransfer?.setData("text/plain", profile);
    event.dataTransfer?.setData("application/x-hiconic-log-profile", profile);
    if (event.dataTransfer) event.dataTransfer.effectAllowed = "move";
  }

  function insertionAt(table: HTMLElement, clientX: number) {
    const header = table.querySelector<HTMLElement>(".log-level-header");
    if (!header) return 0;
    const columns = [...header.querySelectorAll<HTMLElement>(".profile-column")];
    const index = columns.findIndex(column => clientX < column.getBoundingClientRect().left + column.getBoundingClientRect().width / 2);
    const insertionIndex = index < 0 ? columns.length : index;
    const before = insertionIndex === 0
      ? header.querySelector<HTMLElement>(".logger-column")?.getBoundingClientRect()
      : columns[insertionIndex - 1].getBoundingClientRect();
    const after = insertionIndex === columns.length
      ? header.querySelector<HTMLElement>(".effective-column")?.getBoundingClientRect()
      : columns[insertionIndex].getBoundingClientRect();
    const boundary = before && after ? (before.right + after.left) / 2 : after?.left ?? before?.right ?? header.getBoundingClientRect().left;
    setDropGuideLeft(boundary - header.getBoundingClientRect().left - 3.5);
    setDropIndex(insertionIndex);
    return insertionIndex;
  }

  function dragOver(event: DragEvent) {
    if (!props.canManage || (!draggedProfile() && !event.dataTransfer?.types.includes("application/x-hiconic-log-profile"))) return;
    event.preventDefault();
    insertionAt(event.currentTarget as HTMLElement, event.clientX);
    if (event.dataTransfer) event.dataTransfer.dropEffect = "move";
  }

  function drop(event: DragEvent) {
    if (!props.canManage) return;
    const profile = event.dataTransfer?.getData("application/x-hiconic-log-profile") || draggedProfile();
    if (!profile) return;
    event.preventDefault();
    const index = insertionAt(event.currentTarget as HTMLElement, event.clientX);
    setDropIndex(undefined);
    setDraggedProfile("");
    void insertProfile(profile, index);
  }

  function resizeLogger(event: PointerEvent) {
    event.preventDefault();
    stopResizing?.();
    const startX = event.clientX;
    const startWidth = loggerWidth();
    const onMove = (move: PointerEvent) => setLoggerWidth(Math.max(260, Math.min(900, startWidth + move.clientX - startX)));
    const onEnd = () => {
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerup", onEnd);
      stopResizing = undefined;
    };
    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onEnd);
    stopResizing = onEnd;
  }

  function openAdd() {
    setProfileChoice(runtimeNames()[0] ?? newProfileChoice);
    setNewProfile("");
    setNewLogger("");
    setNewLevel("DEBUG");
    setFailure("");
    setShowAdd(true);
  }

  async function addEntry() {
    const profile = (profileChoice() === newProfileChoice ? newProfile() : profileChoice()).trim();
    const logger = newLogger().trim();
    if (!profile || !logger) { setFailure("Profile and logger name are required."); return; }
    if (state()?.packagedProfiles.has(profile)) { setFailure(`Packaged profile ${profile} is immutable.`); return; }
    if (await change("Adding logger level", () => props.client.setRuntimeLogLevel(props.applicationId, logger, newLevel(), profile)))
      setShowAdd(false);
  }

  async function deleteProfile(profile: string) {
    const before = [...(state()?.runtimeProfileCascade ?? [])];
    const remaining = before.filter(name => name !== profile);
    const success = await change("Deleting runtime profile", async () => {
      await props.client.clearRuntimeLogLevels(props.applicationId, profile);
      if (remaining.length)
        await props.client.setLogLevelProfileCascade(props.applicationId, remaining);
      else
        await props.client.resetLogLevelProfileCascade(props.applicationId);
    });
    if (success) setDeleteCandidate("");
  }

  async function setLevel(profile: string, logger: string, level: string) {
    await change("Updating runtime level", () => level
      ? props.client.setRuntimeLogLevel(props.applicationId, logger, level, profile)
      : props.client.resetRuntimeLogLevel(props.applicationId, logger, profile));
  }

  return <section class="log-level-workspace">
    <div class="log-level-toolbar">
      <div class="log-level-actions">
        <button onClick={() => void refresh()} disabled={busy()}>Refresh</button>
        <Show when={props.canManage}>
          <button class="primary" onClick={openAdd} disabled={busy()}>Add logger level</button>
          <button onClick={() => void change("Restoring packaged profiles", () => props.client.resetLogLevelProfileCascade(props.applicationId))}
            disabled={busy() || !usingRuntimeCascade()}>Use packaged profiles</button>
        </Show>
      </div>
      <label class="search"><span>⌕</span><input placeholder="Filter logger names" value={search()}
        onInput={event => setSearch(event.currentTarget.value)} /></label>
    </div>
    <div class="log-level-profiles">
      <div><small>Packaged profiles</small><div class="profile-badges"><For each={packagedNames()}>{name =>
        <button class="profile-badge packaged" title={props.canManage ? "Add profile or drag into the table" : name}
          disabled={!props.canManage || busy()} draggable={props.canManage && !busy()}
          onDragStart={event => startDrag(event, name)} onDragEnd={() => { setDraggedProfile(""); setDropIndex(undefined); }}
          onClick={() => void insertProfile(name, cascade().length)}>{name}</button>
      }</For></div></div>
      <div><small>Runtime profiles</small><div class="profile-badges"><For each={runtimeNames()}>{name =>
        <span class="profile-badge runtime">
          <button title={props.canManage ? "Add profile or drag into the table" : name}
            disabled={!props.canManage || busy()} draggable={props.canManage && !busy()}
            onDragStart={event => startDrag(event, name)} onDragEnd={() => { setDraggedProfile(""); setDropIndex(undefined); }}
            onClick={() => void insertProfile(name, cascade().length)}>{name}</button>
          <Show when={props.canManage}><button class="profile-delete" aria-label={`Delete runtime profile ${name}`}
            disabled={busy()} onClick={() => setDeleteCandidate(name)}>×</button></Show>
        </span>
      }</For><Show when={!runtimeNames().length}><span class="profile-empty">No runtime profiles yet</span></Show></div></div>
    </div>
    <Show when={failure()}>{message => <div class="log-level-error" role="alert">{message()}</div>}</Show>
    <div class="log-level-table" onDragOver={dragOver} onDrop={drop}>
      <div class="log-level-header" style={`grid-template-columns: ${gridColumns()}; min-width: ${gridWidth()}`}>
        <span class="logger-column">Logger<span class="logger-resize" role="separator" aria-label="Resize logger column"
          aria-orientation="vertical" tabIndex={0} onPointerDown={resizeLogger}
          onKeyDown={event => {
            if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
              setLoggerWidth(Math.max(260, Math.min(900, loggerWidth() + (event.key === "ArrowRight" ? 24 : -24))));
              event.preventDefault();
            }
          }} /></span>
        <For each={cascade()}>{profile =>
          <span class="profile-column"
            title={profile} draggable={props.canManage && !busy()}
            onDragStart={event => startDrag(event, profile)}
            onDragEnd={() => { setDraggedProfile(""); setDropIndex(undefined); }}>
            <span class={`profile-badge ${isRuntime(profile) ? "runtime" : "packaged"}`}>
              <span class="profile-label">{profile}</span>
              <Show when={props.canManage}>
                <button class="remove-profile" aria-label={`Remove profile ${profile} from table`}
                  title="Remove from table" disabled={busy()} onClick={() => void removeProfile(profile)}>×</button>
              </Show>
            </span>
          </span>}
        </For>
        <span class="effective-column">Effective</span>
        <Show when={dropIndex() !== undefined}>
          <svg class="log-level-drop-guide" style={`left: ${dropGuideLeft()}px`} viewBox="0 0 7 30" aria-hidden="true">
            <path d="M1 0V10L5 15L1 20V30" />
          </svg>
        </Show>
      </div>
      <Show when={state()} fallback={<div class="empty records">{busy() ? "Loading log levels…" : "No log-level state available."}</div>}>
        {current => <For each={loggerNames()} fallback={<div class="empty records">
          {search().trim() ? "No logger matches the filter." : "No logger levels configured."}
        </div>}>{name =>
          <div class="log-level-row" style={`grid-template-columns: ${gridColumns()}; min-width: ${gridWidth()}`}>
            <LoggerName name={name} />
            <For each={cascade()}>{profile =>
              <Show when={isRuntime(profile) && props.canManage} fallback={
                <LevelValue level={profileLevels(profile)?.get(name)} empty="—" />
              }>
                <details class="runtime-level-menu" data-popup>
                  <summary aria-label={`Change ${name || "root"} in runtime profile ${profile}`}>
                    <LevelValue level={profileLevels(profile)?.get(name)} empty="—" />
                  </summary>
                  <div class="runtime-level-options">
                    <For each={levels}>{level => <button type="button"
                      class={profileLevels(profile)?.get(name) === level ? "selected" : ""}
                      disabled={busy()} onClick={event => {
                        event.currentTarget.closest("details")?.removeAttribute("open");
                        void setLevel(profile, name, level);
                      }}><LevelValue level={level} /></button>}</For>
                    <button type="button" class="reset-runtime-level" disabled={busy() || !profileLevels(profile)?.has(name)}
                      onClick={event => {
                        event.currentTarget.closest("details")?.removeAttribute("open");
                        void setLevel(profile, name, "");
                      }}>Reset override</button>
                  </div>
                </details>
              </Show>
            }</For>
            <LevelValue level={resolvedLevel(name, current().effectiveLevels)} effective empty="unknown" />
          </div>}
        </For>}
      </Show>
    </div>
    <Show when={busy()}><div class="log-level-progress"></div></Show>
    <Show when={showAdd()}>
      <div class="modal-backdrop" onPointerDown={event => { if (event.target === event.currentTarget) setShowAdd(false); }}>
        <section class="log-level-dialog" role="dialog" aria-modal="true" aria-label="Add logger level">
          <header><strong>Add logger level</strong><button onClick={() => setShowAdd(false)} aria-label="Close">×</button></header>
          <div class="dialog-fields">
            <label>Runtime profile<select value={profileChoice()} onChange={event => setProfileChoice(event.currentTarget.value)}>
              <For each={runtimeNames()}>{name => <option value={name}>{name}</option>}</For>
              <option value={newProfileChoice}>Create new runtime profile…</option>
            </select></label>
            <Show when={profileChoice() === newProfileChoice}><label>New profile name<input value={newProfile()}
              onInput={event => setNewProfile(event.currentTarget.value)} placeholder="runtime-default" /></label></Show>
            <label>Logger<input list="log-level-logger-suggestions" value={newLogger()} spellcheck={false}
              onInput={event => setNewLogger(event.currentTarget.value)} placeholder="com.example.Logger" /></label>
            <datalist id="log-level-logger-suggestions"><For each={loggerSuggestions()}>{name => <option value={name} />}</For></datalist>
            <label>Level<select value={newLevel()} onChange={event => setNewLevel(event.currentTarget.value)}>
              <For each={levels}>{level => <option value={level}>{level}</option>}</For>
            </select></label>
          </div>
          <Show when={failure()}>{message => <div class="log-level-error" role="alert">{message()}</div>}</Show>
          <footer><button onClick={() => setShowAdd(false)}>Cancel</button>
            <button class="primary" disabled={busy()} onClick={() => void addEntry()}>Add level</button></footer>
        </section>
      </div>
    </Show>
    <Show when={deleteCandidate()}>{profile =>
      <div class="modal-backdrop" onPointerDown={event => { if (event.target === event.currentTarget) setDeleteCandidate(""); }}>
        <section class="log-level-dialog" role="alertdialog" aria-modal="true" aria-label="Delete runtime profile">
          <header><strong>Delete runtime profile?</strong><button onClick={() => setDeleteCandidate("")} aria-label="Close">×</button></header>
          <p>Remove <strong>{profile()}</strong> and all its logger levels?</p>
          <Show when={failure()}>{message => <div class="log-level-error" role="alert">{message()}</div>}</Show>
          <footer><button onClick={() => setDeleteCandidate("")}>Cancel</button>
            <button class="danger-subtle" disabled={busy()} onClick={() => void deleteProfile(profile())}>Delete profile</button></footer>
        </section>
      </div>
    }</Show>
  </section>;
}
