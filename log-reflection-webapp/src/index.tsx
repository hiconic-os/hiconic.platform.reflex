import "@dev.hiconic/gm_gm-core-api";
import "@dev.hiconic/gm_security-reason-model";
import { render } from "solid-js/web";
import { LogReflectionApi } from "./api";
import "./styles.css";
import "./access.css";

const { App } = await import("./App");

render(() => <App apiFactory={config => new LogReflectionApi(config)} />, document.getElementById("root")!);
