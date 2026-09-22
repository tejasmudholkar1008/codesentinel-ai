import type { ReviewFinding } from "./reviewServiceClient.js";
import type { ReviewInputFile } from "./reviewInputService.js";
import type { ReviewComment } from "../github/pullRequestReviewService.js";

export interface ReviewMappingResult {
  comments: ReviewComment[];
  skippedFindings: ReviewFinding[];
}

function getDiffLines(patch: string): Set<number> {
  const changedLines = new Set<number>();

  const lines = patch.split("\n");
  let newLineNumber = 0;

  for (const line of lines) {
    const hunkMatch = line.match(/^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/);

    if (hunkMatch) {
      newLineNumber = Number(hunkMatch[1]);
      continue;
    }

    if (!newLineNumber) {
      continue;
    }

    if (line.startsWith("+") && !line.startsWith("+++")) {
      changedLines.add(newLineNumber);
      newLineNumber++;
      continue;
    }

    if (line.startsWith("-") && !line.startsWith("---")) {
      continue;
    }

    if (line.startsWith(" ")) {
      changedLines.add(newLineNumber);
      newLineNumber++;
    }
  }

  return changedLines;
}

function buildCommentBody(finding: ReviewFinding): string {
  return [
    `**${finding.severity} — ${finding.category}**`,
    "",
    `### ${finding.title}`,
    "",
    finding.description,
    "",
    `**Suggestion:** ${finding.suggestion}`,
  ].join("\n");
}

export function buildReviewComments(
  findings: ReviewFinding[],
  files: ReviewInputFile[],
): ReviewMappingResult {
  const comments: ReviewComment[] = [];
  const skippedFindings: ReviewFinding[] = [];

  for (const finding of findings) {
    if (finding.line == null || finding.line <= 0) {
      skippedFindings.push(finding);
      continue;
    }

    const file = files.find(
      (reviewFile) => reviewFile.filename === finding.filename,
    );

    if (!file || !file.patch) {
      skippedFindings.push(finding);
      continue;
    }

    const diffLines = getDiffLines(file.patch);

    if (!diffLines.has(finding.line)) {
      skippedFindings.push(finding);
      continue;
    }

    comments.push({
      path: finding.filename,
      line: finding.line,
      side: "RIGHT",
      body: buildCommentBody(finding),
    });
  }

  return {
    comments,
    skippedFindings,
  };
}
