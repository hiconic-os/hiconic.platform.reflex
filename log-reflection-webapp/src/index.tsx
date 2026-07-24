import "@dev.hiconic/gm_gm-core-api";
import { render } from "solid-js/web";
import "./styles.css";

const { App } = await import("./App");

render(() => <App />, document.getElementById("root")!);
