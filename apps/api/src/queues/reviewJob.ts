import type { ReviewInput } from "../services/review/reviewInputService.js";

export interface ReviewJob {
  installationId: number;
  owner: string;
  repo: string;
  reviewInput: ReviewInput;
}
