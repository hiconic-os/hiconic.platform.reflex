import { expect, test, type Page } from "@playwright/test";

const streamButton = (page: Page, name: string) => page.locator(".stream-list button.stream").filter({ hasText: name });
const tab = (page: Page, name: string) => page.locator(".tabs button.tab").filter({ hasText: name });
const tailButton = (page: Page) => page.locator(".toolbar button.tail");

async function openStream(page: Page, name: string) {
  await streamButton(page, name).click();
  await expect(tab(page, name)).toHaveClass(/active/);
}

async function startTail(page: Page, streamId: string) {
  await tailButton(page).click();
  await expect(tailButton(page)).toContainText("Tailing");
  await expect.poll(() => page.evaluate(id => window.logReflectionTestApi.pendingCount(id), streamId)).toBe(1);
}

async function emit(page: Page, streamId: string, messages: string | string[]) {
  const emitted = await page.evaluate(
    ({ streamId, messages }) => window.logReflectionTestApi.emit(streamId, messages),
    { streamId, messages }
  );
  expect(emitted).toBe(true);
}

test.beforeEach(async ({ page }) => {
  await page.goto("/e2e.html");
  await expect(streamButton(page, "Application log")).toBeVisible();
});

test("keeps a tail alive while switching tabs", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");

  await openStream(page, "Security log");
  await expect(tab(page, "Application log")).toHaveClass(/tailing/);
  await emit(page, "application", "arrived in the background");

  await tab(page, "Application log").click();
  await expect(page.getByText("arrived in the background", { exact: true })).toBeVisible();
});

test("tails multiple tabs independently", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");
  await openStream(page, "Security log");
  await startTail(page, "security");

  await emit(page, "application", "application event");
  await emit(page, "security", "security event");
  await expect(page.getByText("security event", { exact: true })).toBeVisible();

  await tab(page, "Application log").click();
  await expect(page.getByText("application event", { exact: true })).toBeVisible();
  await expect(tab(page, "Security log")).toHaveClass(/tailing/);
});

test("discards a delayed response after stopping its tail", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");

  await tailButton(page).click();
  await expect(tailButton(page)).toContainText("Tail");
  await emit(page, "application", "must be discarded");

  await expect(page.getByText("must be discarded", { exact: true })).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => window.logReflectionTestApi.pendingCount("application"))).toBe(0);
});

test("closing one tab does not stop another tab", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");
  await openStream(page, "Security log");
  await startTail(page, "security");

  await tab(page, "Application log").locator("i").click();
  await emit(page, "application", "closed tab event");
  await emit(page, "security", "surviving tab event");

  await expect(tab(page, "Application log")).toHaveCount(0);
  await expect(tab(page, "Security log")).toHaveClass(/tailing/);
  await expect(page.getByText("surviving tab event", { exact: true })).toBeVisible();
});

test("keeps filters and history scope local to each tab", async ({ page }) => {
  await openStream(page, "Application log");
  await page.getByPlaceholder("contains logger").fill("test.application");
  await page.locator("select").filter({ has: page.locator("option[value=rotated]") }).selectOption("rotated");

  await openStream(page, "Security log");
  await expect(page.getByPlaceholder("contains logger")).toHaveValue("");
  await expect(page.locator("select").filter({ has: page.locator("option[value=rotated]") })).toHaveValue("active");
  await page.getByPlaceholder("contains logger").fill("test.security");

  await tab(page, "Application log").click();
  await expect(page.getByPlaceholder("contains logger")).toHaveValue("test.application");
  await expect(page.locator("select").filter({ has: page.locator("option[value=rotated]") })).toHaveValue("rotated");
});

test("rejects an in-flight result from an obsolete filter", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");

  await page.getByPlaceholder("contains logger").fill("fresh.filter");
  await emit(page, "application", "stale filtered event");
  await expect(page.getByText("stale filtered event", { exact: true })).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => window.logReflectionTestApi.pendingFilters("application"))).toEqual(["fresh.filter"]);
});

test("stops all tails and rejects pending results after access loss", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");

  await page.evaluate(() => {
    window.logReflectionTestApi.setAuthenticated(false);
    window.dispatchEvent(new Event("focus"));
  });
  await expect(page.getByText("Session required", { exact: true })).toBeVisible();
  await expect(tab(page, "Application log")).not.toHaveClass(/tailing/);
  await emit(page, "application", "response after access loss");
  await expect(page.getByText("response after access loss", { exact: true })).toHaveCount(0);
});

test("pauses following when scrolled away and resumes on request", async ({ page }) => {
  await openStream(page, "Application log");
  await startTail(page, "application");
  await emit(page, "application", Array.from({ length: 180 }, (_, index) => `record ${index}`));
  await expect(page.locator(".log-row")).toHaveCount(180);

  await page.locator(".log-table").evaluate(element => {
    element.scrollTop = 0;
    element.dispatchEvent(new Event("scroll"));
  });
  await expect(page.getByRole("button", { name: "Resume" })).toBeVisible();
  await page.getByRole("button", { name: "Resume" }).click();
  await expect(page.getByRole("button", { name: "Following" })).toBeVisible();
  await expect.poll(() => page.locator(".log-table").evaluate(element =>
    element.scrollHeight - element.scrollTop - element.clientHeight
  )).toBeLessThan(2);
});
