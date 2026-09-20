import { readFileSync } from "node:fs";
import { App } from "octokit";

import { env } from "../../config/env.js";

const privateKeyPath = env.githubPrivateKeyPath;

const privateKey = readFileSync(privateKeyPath, "utf8");

export const githubApp = new App({
  appId: env.githubAppId,
  privateKey,
});
