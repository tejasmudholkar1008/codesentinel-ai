import { githubApp } from "./githubApp.js";

export interface GitHubInstallation {
  id: number;
  accountLogin: string | null;
  accountType: string | null;
}

export async function getInstallations(): Promise<GitHubInstallation[]> {
  const octokit = await githubApp.octokit;

  const { data } = await octokit.rest.apps.listInstallations({
    per_page: 100,
  });

  return data.map((installation) => ({
    id: installation.id,
    accountLogin:
      installation.account?.login ?? installation.account?.name ?? null,
    accountType: installation.account?.type ?? null,
  }));
}
