import { IUser } from "../authentication/contexts/AuthenticationContextProvider";
import { JobStatus } from "../jobs/types";

export interface IAdminStats {
  totalUsers: number;
  totalRecruiters: number;
  pendingRecruiterRequests: number;
  totalJobs: number;
  totalPosts: number;
  blockedUsers: number;
}

export type RecruiterStatus = "PENDING" | "APPROVED" | "REJECTED" | "PERMANENTLY_REJECTED";

export type RecruiterTrustLevel =
  | "LEGITIMATE"
  | "SUSPICIOUS"
  | "LIKELY_FAKE"
  | "HIGH_RISK";

export interface IRecruiterProfile {
  id: number;
  user: IUser;
  companyName: string;
  companyEmail?: string;
  companyWebsite?: string;
  companyDescription?: string;
  companyLocation?: string;
  status: RecruiterStatus;
  rejectionCount: number;
  createdAt: string;
}

export interface IRecruiterAIReport {
  recruiterProfileId: number;
  trustLevel: RecruiterTrustLevel;
  trustScore: number;
  emailScore: number;
  websiteScore: number;
  companyScore: number;
  locationScore: number;
  consistencyScore: number;
  flags: string[];
  reasoning: string;
  modelUsed: string;
  analyzedAt: string;
}

export interface IAdminJob {
  id: number;
  recruiter?: IUser;
  title: string;
  company: string;
  location: string;
  status: JobStatus;
  removalReason?: string;
  removedBy?: string;
  removedAt?: string;
  createdAt: string;
}

export interface IAdminPost {
  id: number;
  author: IUser;
  content: string;
  picture?: string;
  creationDate: string;
}
