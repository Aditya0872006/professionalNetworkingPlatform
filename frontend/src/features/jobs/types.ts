export type EmploymentType = "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERNSHIP";
export type WorkMode = "ON_SITE" | "HYBRID" | "REMOTE";
export type JobStatus = "DRAFT" | "PUBLISHED" | "CLOSED" | "REMOVED";
export type ApplicationStatus = "APPLIED" | "UNDER_REVIEW" | "SHORTLISTED" | "REJECTED" | "ACCEPTED";

export interface IJob {
  id: number;
  recruiterId: number;
  recruiterName: string;
  title: string;
  company: string;
  description: string;
  location: string;
  employmentType: EmploymentType;
  workMode: WorkMode;
  salary?: string;
  minExperience?: number;
  maxExperience?: number;
  requiredSkills: string[];
  educationRequirement?: string;
  deadline?: string;
  numberOfOpenings?: number;
  status: JobStatus;
  applicantCount: number;
  expired: boolean;
  hasApplied: boolean;
  createdAt: string;
}

export interface IJobApplication {
  id: number;
  jobId: number;
  jobTitle: string;
  company: string;
  applicantId: number;
  applicantName: string;
  applicantEmail: string;
  applicantPosition?: string;
  applicantLocation?: string;
  applicantProfilePicture?: string;
  skills: string[];
  resumeUrl: string;
  status: ApplicationStatus;
  appliedAt: string;
}

export interface IJobFormData {
  title: string;
  company: string;
  description: string;
  location: string;
  employmentType: EmploymentType;
  workMode: WorkMode;
  salary?: string;
  minExperience?: number;
  maxExperience?: number;
  requiredSkills: string[];
  educationRequirement?: string;
  deadline?: string;
  numberOfOpenings?: number;
}

export interface ITopApplicantEvaluation {
  isTopApplicant: boolean;
  matchScore: number;
  matchPercentage: number;
  rank?: number;
  totalApplicants: number;
  percentile?: number;
  isEarlyApplicant: boolean;
  matchedSkills: string[];
  missingSkills: string[];
  headlineMessage: string;
  matchTier: "TOP_APPLICANT" | "STRONG_MATCH" | "MODERATE_MATCH" | "GROWTH_OPPORTUNITY";
}

