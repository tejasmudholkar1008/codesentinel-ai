import { getInstallationClient } from "./githubClient.js";

export interface PullRequestDetails {
  number: number;
  title: string;
  body: string | null;
  state: string;
  htmlUrl: string;
  headSha: string;
  baseBranch: string;
  headBranch: string;
}

export interface PullRequestFile {
  filename: string;
  status: string;
  additions: number;
  deletions: number;
  changes: number;
  patch: string | null;
}

export async function getPullRequest(
  installationId: number,
  owner: string,
  repo: string,
  pullRequestNumber: number,
): Promise<PullRequestDetails> {
  const octokit = await getInstallationClient(installationId);

  const { data } = await octokit.rest.pulls.get({
    owner,
    repo,
    pull_number: pullRequestNumber,
  });

  return {
    number: data.number,
    title: data.title,
    body: data.body,
    state: data.state,
    htmlUrl: data.html_url,
    headSha: data.head.sha,
    baseBranch: data.base.ref,
    headBranch: data.head.ref,
  };
}

export async function getPullRequestFiles(
  installationId: number,
  owner: string,
  repo: string,
  pullRequestNumber: number,
): Promise<PullRequestFile[]> {
  const octokit = await getInstallationClient(installationId);

  const files = await octokit.paginate(octokit.rest.pulls.listFiles, {
    owner,
    repo,
    pull_number: pullRequestNumber,
    per_page: 100,
  });

  return files.map((file) => ({
    filename: file.filename,
    status: file.status,
    additions: file.additions,
    deletions: file.deletions,
    changes: file.changes,
    patch: file.patch ?? null,
  }));
}
