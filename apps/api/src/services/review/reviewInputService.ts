import type {
  PullRequestDetails,
  PullRequestFile,
} from "../github/pullRequestService.js";

export interface ReviewInputFile {
  filename: string;
  status: string;
  additions: number;
  deletions: number;
  changes: number;
  patch: string;
}

export interface ReviewInput {
  pullRequest: {
    number: number;
    title: string;
    description: string | null;
    baseBranch: string;
    headBranch: string;
    headSha: string;
    url: string;
  };

  files: ReviewInputFile[];
}

export function buildReviewInput(
  pullRequest: PullRequestDetails,
  files: PullRequestFile[],
): ReviewInput {
  return {
    pullRequest: {
      number: pullRequest.number,
      title: pullRequest.title,
      description: pullRequest.body,
      baseBranch: pullRequest.baseBranch,
      headBranch: pullRequest.headBranch,
      headSha: pullRequest.headSha,
      url: pullRequest.htmlUrl,
    },

    files: files
      .filter((file) => file.patch)
      .map((file) => ({
        filename: file.filename,
        status: file.status,
        additions: file.additions,
        deletions: file.deletions,
        changes: file.changes,
        patch: file.patch as string,
      })),
  };
}
