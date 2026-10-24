import { redisConnection } from "./redis.js";

try {
  const result = await redisConnection.ping();

  console.log("Redis connection:", result);
} catch (error) {
  console.error("Redis connection failed:", error);
  process.exitCode = 1;
} finally {
  await redisConnection.quit();
}
