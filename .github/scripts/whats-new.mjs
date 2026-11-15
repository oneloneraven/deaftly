const HEADING = /^##\s+What['’]s new\s*$/im;
const ATTRIBUTION = /^(🤖\s*)?Generated with /;
const SECTION_END = line =>
  /^#{1,2}\s/.test(line) || /^(-{3,}|\*{3,}|_{3,})$/.test(line.trim()) || ATTRIBUTION.test(line.trim());
const PLACEHOLDER = /^(none|nothing( user-facing)?|n\/a|-)\.?$/i;

export function hasWhatsNewHeading(body = "") {
  return HEADING.test(body.replace(/\r\n/g, "\n"));
}

export function whatsNewFromPrBody(body = "") {
  const text = (body ?? "").replace(/\r\n/g, "\n");
  const start = text.search(HEADING);
  if (start === -1) return [];
  const section = text.slice(start).split("\n").slice(1);
  const end = section.findIndex(SECTION_END);
  const changes = [];
  let inComment = false;
  for (const line of end === -1 ? section : section.slice(0, end)) {
    const trimmed = line.trim();
    if (inComment || trimmed.startsWith("<!--")) {
      inComment = !trimmed.endsWith("-->");
      continue;
    }
    const change = trimmed.replace(/^[-*]\s+/, "");
    if (change) changes.push(change);
  }
  return changes;
}

export const placeholdersIn = changes => changes.filter(change => PLACEHOLDER.test(change));

export function releaseNotes(changes) {
  return changes.map(change => `- ${change}`).join("\n");
}
