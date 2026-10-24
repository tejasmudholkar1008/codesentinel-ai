import type { FastifyRequest, FastifyReply } from "fastify";

import { env } from "../config/env.js";
import { verifyGithubSignature } from "../services/webhook/verifySignature.js";
import {
  getPullRequest,
  getPullRequestFiles,
} from "../services/github/pullRequestService.js";
import { buildReviewInput } from "../services/review/reviewInputService.js";
import { reviewQueue } from "../queues/reviewQueue.js";

interface PullRequestPayload {
  action?: string;

  installation?: {
    id?: number;
  };

  repository?: {
    name?: string;
    full_name?: string;
    owner?: {
      login?: string;
    };
  };

  pull_request?: {
    number?: number;
    title?: string;
    head?: {
      sha?: string;
    };
  };
}

export async function githubWebhookController(
  request: FastifyRequest,
  reply: FastifyReply,
) {
  const signature = request.headers["x-hub-signature-256"];
  const rawBody = request.rawBody;

  // Verify GitHub's signature against the original request body.
  if (!rawBody) {
    return reply.code(400).send({
      success: false,
      message: "Missing raw request body",
    });
  }

  const isValid = verifyGithubSignature(
    rawBody,
    typeof signature === "string" ? signature : undefined,
    env.githubWebhookSecret,
  );

  if (!isValid) {
    return reply.code(401).send({
      success: false,
      message: "Invalid GitHub webhook signature",
    });
  }

  const event = request.headers["x-github-event"];

  // Process only pull_request events.
  if (event !== "pull_request") {
    return reply.code(200).send({
      success: true,
      message: `Ignored event: ${event ?? "unknown"}`,
    });
  }

  const payload = request.body as PullRequestPayload;
  const action = payload?.action;

  const supportedActions = ["opened", "synchronize", "reopened"];

  if (!action || !supportedActions.includes(action)) {
    return reply.code(200).send({
      success: true,
      message: `Ignored pull request action: ${action ?? "unknown"}`,
    });
  }

  const repository = payload?.repository;
  const pullRequest = payload?.pull_request;

  const owner = repository?.owner?.login;
  const repo = repository?.name;
  const pullRequestNumber = pullRequest?.number;
  const installationId = payload?.installation?.id;

  // Validate all required webhook fields before accessing them.
  if (
    installationId == null ||
    !owner ||
    !repo ||
    pullRequestNumber == null ||
    !Number.isInteger(pullRequestNumber) ||
    pullRequestNumber < 1 ||
    !pullRequest?.title ||
    !pullRequest.head?.sha
  ) {
    request.log.error(
      {
        installationId,
        owner,
        repo,
        pullRequestNumber,
      },
      "Incomplete pull request webhook payload",
    );

    return reply.code(400).send({
      success: false,
      message: "Incomplete pull request webhook payload",
    });
  }

  request.log.info(
    {
      event,
      action,
      owner,
      repo,
      pullRequestNumber,
      installationId,
      title: pullRequest.title,
      commitSha: pullRequest.head.sha,
    },
    "Pull request webhook received",
  );

  try {
    // Retrieve pull request metadata from GitHub.
    const pullRequestData = await getPullRequest(
      installationId,
      owner,
      repo,
      pullRequestNumber,
    );

    // Retrieve changed files and their patches.
    const files = await getPullRequestFiles(
      installationId,
      owner,
      repo,
      pullRequestNumber,
    );

    request.log.info(
      {
        pullRequestNumber,
        filesCount: files.length,
      },
      "Pull request data retrieved",
    );

    // Build the payload consumed by the Java review service.
    const reviewInput = buildReviewInput(
      pullRequestData,
      files,
      `${owner}/${repo}`,
    );

    const headSha = reviewInput.pullRequest.headSha;

    if (!headSha) {
      request.log.error(
        { owner, repo, pullRequestNumber },
        "Missing pull request head SHA",
      );

      return reply.code(400).send({
        success: false,
        message: "Missing pull request head SHA",
      });
    }

    request.log.info(
      {
        pullRequestNumber,
        filesCount: reviewInput.files.length,
        headSha,
      },
      "Review input built",
    );

    // Use the repository, PR number, and commit SHA to identify this review.
    // This suppresses duplicate jobs while the job ID remains in Redis.
    const jobId = `${owner}-${repo}-pr-${pullRequestNumber}-${headSha}`;

    const job = await reviewQueue.add(
      "review-pull-request",
      {
        installationId,
        owner,
        repo,
        reviewInput,
      },
      {
        jobId,
      },
    );

    request.log.info(
      {
        jobId: job.id,
        owner,
        repo,
        pullRequestNumber,
        installationId,
        headSha,
        filesCount: reviewInput.files.length,
      },
      "Pull request review enqueued",
    );

    // Return immediately after enqueueing; the worker processes the review.
    return reply.code(202).send({
      success: true,
      message: "Pull request review queued for processing",
      data: {
        owner,
        repo,
        pullRequestNumber,
        installationId,
        action,
        commitSha: headSha,
        filesCount: reviewInput.files.length,
        jobId: job.id,
        status: "QUEUED",
      },
    });
  } catch (error) {
    request.log.error(
      {
        error,
        owner,
        repo,
        pullRequestNumber,
        installationId,
      },
      "Failed to enqueue pull request review",
    );

    return reply.code(500).send({
      success: false,
      message: "Failed to enqueue pull request review",
    });
  }
}
