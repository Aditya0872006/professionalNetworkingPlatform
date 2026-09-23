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
