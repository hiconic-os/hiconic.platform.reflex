import "@dev.hiconic/gm_gm-core-api";
import "@dev.hiconic/gm_security-reason-model";
import { render } from "solid-js/web";
import { App } from "../src/App";
import "../src/styles.css";
import "../src/access.css";
import { mockLogApi } from "./mock-api";

window.logReflectionTestApi = mockLogApi;

render(() => <App
  apiFactory={() => mockLogApi}
  configLoader={async () => ({ servicesUrl: "http://mock.invalid/services" })}
/>, document.getElementById("root")!);
