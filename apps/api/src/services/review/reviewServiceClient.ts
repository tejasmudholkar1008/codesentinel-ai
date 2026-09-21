import { env } from "../../config/env.js";
import type { ReviewInput } from "./reviewInputService.js";

export interface ReviewFinding {
  severity: string;
  category: string;
  filename: string;
  line: number | null;
  title: string;
  description: string;
  suggestion: string;
}

export interface ReviewResponse {
  status: string;
  filesAnalyzed: number;
  issuesFound: number;
  findings: ReviewFinding[];
}

export async function requestCodeReview(
  reviewInput: ReviewInput,
): Promise<ReviewResponse> {
  const response = await fetch(`${env.reviewServiceUrl}/api/v1/reviews`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(reviewInput),
  });

  if (!response.ok) {
    const errorBody = await response.text();

    throw new Error(`Review service returned ${response.status}: ${errorBody}`);
  }

  return (await response.json()) as ReviewResponse;
}
