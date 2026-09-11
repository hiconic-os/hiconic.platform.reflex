import { ApproveBrowserAcceptance, BrowserAcceptanceState, ListBrowserAcceptances, RejectBrowserAcceptance, RevokeBrowserAcceptance, type BrowserAcceptance } from "@dev.hiconic/platform.reflex_browser-acceptance-api-model";
import { remote } from "@dev.hiconic/tf.js_hc-js-api";

export interface RuntimeConfig { servicesUrl: string; }

export class BrowserAcceptanceApi {
  private readonly evaluator;
  constructor(config: RuntimeConfig) {
    const servicesUrl = new URL(config.servicesUrl, window.location.origin).toString().replace(/\/$/, "");
    this.evaluator = remote.connect(servicesUrl).evaluatorBuilder().setDefaultDomain("security").build();
  }
  async list(state?: BrowserAcceptanceState): Promise<BrowserAcceptance[]> {
    const request = ListBrowserAcceptances.create();
    request.state = state ?? null;
    const response = await this.evaluate<any>(request);
    return Array.from(response.acceptances ?? []);
  }
  approve(id: string) { return this.change(ApproveBrowserAcceptance.create(), id); }
  reject(id: string) { return this.change(RejectBrowserAcceptance.create(), id); }
  revoke(id: string) { return this.change(RevokeBrowserAcceptance.create(), id); }
  private change(request: any, id: string) { request.acceptanceId = id; return this.evaluate<BrowserAcceptance>(request); }
  private async evaluate<T>(request: any): Promise<T> {
    const result = await request.EvalAndGetReasoned(this.evaluator);
    if (result.isUnsatisfied()) { const reason = result.whyUnsatisfied(); throw new Error(reason?.Stringify?.() ?? String(reason)); }
    return result.get() as T;
  }
}
export { BrowserAcceptanceState };
export type { BrowserAcceptance };
