import { Worker } from "bullmq";
import { redisConnection } from "./redis.js";
import type { ReviewJob } from "./reviewJob.js";
import { requestCodeReview } from "../services/review/reviewServiceClient.js";
import { buildReviewComments } from "../services/review/reviewFindingMapper.js";
import { submitPullRequestReview } from "../services/github/pullRequestReviewService.js";

export const reviewWorker = new Worker<ReviewJob>(
  "code-review",
  async (job) => {
    const { installationId, owner, repo, reviewInput } = job.data;

    const pullRequestNumber = reviewInput.pullRequest.number;

    console.info("Processing code review", {
      jobId: job.id,
      pullRequestNumber,
      repository: `${owner}/${repo}`,
    });

    // 1. Request AI analysis from Java.
    const reviewResponse = await requestCodeReview(reviewInput);

    // 2. Map findings to valid GitHub diff locations.
    const { comments, skippedFindings } = buildReviewComments(
      reviewResponse.findings,
      reviewInput.files,
    );

    // 3. Post inline review comments to GitHub.
    if (comments.length > 0) {
      await submitPullRequestReview({
        installationId,
        owner,
        repo,
        pullRequestNumber,
        commitId: reviewInput.pullRequest.headSha,
        body: `CodeSentinel AI found ${reviewResponse.issuesFound} issue(s) in this pull request.`,
        comments,
      });
    }

    console.info("Code review completed", {
      jobId: job.id,
      pullRequestNumber,
      filesAnalyzed: reviewResponse.filesAnalyzed,
      issuesFound: reviewResponse.issuesFound,
      commentsPosted: comments.length,
      skippedFindings: skippedFindings.length,
    });

    return {
      status: reviewResponse.status,
      issuesFound: reviewResponse.issuesFound,
      commentsPosted: comments.length,
      skippedFindings: skippedFindings.length,
    };
  },
  {
    connection: redisConnection,
  },
);

reviewWorker.on("completed", (job) => {
  console.info("Review job completed", {
    jobId: job.id,
  });
});

reviewWorker.on("failed", (job, error) => {
  console.error("Review job failed:", {
    jobId: job?.id,
    name: error.name,
    message: error.message,
    stack: error.stack,
    cause: error.cause,
  });
});

reviewWorker.on("error", (error) => {
  console.error("BullMQ worker error:", {
    name: error.name,
    message: error.message,
    stack: error.stack,
    cause: error.cause,
    details: Object.getOwnPropertyNames(error).reduce<Record<string, unknown>>(
      (result, key) => {
        result[key] = Reflect.get(error, key);
        return result;
      },
      {},
    ),
  });
});

async function shutdown() {
  console.info("Shutting down review worker");

  await reviewWorker.close();
  await redisConnection.quit();
}

process.once("SIGINT", () => {
  void shutdown();
});

process.once("SIGTERM", () => {
  void shutdown();
});
