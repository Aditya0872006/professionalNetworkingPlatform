import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { EmploymentType, WorkMode } from "../../types";
import classes from "./PostJob.module.scss";

export function PostJob() {
  usePageTitle("Post a Job | LinkedIn");
  const { user } = useAuthentication();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [company, setCompany] = useState(user?.company || "");
  const [location, setLocation] = useState(user?.location || "");
  const [employmentType, setEmploymentType] = useState<EmploymentType>("FULL_TIME");
  const [workMode, setWorkMode] = useState<WorkMode>("HYBRID");
  const [salary, setSalary] = useState("");
  const [minExp, setMinExp] = useState<number | "">("");
  const [maxExp, setMaxExp] = useState<number | "">("");
  const [skillsString, setSkillsString] = useState("");
  const [education, setEducation] = useState("");
  const [deadline, setDeadline] = useState("");
  const [openings, setOpenings] = useState<number>(1);
  const [description, setDescription] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");

    if (!title.trim() || !company.trim() || !location.trim() || !description.trim()) {
      setErrorMessage("Please fill in all mandatory job details (Title, Company, Location, Description).");
      return;
    }

    const skills = skillsString
      .split(",")
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

    const payload = {
      title: title.trim(),
      company: company.trim(),
      location: location.trim(),
      employmentType,
      workMode,
      salary: salary.trim() || null,
      minExperience: minExp === "" ? null : Number(minExp),
      maxExperience: maxExp === "" ? null : Number(maxExp),
      requiredSkills: skills,
      educationRequirement: education.trim() || null,
      deadline: deadline ? `${deadline}:00` : null,
      numberOfOpenings: openings || 1,
      description: description.trim(),
    };

    setIsSubmitting(true);

    await request({
      endpoint: "/api/v1/jobs",
      method: "POST",
      body: JSON.stringify(payload),
      onSuccess: () => {
        navigate("/recruiter/jobs");
      },
      onFailure: (err) => {
        setErrorMessage(err);
        setIsSubmitting(false);
      },
    });
  };

  return (
    <div className={classes.root}>
      <div className={classes.card}>
        <h1>Post a New Opportunity</h1>
        <p>Attract qualified professionals from across the platform to join your team.</p>

        {errorMessage && <div className={classes.errorBanner}>{errorMessage}</div>}

        <form onSubmit={handleSubmit}>
          <div className={classes.formGroup}>
            <label htmlFor="title">Job Title *</label>
            <input
              id="title"
              type="text"
              placeholder="e.g. Senior Full Stack Engineer"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div className={classes.row}>
            <div className={classes.formGroup}>
              <label htmlFor="company">Company Name *</label>
              <input
                id="company"
                type="text"
                placeholder="Company hiring for this role"
                value={company}
                onChange={(e) => setCompany(e.target.value)}
                required
              />
            </div>
            <div className={classes.formGroup}>
              <label htmlFor="location">Job Location *</label>
              <input
                id="location"
                type="text"
                placeholder="e.g. San Francisco, CA or Remote"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                required
              />
            </div>
          </div>

          <div className={classes.row}>
            <div className={classes.formGroup}>
              <label htmlFor="employmentType">Employment Type</label>
              <select
                id="employmentType"
                value={employmentType}
                onChange={(e) => setEmploymentType(e.target.value as EmploymentType)}
              >
                <option value="FULL_TIME">Full Time</option>
                <option value="PART_TIME">Part Time</option>
                <option value="CONTRACT">Contract</option>
                <option value="INTERNSHIP">Internship</option>
              </select>
            </div>
            <div className={classes.formGroup}>
              <label htmlFor="workMode">Workplace Mode</label>
              <select
                id="workMode"
                value={workMode}
                onChange={(e) => setWorkMode(e.target.value as WorkMode)}
              >
                <option value="HYBRID">Hybrid</option>
                <option value="ON_SITE">On-site</option>
                <option value="REMOTE">Remote</option>
              </select>
            </div>
          </div>

          <div className={classes.row}>
            <div className={classes.formGroup}>
              <label htmlFor="salary">Salary / Compensation (Optional)</label>
              <input
                id="salary"
                type="text"
                placeholder="e.g. $120,000 - $150,000 / yr"
                value={salary}
                onChange={(e) => setSalary(e.target.value)}
              />
            </div>
            <div className={classes.formGroup}>
              <label htmlFor="openings">Number of Openings</label>
              <input
                id="openings"
                type="number"
                min={1}
                value={openings}
                onChange={(e) => setOpenings(Number(e.target.value))}
              />
            </div>
          </div>

          <div className={classes.row}>
            <div className={classes.formGroup}>
              <label htmlFor="minExp">Min Experience (Years)</label>
              <input
                id="minExp"
                type="number"
                min={0}
                placeholder="e.g. 3"
                value={minExp}
                onChange={(e) => setMinExp(e.target.value === "" ? "" : Number(e.target.value))}
              />
            </div>
            <div className={classes.formGroup}>
              <label htmlFor="maxExp">Max Experience (Years)</label>
              <input
                id="maxExp"
                type="number"
                min={0}
                placeholder="e.g. 7"
                value={maxExp}
                onChange={(e) => setMaxExp(e.target.value === "" ? "" : Number(e.target.value))}
              />
            </div>
          </div>

          <div className={classes.formGroup}>
            <label htmlFor="skills">Required Skills (Comma separated)</label>
            <input
              id="skills"
              type="text"
              placeholder="e.g. Java, Spring Boot, React, TypeScript, Docker"
              value={skillsString}
              onChange={(e) => setSkillsString(e.target.value)}
            />
            <span className={classes.helper}>
              Candidates matching these skills will receive job recommendation notifications!
            </span>
          </div>

          <div className={classes.row}>
            <div className={classes.formGroup}>
              <label htmlFor="education">Education Requirement</label>
              <input
                id="education"
                type="text"
                placeholder="e.g. Bachelor's Degree in Computer Science or equivalent"
                value={education}
                onChange={(e) => setEducation(e.target.value)}
              />
            </div>
            <div className={classes.formGroup}>
              <label htmlFor="deadline">Application Deadline</label>
              <input
                id="deadline"
                type="datetime-local"
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
              />
              <span className={classes.helper}>
                Applications are strictly blocked after this date and time.
              </span>
            </div>
          </div>

          <div className={classes.formGroup}>
            <label htmlFor="description">Job Description & Responsibilities *</label>
            <textarea
              id="description"
              placeholder="Provide a comprehensive description of the role, daily responsibilities, and qualifications..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>

          <div className={classes.actions}>
            <Button
              type="button"
              outline
              onClick={() => navigate("/recruiter/jobs")}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Publishing Job..." : "Publish Job Opening"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
