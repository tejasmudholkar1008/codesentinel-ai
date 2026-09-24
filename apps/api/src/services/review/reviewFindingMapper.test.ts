import { describe, expect, it } from "vitest";
import { buildReviewComments } from "./reviewFindingMapper.js";
import type { ReviewFinding } from "./reviewServiceClient.js";
import type { ReviewInputFile } from "./reviewInputService.js";

describe("buildReviewComments", () => {
  const files: ReviewInputFile[] = [
    {
      filename: "apps/api/src/test/example.ts",
      status: "added",
      additions: 3,
      deletions: 0,
      changes: 3,
      patch: [
        "@@ -0,0 +1,3 @@",
        "+export function getUserPassword(user: any) {",
        "+  return user.password;",
        "+}",
      ].join("\n"),
    },
  ];

  it("maps a finding on a valid changed line", () => {
    const findings: ReviewFinding[] = [
      {
        severity: "HIGH",
        category: "SECURITY",
        filename: "apps/api/src/test/example.ts",
        line: 2,
        title: "Sensitive data exposure",
        description: "The function exposes a password.",
        suggestion: "Do not return passwords from the API.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(1);
    expect(result.skippedFindings).toHaveLength(0);

    expect(result.comments[0]).toEqual({
      path: "apps/api/src/test/example.ts",
      line: 2,
      side: "RIGHT",
      body: expect.stringContaining("Sensitive data exposure"),
    });
  });

  it("skips findings without a line", () => {
    const findings: ReviewFinding[] = [
      {
        severity: "MEDIUM",
        category: "QUALITY",
        filename: "apps/api/src/test/example.ts",
        line: null,
        title: "Code quality issue",
        description: "Review this implementation.",
        suggestion: "Consider simplifying the implementation.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(0);
    expect(result.skippedFindings).toHaveLength(1);
  });

  it("skips findings for files that are not part of the review", () => {
    const findings: ReviewFinding[] = [
      {
        severity: "HIGH",
        category: "SECURITY",
        filename: "src/unknown.ts",
        line: 2,
        title: "Security issue",
        description: "Potential security issue.",
        suggestion: "Review this code.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(0);
    expect(result.skippedFindings).toHaveLength(1);
  });

  it("skips findings for lines outside the changed diff", () => {
    const findings: ReviewFinding[] = [
      {
        severity: "LOW",
        category: "QUALITY",
        filename: "apps/api/src/test/example.ts",
        line: 20,
        title: "Quality issue",
        description: "Potential improvement.",
        suggestion: "Consider refactoring.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(0);
    expect(result.skippedFindings).toHaveLength(1);
  });

  it("should create comments for multiple findings on the same file", () => {
    const files = [
      {
        filename: "src/auth.ts",
        status: "modified",
        additions: 2,
        deletions: 0,
        changes: 2,
        patch: [
          "@@ -1,3 +1,5 @@",
          " const auth = true;",
          "+const token = getToken();",
          "+const user = getUser();",
          " return auth;",
        ].join("\n"),
      },
    ];

    const findings = [
      {
        severity: "HIGH",
        category: "Security",
        filename: "src/auth.ts",
        line: 2,
        title: "Potential token exposure",
        description: "Token may be exposed.",
        suggestion: "Avoid exposing the token.",
      },
      {
        severity: "MEDIUM",
        category: "Quality",
        filename: "src/auth.ts",
        line: 3,
        title: "Unused user value",
        description: "The value may not be required.",
        suggestion: "Remove it if unnecessary.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(2);
    expect(result.skippedFindings).toHaveLength(0);

    expect(result.comments[0]).toMatchObject({
      path: "src/auth.ts",
      line: 2,
      side: "RIGHT",
    });

    expect(result.comments[1]).toMatchObject({
      path: "src/auth.ts",
      line: 3,
      side: "RIGHT",
    });
  });

  it("should correctly handle multiple diff hunks", () => {
    const files = [
      {
        filename: "src/service.ts",
        status: "modified",
        additions: 2,
        deletions: 0,
        changes: 2,
        patch: [
          "@@ -1,2 +1,3 @@",
          " const first = true;",
          "+const firstChange = true;",
          "",
          "@@ -20,2 +21,3 @@",
          " const second = true;",
          "+const secondChange = true;",
          " return second;",
        ].join("\n"),
      },
    ];

    const findings = [
      {
        severity: "HIGH",
        category: "Bug",
        filename: "src/service.ts",
        line: 22,
        title: "Potential bug",
        description: "Potential issue in this line.",
        suggestion: "Review this logic.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(1);

    expect(result.comments[0]).toMatchObject({
      path: "src/service.ts",
      line: 22,
      side: "RIGHT",
    });
  });

  it("should skip findings that point to deleted lines", () => {
    const files = [
      {
        filename: "src/service.ts",
        status: "modified",
        additions: 1,
        deletions: 1,
        changes: 2,
        patch: [
          "@@ -10,3 +10,3 @@",
          "-const oldValue = true;",
          "+const newValue = true;",
          " return newValue;",
        ].join("\n"),
      },
    ];

    const findings = [
      {
        severity: "HIGH",
        category: "Bug",
        filename: "src/service.ts",
        line: 10,
        title: "Issue in deleted code",
        description: "This line was removed.",
        suggestion: "Review the replacement.",
      },
    ];

    const result = buildReviewComments(findings, files);

    expect(result.comments).toHaveLength(1);
  });
});
