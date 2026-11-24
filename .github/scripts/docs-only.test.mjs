import assert from "node:assert/strict";
import { test } from "node:test";
import { isDocsOnly } from "./docs-only.mjs";

test("docs, Claude config and root Markdown are docs only", () => {
  assert.equal(isDocsOnly(["docs/brand/style-guide.md", ".claude/skills/x/SKILL.md", "README.md", "CLAUDE.md"]), true);
});

test("help pages ship in the app, so they aren't docs only", () => {
  assert.equal(isDocsOnly(["help/camera.md"]), false);
});

test("tests, lint config and workflows still need the check", () => {
  for (const path of ["app/src/test/kotlin/A.kt", "config/detekt/detekt.yml", ".github/workflows/test.yml", "third_party/ncnn/README.md"]) {
    assert.equal(isDocsOnly(["docs/workflow.md", path]), false, path);
  }
});

test("no changed files runs the check", () => {
  assert.equal(isDocsOnly([]), false);
});
