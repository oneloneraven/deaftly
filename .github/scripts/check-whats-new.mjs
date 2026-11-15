import { hasWhatsNewHeading, placeholdersIn, whatsNewFromPrBody } from "./whats-new.mjs";

const { body = "", labels = [] } = JSON.parse(process.env.PR_JSON ?? "{}");
if (!labels.some(({ name }) => /^release: (major|minor|patch)$/.test(name))) {
  console.log("No release, so no What's new needed.");
  process.exit(0);
}
if (!hasWhatsNewHeading(body ?? "")) {
  console.log(
    "::error::This PR makes a release, so its description needs a \"## What's new\" heading: one plain sentence per change someone using Deaftly will notice, shown in the release notes. Leave it empty only if they won't notice anything. See docs/definition-of-done.md.",
  );
  process.exit(1);
}
const placeholders = placeholdersIn(whatsNewFromPrBody(body ?? ""));
if (placeholders.length) {
  console.log(
    `::error::Leave "What's new" empty when someone using Deaftly won't notice anything; "${placeholders[0]}" would be shown to them as a change.`,
  );
  process.exit(1);
}
console.log("What's new heading present.");
