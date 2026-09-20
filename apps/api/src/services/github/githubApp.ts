import { readFileSync } from "node:fs";
import { App } from "octokit";

import { env } from "../../config/env.js";

const privateKeyPath =
  "/Users/tejasmudholkar/Developer/ai-lab/github-app/codesentinel-ai-reviewer.2026-09-18.private-key.pem";

const privateKey = readFileSync(privateKeyPath, "utf8");

export const githubApp = new App({
  appId: env.githubAppId,
  privateKey,
});
