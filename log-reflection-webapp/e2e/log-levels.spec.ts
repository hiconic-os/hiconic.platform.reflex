import { expect, test } from "@playwright/test";

test("edits a runtime profile, reorders the cascade and resets to packaged", async ({ page }) => {
  await page.goto("/e2e.html");
  await page.locator(".levels-action").click();
  const editor = page.locator(".log-level-workspace");
  await expect(editor.getByRole("button", { name: "Use packaged profiles" })).toBeDisabled();
  await expect(editor.locator(".log-level-header .profile-column")).toHaveCount(1);

  await editor.getByRole("button", { name: "Add logger level" }).click();
  const dialog = page.getByRole("dialog", { name: "Add logger level" });
  await dialog.getByLabel("New profile name").fill("my-runtime");
  await dialog.getByLabel("Logger", { exact: true }).fill("example.Logger");
  await dialog.locator("select").last().selectOption("ERROR");
  await dialog.getByRole("button", { name: "Add level" }).click();
  await expect(dialog).toHaveCount(0);
  await expect(editor.getByRole("button", { name: "Use packaged profiles" })).toBeEnabled();
  await expect(editor.locator(".log-level-header .profile-column")).toHaveCount(2);
  await expect(editor.locator(".log-level-row").filter({ hasText: "example.Logger" }).locator(".log-level-value.effective")).toHaveText("ERROR");

  await editor.locator(".profile-badge.packaged").filter({ hasText: "platform-debug" }).click();
  await expect(editor.locator(".log-level-header .profile-column")).toHaveCount(3);
  await expect(editor.locator(".log-level-row").filter({ hasText: "example.Logger" }).locator(".log-level-value.effective")).toHaveText("DEBUG");

  const headers = editor.locator(".log-level-header .profile-column");
  await headers.filter({ hasText: "platform-debug" }).dragTo(headers.filter({ hasText: "my-runtime" }), { targetPosition: { x: 2, y: 20 } });
  await expect(headers.nth(1)).toContainText("platform-debug");
  await expect(editor.locator(".log-level-row").filter({ hasText: "example.Logger" }).locator(".log-level-value.effective")).toHaveText("ERROR");

  await editor.getByRole("button", { name: "Use packaged profiles" }).click();
  await expect(editor.getByRole("button", { name: "Use packaged profiles" })).toBeDisabled();
  await expect(headers).toHaveCount(1);
  await expect(editor.locator(".log-level-row").filter({ hasText: "example.Logger" }).locator(".log-level-value.effective")).toHaveText("INFO");
});

test("asks before deleting a runtime profile", async ({ page }) => {
  await page.goto("/e2e.html");
  await page.locator(".levels-action").click();
  const editor = page.locator(".log-level-workspace");
  await editor.getByRole("button", { name: "Add logger level" }).click();
  const dialog = page.getByRole("dialog", { name: "Add logger level" });
  await dialog.getByLabel("New profile name").fill("temporary");
  await dialog.getByLabel("Logger", { exact: true }).fill("example.Logger");
  await dialog.getByRole("button", { name: "Add level" }).click();

  await editor.getByRole("button", { name: "Delete runtime profile temporary" }).click();
  const confirmation = page.getByRole("alertdialog", { name: "Delete runtime profile" });
  await expect(confirmation).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(confirmation).toHaveCount(0);
  await expect(editor.getByRole("button", { name: "Delete runtime profile temporary" })).toBeVisible();
  await editor.getByRole("button", { name: "Delete runtime profile temporary" }).click();
  await confirmation.getByRole("button", { name: "Delete profile" }).click();
  await expect(editor.getByRole("button", { name: "Delete runtime profile temporary" })).toHaveCount(0);
});

test("drops badges and headers at the first insertion slot, then removes a column", async ({ page }) => {
  await page.goto("/e2e.html");
  await page.locator(".levels-action").click();
  const editor = page.locator(".log-level-workspace");
  const headers = editor.locator(".log-level-header .profile-column");
  const loggerHeader = editor.locator(".log-level-header .logger-column");

  await editor.locator(".profile-badge.packaged").filter({ hasText: "platform-debug" })
    .dragTo(headers.first(), { targetPosition: { x: 2, y: 20 } });
  await expect(headers.first()).toContainText("platform-debug");
  await expect(headers).toHaveCount(2);

  await headers.filter({ hasText: "packaged-default" })
    .dragTo(loggerHeader, { targetPosition: { x: 5, y: 5 } });
  await expect(headers.first()).toContainText("packaged-default");

  await editor.getByRole("button", { name: "Remove profile platform-debug from table" }).click();
  await expect(headers).toHaveCount(1);
  await expect(editor.locator(".profile-badge.packaged").filter({ hasText: "platform-debug" })).toBeVisible();

  await editor.getByRole("button", { name: "Add logger level" }).click();
  const dialog = page.getByRole("dialog", { name: "Add logger level" });
  await dialog.getByLabel("New profile name").fill("first-runtime");
  await dialog.getByLabel("Logger", { exact: true }).fill("example.Logger");
  await dialog.getByRole("button", { name: "Add level" }).click();
  await editor.locator(".profile-badge.runtime button[draggable]").filter({ hasText: "first-runtime" })
    .dragTo(loggerHeader, { targetPosition: { x: 5, y: 5 } });
  await expect(headers.first()).toContainText("first-runtime");
});

test("shows resolved root level and keeps logger suffixes visible in a resizable column", async ({ page }) => {
  await page.goto("/e2e.html");
  await page.locator(".levels-action").click();
  const editor = page.locator(".log-level-workspace");
  await editor.getByRole("button", { name: "Add logger level" }).click();
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog", { name: "Add logger level" })).toHaveCount(0);

  await expect(editor.locator(".logger-name strong").filter({ hasText: "Logger" })).toBeVisible();
  const packageName = editor.locator(".logger-name[title='example.service']");
  await expect(packageName.locator("strong")).toHaveText("service");
  await expect(packageName).toHaveCSS("font-style", "normal");
  await editor.getByRole("separator", { name: "Resize logger column" }).press("ArrowLeft");
  await expect(editor.locator(".log-level-header")).toHaveAttribute("style", /396px/);
  await expect(editor.locator(".log-level-row").filter({ hasText: "example.Other" }).locator(".log-level-value.effective"))
    .toHaveText("INFO");
});

test("can remove the sole packaged profile and restore it", async ({ page }) => {
  await page.goto("/e2e.html");
  await page.locator(".levels-action").click();
  const editor = page.locator(".log-level-workspace");
  const headers = editor.locator(".log-level-header .profile-column");
  await expect(headers).toHaveCount(1);
  await editor.getByRole("button", { name: "Remove profile packaged-default from table" }).click();
  await expect(headers).toHaveCount(0);
  await expect(editor.getByRole("button", { name: "Use packaged profiles" })).toBeEnabled();
  await editor.getByRole("button", { name: "Use packaged profiles" }).click();
  await expect(headers).toHaveCount(1);
});
