import type { FastifyInstance } from "fastify";
import { getInstallations } from "../services/github/installationService.js";

export async function githubInstallationRoutes(app: FastifyInstance) {
  app.get("/github/installations", async (_request, reply) => {
    try {
      const installations = await getInstallations();

      return reply.code(200).send({
        success: true,
        data: installations,
      });
    } catch (error) {
      app.log.error(error, "Failed to retrieve GitHub App installations");

      return reply.code(500).send({
        success: false,
        message: "Failed to retrieve GitHub App installations",
      });
    }
  });
}
