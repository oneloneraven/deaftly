import assert from "node:assert/strict";
import { test } from "node:test";
import { gradleWithRetry, isDownloadFailure, retryWaitSeconds } from "./gradle.mjs";

const PLUGIN_FAILURE = `* What went wrong:
Plugin [id: 'org.jetbrains.kotlin.jvm', version: '2.4.21', apply: false] was not found in any of the following sources:
- Plugin Repositories (could not resolve plugin artifact 'org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.4.21')`;

const TEST_FAILURE = `SavedChatsScreenshotTest > listLightTheme FAILED
    java.lang.AssertionError at SavedChatsScreenshotTest.kt:81
* What went wrong:
Execution failed for task ':app:testDebugUnitTest'.`;

const RATE_LIMITED = `> Could not GET 'https://repo.maven.apache.org/maven2/x.pom'.
   > Received status code 429 from server: Too Many Requests`;

const noWait = async () => {};

function waits() {
  const seconds = [];
  return { wait: async s => seconds.push(s), seconds };
}

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
  assert.equal(await gradleWithRetry(["check"], runner, () => {}, noWait), 0);
  assert.deepEqual(calls, [["check"]]);
});

test("a download failure runs once more with --stacktrace, and its result counts", async () => {
  const { runner, calls } = runnerOf({ code: 1, output: PLUGIN_FAILURE }, { code: 0, output: "" });
  const logged = [];
  assert.equal(await gradleWithRetry(["check"], runner, line => logged.push(line), noWait), 0);
  assert.deepEqual(calls, [["check"], ["check", "--stacktrace"]]);
  assert.match(logged[0], /^::warning::/);
});

test("a second download failure fails the step", async () => {
  const { runner } = runnerOf({ code: 1, output: PLUGIN_FAILURE }, { code: 1, output: PLUGIN_FAILURE });
  assert.equal(await gradleWithRetry(["check"], runner, () => {}, noWait), 1);
});

test("a real build failure is not retried", async () => {
  const { runner, calls } = runnerOf({ code: 1, output: TEST_FAILURE });
  assert.equal(await gradleWithRetry(["check"], runner, () => {}, noWait), 1);
  assert.equal(calls.length, 1);
});

test("a rate limit waits longer than a dropped download before the retry", () => {
  assert.equal(retryWaitSeconds(RATE_LIMITED), 300);
  assert.equal(retryWaitSeconds(PLUGIN_FAILURE), 30);
});

test("the retry waits, and says how long", async () => {
  const { runner } = runnerOf({ code: 1, output: RATE_LIMITED }, { code: 0, output: "" });
  const { wait, seconds } = waits();
  const logged = [];
  assert.equal(await gradleWithRetry(["check"], runner, line => logged.push(line), wait), 0);
  assert.deepEqual(seconds, [300]);
  assert.match(logged[0], /Waiting 300 seconds/);
});

test("a passing run or a real build failure doesn't wait", async () => {
  for (const result of [{ code: 0, output: "" }, { code: 1, output: TEST_FAILURE }]) {
    const { runner } = runnerOf(result);
    const { wait, seconds } = waits();
    await gradleWithRetry(["check"], runner, () => {}, wait);
    assert.deepEqual(seconds, []);
  }
});
