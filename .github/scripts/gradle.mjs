import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";

const TAIL_LIMIT = 256 * 1024;
const RATE_LIMITED_WAIT_SECONDS = 300;
const DOWNLOAD_FAILURE_WAIT_SECONDS = 30;

const DOWNLOAD_FAILURES = [
  /could not resolve plugin artifact/i,
  /Could not GET '/,
  /Could not HEAD '/,
  /Could not download /,
  /Read timed out/,
  /Connect timed out/,
  /Connection reset/,
  /Received status code (?:408|429|5\d\d) from server/,
];

/** Whether a failed Gradle run's output shows a dependency or plugin download failure, rather than a real build failure. */
export function isDownloadFailure(output) {
  return DOWNLOAD_FAILURES.some(pattern => pattern.test(output));
}

/** How long to wait before retrying: a rate limit needs longer to clear than a dropped connection. */
export function retryWaitSeconds(output) {
  return /Received status code 429 from server/.test(output) ? RATE_LIMITED_WAIT_SECONDS : DOWNLOAD_FAILURE_WAIT_SECONDS;
}

const sleep = seconds => new Promise(resolve => setTimeout(resolve, seconds * 1000));

/** Runs Gradle with the given arguments, echoing its output, and resolves to its exit code and the tail of its output. */
function run(args) {
  return new Promise(resolve => {
    const child = spawn("./gradlew", args, { stdio: ["inherit", "pipe", "pipe"] });
    let tail = "";
    const keep = chunk => {
      tail = (tail + chunk).slice(-TAIL_LIMIT);
    };
    child.stdout.on("data", chunk => {
      process.stdout.write(chunk);
      keep(chunk);
    });
    child.stderr.on("data", chunk => {
      process.stderr.write(chunk);
      keep(chunk);
    });
    child.on("close", code => resolve({ code: code ?? 1, output: tail }));
  });
}

/** Runs Gradle once more, after a wait and with --stacktrace, if the first run failed only because a download failed. */
export async function gradleWithRetry(args, runner = run, log = console.log, wait = sleep) {
  const first = await runner(args);
  if (first.code === 0 || !isDownloadFailure(first.output)) return first.code;
  const seconds = retryWaitSeconds(first.output);
  log(
    `::warning::Gradle couldn't download a dependency or plugin. Waiting ${seconds} seconds, then trying once more, with --stacktrace to show why.`,
  );
  await wait(seconds);
  return (await runner([...args, "--stacktrace"])).code;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  process.exitCode = await gradleWithRetry(process.argv.slice(2));
}
