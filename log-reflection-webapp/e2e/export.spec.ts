import { readFile } from "node:fs/promises";
import { expect, test, type Download, type Page } from "@playwright/test";

async function openPopulatedView(page: Page) {
  await page.goto("/e2e.html");
  await page.locator(".stream-list button.stream").filter({ hasText: "Application log" }).click();
  const tail = page.locator(".toolbar button.tail");
  await tail.click();
  await expect.poll(() => page.evaluate(() => window.logReflectionTestApi.pendingCount("application"))).toBe(1);
  await page.evaluate(() => window.logReflectionTestApi.emit("application", [
    "first line\nsecond <&>",
    "another record"
  ]));
  await expect(page.locator(".log-row")).toHaveCount(2);
  await tail.click();

  await page.locator(".column-picker summary").click();
  await page.locator(".column-picker label").filter({ hasText: "Node" }).getByRole("checkbox").uncheck();
}

async function exportView(page: Page, format: string): Promise<{ download: Download; content: string }> {
  await page.locator(".view-export summary").click();
  const pending = page.waitForEvent("download");
  await page.getByRole("menuitem", { name: format }).click();
  const download = await pending;
  const path = await download.path();
  if (!path) throw new Error("Browser did not expose the downloaded view export");
  return { download, content: await readFile(path, "utf8") };
}

test.beforeEach(async ({ page }) => {
  await openPopulatedView(page);
});

test("exports visible tab columns as multiline plain text", async ({ page }) => {
  const { download, content } = await exportView(page, "Plain text");

  expect(download.suggestedFilename()).toBe("application-log_merged_view.txt");
  expect(content).toContain("test.application  first line\nsecond <&>");
  expect(content).toContain("test.application  another record");
  expect(content).not.toContain("node-a");
});

test("exports one selected-column JSON object per physical line", async ({ page }) => {
  const { download, content } = await exportView(page, "JSONL");
  const lines = content.split("\n");
  const records = lines.map(line => JSON.parse(line));

  expect(download.suggestedFilename()).toBe("application-log_merged_view.jsonl");
  expect(lines).toHaveLength(2);
  expect(records[0].message).toBe("first line\nsecond <&>");
  expect(records[0].logger).toBe("test.application");
  expect(records[0]).not.toHaveProperty("node");
});

test("exports an Excel-compatible XML table without losing multiline text", async ({ page }) => {
  const { download, content } = await exportView(page, "Excel XML");

  expect(download.suggestedFilename()).toBe("application-log_merged_view.xml");
  expect(content).toContain("<?mso-application progid=\"Excel.Sheet\"?>");
  expect(content).toContain("ss:ExpandedColumnCount=\"4\"");
  expect(content).toContain("ss:ExpandedRowCount=\"3\"");
  expect(content).toContain("ss:Type=\"DateTime\"");
  expect(content).toContain("first line\nsecond &lt;&amp;&gt;");
  expect(content).not.toContain(">Node<");
});
