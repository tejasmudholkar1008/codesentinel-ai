import { githubApp } from "./githubApp.js";

export async function getInstallationClient(installationId: number) {
  return githubApp.getInstallationOctokit(installationId);
}
