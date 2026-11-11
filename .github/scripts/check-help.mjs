import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";

const dir = process.argv[2] ?? "help";
const pages = readdirSync(dir).filter(file => file.endsWith(".md"));
const problems = [];
const orders = new Map();

for (const file of pages) {
  const text = readFileSync(join(dir, file), "utf8");
  const front = text.match(/^---\n([\s\S]*?)\n---\n/)?.[1];
  if (!front) {
    problems.push(`${dir}/${file} has no front matter`);
    continue;
  }
  const title = front.match(/^title: *(.+)$/m)?.[1]?.trim();
  const order = front.match(/^order: *(\d+)$/m)?.[1];
  if (!title) problems.push(`${dir}/${file} is missing title`);
  if (!order) problems.push(`${dir}/${file} is missing order`);
  else if (orders.has(order)) problems.push(`${dir}/${file} has the same order as ${orders.get(order)}`);
  else orders.set(order, file);
  for (const [, target] of text.matchAll(/\]\(([^)#:]+\.md)(?:#[^)]*)?\)/g)) {
    if (!existsSync(join(dir, target))) problems.push(`${dir}/${file} links to missing page ${target}`);
  }
}

if (problems.length) {
  for (const problem of problems) console.error(`::error::${problem}`);
  process.exit(1);
}
console.log(`${pages.length} help pages checked`);
