import { describe, expect, it } from "vitest";
import { buildReviewInput, type ReviewInput } from "./reviewInputService.js";
import type {
  PullRequestDetails,
  PullRequestFile,
} from "../github/pullRequestService.js";

describe("buildReviewInput", () => {
  const pullRequest: PullRequestDetails = {
    number: 25,
    title: "Add authentication",
    body: "Adds authentication support",
    state: "open",
    htmlUrl: "https://github.com/tejasmudholkar1008/codesentinel-ai/pull/25",
    headSha: "abcdef1234567890",
    baseBranch: "main",
    headBranch: "feature/authentication",
  };

  it("should build review input with pull request and repository details", () => {
    const files: PullRequestFile[] = [
      {
        filename: "src/auth.ts",
        status: "modified",
        additions: 10,
        deletions: 2,
        changes: 12,
        patch: "@@ -1,3 +1,11 @@\n+const token = 'test';",
      },
    ];

    const result: ReviewInput = buildReviewInput(
      pullRequest,
      files,
      "tejasmudholkar1008/codesentinel-ai",
    );

    expect(result.pullRequest).toEqual({
      repository: "tejasmudholkar1008/codesentinel-ai",
      number: 25,
      title: "Add authentication",
      description: "Adds authentication support",
      baseBranch: "main",
      headBranch: "feature/authentication",
      headSha: "abcdef1234567890",
      url: "https://github.com/tejasmudholkar1008/codesentinel-ai/pull/25",
    });
  });

  it("should include file metadata and patch", () => {
    const files: PullRequestFile[] = [
      {
        filename: "src/auth.ts",
        status: "modified",
        additions: 10,
        deletions: 2,
        changes: 12,
        patch: "@@ -1,3 +1,11 @@\n+const token = 'test';",
      },
    ];

    const result = buildReviewInput(
      pullRequest,
      files,
      "tejasmudholkar1008/codesentinel-ai",
    );

    expect(result.files).toEqual([
      {
        filename: "src/auth.ts",
        status: "modified",
        additions: 10,
        deletions: 2,
        changes: 12,
        patch: "@@ -1,3 +1,11 @@\n+const token = 'test';",
      },
    ]);
  });

  it("should exclude files without a patch", () => {
    const files: PullRequestFile[] = [
      {
        filename: "src/auth.ts",
        status: "modified",
        additions: 10,
        deletions: 2,
        changes: 12,
        patch: "@@ -1,3 +1,11 @@\n+const token = 'test';",
      },
      {
        filename: "image.png",
        status: "added",
        additions: 0,
        deletions: 0,
        changes: 0,
        patch: null,
      },
    ];

    const result = buildReviewInput(
      pullRequest,
      files,
      "tejasmudholkar1008/codesentinel-ai",
    );

    expect(result.files).toHaveLength(1);
    expect(result.files[0].filename).toBe("src/auth.ts");
  });

  it("should preserve a null pull request description", () => {
    const files: PullRequestFile[] = [];

    const result = buildReviewInput(
      {
        ...pullRequest,
        body: null,
      },
      files,
      "tejasmudholkar1008/codesentinel-ai",
    );

    expect(result.pullRequest.description).toBeNull();
  });
});
