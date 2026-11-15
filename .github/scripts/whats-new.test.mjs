import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { planFromRepo, planReleases } from "./plan-releases.mjs";
import { hasWhatsNewHeading, placeholdersIn, releaseNotes, whatsNewFromPrBody } from "./whats-new.mjs";

const PR_BODY = `Closes #242

## What's new

<!-- One plain sentence per change someone using Deaftly would notice. -->
- Templates can join gaps with and or or.
Fatima's saved phrases keep their order.

## How it was verified

Ran it.
`;

test("takes the lines under the heading, without bullets or the template comment", () => {
  assert.deepEqual(whatsNewFromPrBody(PR_BODY), [
    "Templates can join gaps with and or or.",
    "Fatima's saved phrases keep their order.",
  ]);
});

test("is empty when the section is empty, missing or the body absent", () => {
  assert.deepEqual(whatsNewFromPrBody("## What's new\n\n<!-- c -->\n\n## Next\n"), []);
  assert.deepEqual(whatsNewFromPrBody("Closes #1"), []);
  assert.deepEqual(whatsNewFromPrBody(undefined), []);
  assert.deepEqual(whatsNewFromPrBody(null), []);
});

test("stops at the attribution footer, a rule or the next heading", () => {
  assert.deepEqual(whatsNewFromPrBody("## What's new\n\nA.\n\n🤖 Generated with [Claude Code](https://x)\n"), ["A."]);
  assert.deepEqual(whatsNewFromPrBody("## What's new\n\nA.\n---\nB.\n"), ["A."]);
  assert.deepEqual(whatsNewFromPrBody("## What's new\n\nA.\n## Next\nB.\n"), ["A."]);
});

test("skips multi-line comments, accepts a curly apostrophe and Windows line ends", () => {
  assert.deepEqual(whatsNewFromPrBody("## What’s new\r\n<!-- one\r\ntwo -->\r\nA.\r\n"), ["A."]);
});

test("detects the heading", () => {
  assert.equal(hasWhatsNewHeading("Closes #1\n\n## What's new\n"), true);
  assert.equal(hasWhatsNewHeading("Closes #1\r\n\r\n## What’s new\r\n"), true);
  assert.equal(hasWhatsNewHeading("Closes #1"), false);
  assert.equal(hasWhatsNewHeading(""), false);
});

test("flags placeholders", () => {
  assert.deepEqual(placeholdersIn(["None", "Nothing user-facing.", "n/a", "-", "A real change."]), [
    "None",
    "Nothing user-facing.",
    "n/a",
    "-",
  ]);
});

test("formats release notes as bullets, empty when there are none", () => {
  assert.equal(releaseNotes(["A.", "B."]), "- A.\n- B.");
  assert.equal(releaseNotes([]), "- Behind-the-scenes improvements and security updates.");
});

const check = (labels, body) =>
  spawnSync(process.execPath, [fileURLToPath(new URL("./check-whats-new.mjs", import.meta.url))], {
    env: { ...process.env, PR_JSON: JSON.stringify({ body, labels: labels.map(name => ({ name })) }) },
    encoding: "utf8",
  });

for (const label of ["release: patch", "release: minor", "release: major"]) {
  test(`check fails ${label} with no heading`, () => {
    const result = check(["bug", label], "Closes #1");
    assert.equal(result.status, 1);
    assert.match(result.stdout, /## What's new/);
  });
}

test("check passes a released PR with the heading, even left empty", () => {
  assert.equal(check(["release: patch"], "Closes #1\n\n## What's new\n").status, 0);
});

for (const line of ["Nothing user-facing.", "None", "- n/a", "Nothing"]) {
  test(`check fails the placeholder ${JSON.stringify(line)}`, () => {
    const result = check(["release: patch"], `Closes #1\n\n## What's new\n\n${line}\n\n## Next\n`);
    assert.equal(result.status, 1);
    assert.match(result.stdout, /empty/);
  });
}

test("check skips release: none and unlabelled PRs", () => {
  assert.equal(check(["release: none"], "Closes #1").status, 0);
  assert.equal(check([], "Closes #1").status, 0);
});

test("each release carries only its own PR's lines; none and empty PRs add nothing", () => {
  const prs = {
    a: { number: 1, labels: ["release: minor"], body: "## What's new\n\n- Fatima can pin a phrase.\n- Pinned phrases sit first.\n" },
    b: { number: 2, labels: ["release: none"], body: "## What's new\n\n- Should never appear.\n" },
    c: { number: 3, labels: ["release: patch"], body: "## What's new\n\n<!-- nothing -->\n" },
  };
  const plan = planReleases({
    commits: ["a", "b", "c"],
    latestVersion: "v1.0.0",
    isTagged: () => false,
    prFor: sha => prs[sha],
  });
  assert.deepEqual(
    plan.map(({ version, notes }) => [version, notes]),
    [
      ["v1.1.0", "- Fatima can pin a phrase.\n- Pinned phrases sit first."],
      ["v1.1.1", "- Behind-the-scenes improvements and security updates."],
    ],
  );
});

test("a manual release has an empty body", () => {
  const git = args => (args[0] === "tag" && args[1] === "-l" ? "v1.0.0" : args[0] === "rev-parse" ? "abc" : "");
  assert.deepEqual(planFromRepo({ git, prFor: () => null, bumpInput: "patch" }), [
    { sha: "abc", version: "v1.0.1", pr: null, notes: "- Behind-the-scenes improvements and security updates." },
  ]);
});
