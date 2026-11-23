import assert from "node:assert/strict";
import { test } from "node:test";
import { gradleWithRetry, isDownloadFailure } from "./gradle.mjs";

const PLUGIN_FAILURE = `* What went wrong:
Plugin [id: 'org.jetbrains.kotlin.jvm', version: '2.4.21', apply: false] was not found in any of the following sources:
- Plugin Repositories (could not resolve plugin artifact 'org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.4.21')`;

const TEST_FAILURE = `SavedChatsScreenshotTest > listLightTheme FAILED
    java.lang.AssertionError at SavedChatsScreenshotTest.kt:81
* What went wrong:
Execution failed for task ':app:testDebugUnitTest'.`;

function runnerOf(...results) {
  const calls = [];
  const runner = async args => {
    calls.push(args);
    return results[calls.length - 1];
  };
  return { runner, calls };
}

test("a plugin that can't be downloaded is a download failure", () => {
  assert.equal(isDownloadFailure(PLUGIN_FAILURE), true);
  assert.equal(isDownloadFailure("Could not GET 'https://repo.maven.apache.org/x.pom'. Received status code 503 from server"), true);
  assert.equal(isDownloadFailure("> Read timed out"), true);
});

test("failing tests and compile errors are not download failures", () => {
  assert.equal(isDownloadFailure(TEST_FAILURE), false);
  assert.equal(isDownloadFailure("e: file:///app/Foo.kt:3:5 Unresolved reference 'bar'."), false);
  assert.equal(isDownloadFailure("Received status code 404 from server"), false);
});

test("a passing run runs once", async () => {
  const { runner, calls } = runnerOf({ code: 0, output: "" });
  assert.equal(await gradleWithRetry(["check"], runner, () => {}), 0);
  assert.deepEqual(calls, [["check"]]);
});

test("a download failure runs once more with --stacktrace, and its result counts", async () => {
  const { runner, calls } = runnerOf({ code: 1, output: PLUGIN_FAILURE }, { code: 0, output: "" });
  const logged = [];
  assert.equal(await gradleWithRetry(["check"], runner, line => logged.push(line)), 0);
  assert.deepEqual(calls, [["check"], ["check", "--stacktrace"]]);
  assert.match(logged[0], /^::warning::/);
});

test("a second download failure fails the step", async () => {
  const { runner } = runnerOf({ code: 1, output: PLUGIN_FAILURE }, { code: 1, output: PLUGIN_FAILURE });
  assert.equal(await gradleWithRetry(["check"], runner, () => {}), 1);
});

test("a real build failure is not retried", async () => {
  const { runner, calls } = runnerOf({ code: 1, output: TEST_FAILURE });
  assert.equal(await gradleWithRetry(["check"], runner, () => {}), 1);
  assert.equal(calls.length, 1);
});
