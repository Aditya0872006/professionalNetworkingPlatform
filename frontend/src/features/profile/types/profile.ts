export interface IUserSkill {
  id: number;
  skillName: string;
}

export interface IEducation {
  id: number;
  institution: string;
  degree?: string | null;
  fieldOfStudy?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

export interface IExperience {
  id: number;
  companyName: string;
  jobTitle: string;
  startDate?: string | null;
  endDate?: string | null;
  description?: string | null;
}

export interface IProject {
  id: number;
  projectName: string;
  description?: string | null;
  projectUrl?: string | null;
}
