import { getInstallationClient } from "./githubClient.js";

export interface ReviewComment {
  path: string;
  line: number;
  side: "RIGHT";
  body: string;
}

export interface SubmitPullRequestReviewInput {
  installationId: number;
  owner: string;
  repo: string;
  pullRequestNumber: number;
  commitId: string;
  body: string;
  comments: ReviewComment[];
}

export async function submitPullRequestReview(
  input: SubmitPullRequestReviewInput,
) {
  const octokit = await getInstallationClient(input.installationId);

  return octokit.rest.pulls.createReview({
    owner: input.owner,
    repo: input.repo,
    pull_number: input.pullRequestNumber,
    commit_id: input.commitId,
    body: input.body,
    event: "COMMENT",
    comments: input.comments,
  });
}
