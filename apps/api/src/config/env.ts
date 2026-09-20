import "dotenv/config";

const port = Number(process.env.PORT ?? 3000);
const githubAppId = Number(process.env.GITHUB_APP_ID ?? 0);

if (Number.isNaN(port)) {
  throw new Error("PORT must be a valid number");
}

if (!githubAppId) {
  throw new Error("GITHUB_APP_ID must be a valid number");
}

export const env = {
  nodeEnv: process.env.NODE_ENV ?? "development",
  port,
  githubWebhookSecret: process.env.GITHUB_WEBHOOK_SECRET ?? "",
  githubAppId,
};
