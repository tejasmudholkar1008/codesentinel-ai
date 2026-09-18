import Fastify from "fastify";
import rawBody from "fastify-raw-body";

import { env } from "./config/env.js";
import { githubWebhookRoutes } from "./routes/githubWebhookRoutes.js";

const app = Fastify({
  logger: true,
});

await app.register(rawBody, {
  field: "rawBody",
  global: false,
  encoding: "utf8",
  runFirst: true,
});

app.get("/health", async () => {
  return {
    success: true,
    service: "codesentinel-api",
    status: "healthy",
  };
});

await app.register(githubWebhookRoutes);

try {
  await app.listen({
    port: env.port,
    host: "0.0.0.0",
  });

  console.log(`CodeSentinel API running on http://localhost:${env.port}`);
} catch (error) {
  app.log.error(error);
  process.exit(1);
}
