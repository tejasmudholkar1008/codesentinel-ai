import { Queue } from "bullmq";
import { redisConnection } from "./redis.js";
import { ReviewJob } from "./reviewJob.js";

export const reviewQueue = new Queue<ReviewJob>("code-review", {
  connection: redisConnection,
  defaultJobOptions: {
    attempts: 3,
    backoff: {
      type: "exponential",
      delay: 5000,
    },
    removeOnComplete: 100,
    removeOnFail: 500,
  },
});
