import { expect, test, type Page } from "@playwright/test";

async function openDownloadDialog(page: Page) {
  await page.getByRole("button", { name: /Download logs/ }).click();
  await expect(page.getByRole("dialog", { name: "Download remote logs" })).toBeVisible();
}

async function downloadArchive(page: Page) {
  const pending = page.waitForEvent("download");
  await page.getByRole("button", { name: "Download ZIP" }).click();
  return pending;
}

test.beforeEach(async ({ page }) => {
  await page.goto("/e2e.html");
  const applicationLog = page.locator(".stream-list button.stream").filter({ hasText: "Application log" });
  await expect(applicationLog).toBeVisible();
  await applicationLog.click();
  await expect(page.getByRole("button", { name: /Download logs/ })).toBeVisible();
});
test("masks a remote export and permits a second download without reloading", async ({ page }) => {
  await page.evaluate(() => window.logReflectionTestApi.setBundleDelay(250));
  await openDownloadDialog(page);

  const firstDownload = downloadArchive(page);
  const mask = page.getByRole("status");
  await expect(mask).toContainText("Creating remote log archive");
  await expect(page.getByRole("button", { name: "Download ZIP" })).toBeDisabled();
  await firstDownload;
  await expect(mask).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => window.logReflectionTestApi.bundleRequests())).toBe(1);

  await openDownloadDialog(page);
  const secondDownload = downloadArchive(page);
  await expect(mask).toContainText("Creating remote log archive");
  await secondDownload;
  await expect(mask).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => window.logReflectionTestApi.bundleRequests())).toBe(2);
});
