import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { matches } from "./check-release-paths.mjs";

export const DOCS_ONLY = ["docs/", ".claude/", "*.md"];

export function isDocsOnly(changed) {
  return changed.length > 0 && changed.every(path => DOCS_ONLY.some(entry => matches(path, entry)));
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const changed = readFileSync(0, "utf8").split("\n").filter(Boolean);
  const docsOnly = isDocsOnly(changed);
  console.log(docsOnly ? "Only docs changed, so the Gradle check is skipped." : "Code or build files changed, so the Gradle check runs.");
  console.log(`docs-only=${docsOnly}`);
}
