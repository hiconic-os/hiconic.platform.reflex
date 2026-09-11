import { For, Show, createSignal, onMount } from "solid-js";
import { BrowserAcceptanceApi, BrowserAcceptanceState, type BrowserAcceptance, type RuntimeConfig } from "./api";

export function App() {
  const [api, setApi] = createSignal<BrowserAcceptanceApi>();
  const [items, setItems] = createSignal<BrowserAcceptance[]>([]);
  const [state, setState] = createSignal<BrowserAcceptanceState | undefined>(BrowserAcceptanceState.PENDING);
  const [busy, setBusy] = createSignal(false);
  const [error, setError] = createSignal<string>();
  onMount(async () => { const config = await fetch("./runtime-config.json").then(r => r.json()) as RuntimeConfig; const client = new BrowserAcceptanceApi(config); setApi(client); await refresh(client); });
  async function refresh(client = api()) { if (!client) return; setBusy(true); setError(); try { setItems(await client.list(state())); } catch (e) { setError(e instanceof Error ? e.message : String(e)); } finally { setBusy(false); } }
  async function change(action: "approve" | "reject" | "revoke", id: string) { const client = api(); if (!client) return; setBusy(true); setError(); try { await client[action](id); await refresh(client); } catch (e) { setError(e instanceof Error ? e.message : String(e)); setBusy(false); } }
  return <main>
    <header><div><span class="eyebrow">hiconic security</span><h1>Device approvals</h1><p class="intro">An approval recognizes this browser on this device.</p></div><button onClick={() => refresh()} disabled={busy()}>Refresh</button></header>
    <nav>
      <button classList={{active: state() === BrowserAcceptanceState.PENDING}} onClick={() => { setState(BrowserAcceptanceState.PENDING); refresh(); }}>Pending</button>
      <button classList={{active: state() === BrowserAcceptanceState.APPROVED}} onClick={() => { setState(BrowserAcceptanceState.APPROVED); refresh(); }}>Approved</button>
      <button classList={{active: state() === undefined}} onClick={() => { setState(); refresh(); }}>All</button>
    </nav>
    <Show when={error()}><div class="error">{error()}</div></Show>
    <section class="list"><Show when={items().length} fallback={<div class="empty">No device approvals in this view.</div>}>
      <For each={items()}>{item => <article>
        <div class="card-header">
          <div class="identity">
            <strong>{item.userName || item.userId || "Unknown user"}</strong>
            <span>{item.userId || "No user ID"}</span>
          </div>
          <span class={`state ${String(item.state).toLowerCase()}`}>{stateLabel(item.state)}</span>
          <div class="actions"><Show when={item.state === BrowserAcceptanceState.PENDING}><button class="primary" onClick={() => change("approve", item.id!)}>Approve device</button><button onClick={() => change("reject", item.id!)}>Reject</button></Show><Show when={item.state === BrowserAcceptanceState.APPROVED}><button class="danger" onClick={() => change("revoke", item.id!)}>Revoke approval</button></Show></div>
        </div>
        <dl class="details">
          <Detail label="Browser" value={describeClient(item)} title={item.userAgent} />
          <Detail label="Entry point" value={item.entryPoint || "All entry points"} />
          <Detail label="Requested" value={format(item.createdAt)} />
          <Detail label="Last seen" value={format(item.lastSeenAt)} />
          <Detail label="Client address" value={addressRange(item.requestorAddress, item.lastRequestorAddress)} />
          <Show when={directPeer(item)}>{value => <Detail label="Direct peer" value={value()} />}</Show>
        </dl>
      </article>}</For>
    </Show></section>
  </main>;
}
function Detail(props: { label: string; value: string; title?: unknown }) {
  return <div><dt>{props.label}</dt><dd title={String(props.title || "")}>{props.value}</dd></div>;
}
function stateLabel(value: unknown) {
  return String(value || "unknown").toLowerCase().replace(/^./, character => character.toUpperCase());
}
function format(value: unknown) { return value ? new Date(value as any).toLocaleString() : "—"; }
function addressRange(first: unknown, last: unknown) {
  const initial = String(first || "unknown");
  const latest = String(last || first || "unknown");
  return initial === latest ? initial : `${initial} → ${latest}`;
}
function directPeer(item: BrowserAcceptance) {
  const direct = addressRange(item.directRequestorAddress, item.lastDirectRequestorAddress);
  const client = addressRange(item.requestorAddress, item.lastRequestorAddress);
  return direct !== "unknown" && direct !== client ? direct : undefined;
}
function describeClient(item: BrowserAcceptance) {
  const ua = String(item.userAgent || "");
  const brands = cleanHint(item.clientHintsUserAgent);
  const browser = brands || match(ua, /Edg\/([\d.]+)/, "Edge") || match(ua, /Chrome\/([\d.]+)/, "Chrome")
    || match(ua, /Firefox\/([\d.]+)/, "Firefox") || match(ua, /Version\/([\d.]+).*Safari\//, "Safari") || "Unknown browser";
  const platformHint = cleanHint(item.clientHintsPlatform);
  const platform = platformHint || (/Windows/i.test(ua) ? "Windows" : /Android/i.test(ua) ? "Android" : /iPhone|iPad/i.test(ua) ? "iOS"
    : /Mac OS X/i.test(ua) ? "macOS" : /Linux/i.test(ua) ? "Linux" : "Unknown platform");
  const mobile = String(item.clientHintsMobile || "") === "?1" || /Mobile/i.test(ua);
  return `${browser} · ${platform}${mobile ? " · Mobile" : ""}`;
}
function cleanHint(value: unknown) { return String(value || "").replaceAll('"', "").trim(); }
function match(value: string, pattern: RegExp, name: string) { const result = value.match(pattern); return result ? `${name} ${result[1]}` : undefined; }
