import { Redis } from "ioredis";
import { env } from "../config/env.js";

const redisUrl = new URL(env.redisUrl);

export const redisConnection = new Redis({
  host: redisUrl.hostname,
  port: Number(redisUrl.port),
  maxRetriesPerRequest: null,
});
