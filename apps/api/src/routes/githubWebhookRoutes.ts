import type { FastifyInstance } from "fastify";
import { githubWebhookController } from "../controllers/githubWebhookController.js";

export async function githubWebhookRoutes(app: FastifyInstance) {
  app.post(
    "/webhooks/github",
    {
      config: {
        rawBody: true,
      },
    },
    githubWebhookController,
  );
}
