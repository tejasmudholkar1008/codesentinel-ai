import type { FastifyRequest, FastifyReply } from "fastify";

import { env } from "../config/env.js";
import { verifyGithubSignature } from "../services/webhook/verifySignature.js";
import {
  getPullRequest,
  getPullRequestFiles,
} from "../services/github/pullRequestService.js";
import { buildReviewInput } from "../services/review/reviewInputService.js";
import { requestCodeReview } from "../services/review/reviewServiceClient.js";
import { buildReviewComments } from "../services/review/reviewFindingMapper.js";
import { submitPullRequestReview } from "../services/github/pullRequestReviewService.js";

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

  // Use the original raw request body for GitHub signature verification.
  const rawBody = request.rawBody;

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

  // We only process pull_request events.
  if (event !== "pull_request") {
    return reply.code(200).send({
      success: true,
      message: `Ignored event: ${event ?? "unknown"}`,
    });
  }

  const payload = request.body as PullRequestPayload;
  const action = payload.action;

  const supportedActions = ["opened", "synchronize", "reopened"];

  // Ignore unsupported PR actions.
  if (!action || !supportedActions.includes(action)) {
    return reply.code(200).send({
      success: true,
      message: `Ignored pull request action: ${action ?? "unknown"}`,
    });
  }

  const repository = payload.repository;
  const pullRequest = payload.pull_request;

  const owner = repository?.owner?.login;
  const repo = repository?.name;
  const pullRequestNumber = pullRequest?.number;
  const installationId = payload.installation?.id;

  // Validate the information required to access the PR.
  if (!installationId || !owner || !repo || !pullRequestNumber) {
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
      title: pullRequest?.title,
      commitSha: pullRequest?.head?.sha,
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

    const reviewInput = buildReviewInput(pullRequestData, files);

    request.log.info(
      {
        pullRequestNumber,
        filesCount: reviewInput.files.length,
      },
      "Review input built",
    );

    const reviewResponse = await requestCodeReview(reviewInput);

    request.log.info(
      {
        pullRequestNumber,
        filesAnalyzed: reviewResponse.filesAnalyzed,
        issuesFound: reviewResponse.issuesFound,
      },
      "Code review completed",
    );

    const { comments, skippedFindings } = buildReviewComments(
      reviewResponse.findings,
      reviewInput.files,
    );

    request.log.info(
      {
        pullRequestNumber,
        commentsCount: comments.length,
        skippedFindingsCount: skippedFindings.length,
      },
      "Review findings mapped to GitHub comments",
    );

    if (comments.length > 0) {
      await submitPullRequestReview({
        installationId,
        owner,
        repo,
        pullRequestNumber,
        commitId: pullRequestData.headSha,
        body: `CodeSentinel AI found ${reviewResponse.issuesFound} issue(s) in this pull request.`,
        comments,
      });

      request.log.info(
        {
          pullRequestNumber,
          commentsCount: comments.length,
        },
        "GitHub pull request review submitted",
      );
    }

    return reply.code(200).send({
      success: true,
      message: "Pull request review accepted",
      data: {
        owner,
        repo,
        pullRequestNumber,
        installationId,
        action,
        commitSha: pullRequest?.head?.sha,
        filesCount: files.length,
        review: reviewResponse,
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
      "Failed to process pull request review",
    );

    return reply.code(500).send({
      success: false,
      message: "Failed to process pull request review",
    });
  }
}
