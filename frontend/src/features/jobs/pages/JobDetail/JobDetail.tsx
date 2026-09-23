import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { IJob } from "../../types";
import classes from "./JobDetail.module.scss";

export function JobDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuthentication();

  const [job, setJob] = useState<IJob | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);
  const [resumeFile, setResumeFile] = useState<File | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [applyError, setApplyError] = useState("");
  const [applySuccess, setApplySuccess] = useState("");

  const fileInputRef = useRef<HTMLInputElement>(null);

  usePageTitle(job ? `${job.title} at ${job.company}` : "Job Details");

  const fetchJob = async () => {
    setIsLoading(true);
    await request<IJob>({
      endpoint: `/api/v1/jobs/${id}`,
      onSuccess: (data) => setJob(data),
      onFailure: (err) => console.error("Error fetching job details:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchJob();
  }, [id]);

  const handleApplySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resumeFile) {
      setApplyError("Please select a resume file to upload (PDF, DOC, or DOCX).");
      return;
    }

    setIsSubmitting(true);
    setApplyError("");

    const formData = new FormData();
    formData.append("resume", resumeFile);

    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/api/v1/jobs/${id}/apply`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: formData,
      });

      if (!response.ok) {
        let msg = "Failed to submit application.";
        try {
          const errData = await response.json();
          msg = errData.message || msg;
        } catch {
          // ignore
        }
        throw new Error(msg);
      }

      setApplySuccess("Your application was submitted successfully!");
      setIsApplyModalOpen(false);
      setJob((prev) => (prev ? { ...prev, hasApplied: true, applicantCount: (prev.applicantCount || 0) + 1 } : prev));
    } catch (err) {
      if (err instanceof Error) {
        setApplyError(err.message);
      } else {
        setApplyError("An unexpected error occurred during submission.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <Loader />;
  }

  if (!job) {
    return (
      <div className={classes.root}>
        <button className={classes.backBtn} onClick={() => navigate("/jobs")}>
          ← Back to Jobs
        </button>
        <div className={classes.card} style={{ textAlign: "center", padding: "3rem" }}>
          <h2>Job Not Found</h2>
          <p>The job posting you are looking for does not exist or may have been removed.</p>
          <Button onClick={() => navigate("/jobs")}>Browse All Jobs</Button>
        </div>
      </div>
    );
  }

  const formatEmploymentType = (type: string) => {
    return type ? type.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()) : "";
  };

  const formatWorkMode = (mode: string) => {
    return mode ? mode.replace(/_/g, "-").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()) : "";
  };

  const canApply = user?.role === "ROLE_USER" && !job.hasApplied && !job.expired && job.status === "PUBLISHED";

  return (
    <div className={classes.root}>
      <button className={classes.backBtn} onClick={() => navigate("/jobs")}>
        ← Back to Jobs
      </button>

      <div className={classes.card}>
        <div className={classes.header}>
          <div className={classes.titleSection}>
            <h1>{job.title}</h1>
            <div className={classes.company}>{job.company}</div>
            <div className={classes.location}>📍 {job.location || "Remote"}</div>
          </div>

          <div className={classes.actionSection}>
            {job.hasApplied ? (
              <div className={classes.appliedNotice}>
                ✓ You applied for this role
              </div>
            ) : job.expired ? (
              <div style={{ color: "#c62828", fontWeight: 600 }}>
                Deadline Passed
              </div>
            ) : canApply ? (
              <Button onClick={() => setIsApplyModalOpen(true)}>
                Apply Now
              </Button>
            ) : user?.role === "ROLE_RECRUITER" && user.id === String(job.recruiterId) ? (
              <Button outline onClick={() => navigate(`/recruiter/jobs/${job.id}/applicants`)}>
                View Applicants ({job.applicantCount || 0})
              </Button>
            ) : null}

            <span style={{ fontSize: "0.85rem", color: "#666" }}>
              👥 {job.applicantCount || 0} applicants
            </span>
          </div>
        </div>

        {/* Deadline Notification */}
        {job.deadline && (
          <div className={`${classes.deadlineAlert} ${job.expired ? classes.expired : classes.active}`}>
            {job.expired ? (
              <span>
                ⚠️ <strong>Application Closed:</strong> The deadline ({new Date(job.deadline).toLocaleDateString()}) has expired. New applications are no longer accepted.
              </span>
            ) : (
              <span>
                ⏰ <strong>Application Deadline:</strong> Please submit your application before {new Date(job.deadline).toLocaleString()}.
              </span>
            )}
          </div>
        )}

        {applySuccess && (
          <div className={classes.appliedNotice} style={{ margin: "1rem 0" }}>
            ✓ {applySuccess}
          </div>
        )}

        {/* Meta summary grid */}
        <div className={classes.metaGrid}>
          <div className={classes.metaItem}>
            <span className={classes.metaLabel}>Employment Type</span>
            <span className={classes.metaValue}>{formatEmploymentType(job.employmentType)}</span>
          </div>
          <div className={classes.metaItem}>
            <span className={classes.metaLabel}>Workplace</span>
            <span className={classes.metaValue}>{formatWorkMode(job.workMode)}</span>
          </div>
          <div className={classes.metaItem}>
            <span className={classes.metaLabel}>Compensation</span>
            <span className={classes.metaValue}>{job.salary || "Not specified"}</span>
          </div>
          <div className={classes.metaItem}>
            <span className={classes.metaLabel}>Experience</span>
            <span className={classes.metaValue}>
              {job.minExperience != null
                ? `${job.minExperience}${job.maxExperience ? ` - ${job.maxExperience}` : "+"} years`
                : "Entry Level / Any"}
            </span>
          </div>
          {job.numberOfOpenings != null && (
            <div className={classes.metaItem}>
              <span className={classes.metaLabel}>Openings</span>
              <span className={classes.metaValue}>{job.numberOfOpenings}</span>
            </div>
          )}
          {job.educationRequirement && (
            <div className={classes.metaItem}>
              <span className={classes.metaLabel}>Education</span>
              <span className={classes.metaValue}>{job.educationRequirement}</span>
            </div>
          )}
        </div>

        {/* Required Skills */}
        {job.requiredSkills && job.requiredSkills.length > 0 && (
          <div className={classes.section}>
            <h3>Key Skills & Requirements</h3>
            <div className={classes.skillsContainer}>
              {job.requiredSkills.map((skill, index) => (
                <span key={index} className={classes.skillTag}>
                  {skill}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Description */}
        <div className={classes.section}>
          <h3>Job Description</h3>
          <p>{job.description}</p>
        </div>

        {/* Recruiter info */}
        <div className={classes.section} style={{ borderTop: "1px solid #f0f0f0", paddingTop: "1.5rem" }}>
          <h3>Posted by</h3>
          <p style={{ margin: 0, fontWeight: 500, color: "#333" }}>{job.recruiterName}</p>
          <span style={{ fontSize: "0.85rem", color: "#777" }}>
            Posted on {new Date(job.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Apply Modal */}
      {isApplyModalOpen && (
        <div className={classes.modalBackdrop} onClick={() => setIsApplyModalOpen(false)}>
          <div className={classes.modal} onClick={(e) => e.stopPropagation()}>
            <h2>Apply to {job.company}</h2>
            <p>Role: <strong>{job.title}</strong></p>

            {applyError && (
              <p style={{ color: "#d32f2f", background: "#fde8e8", padding: "0.6rem", borderRadius: "4px" }}>
                {applyError}
              </p>
            )}

            <form onSubmit={handleApplySubmit}>
              <div
                className={classes.fileUploadArea}
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".pdf,.doc,.docx"
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      setResumeFile(e.target.files[0]);
                      setApplyError("");
                    }
                  }}
                />
                <div className={classes.uploadPrompt}>
                  📁 {resumeFile ? "Change Resume File" : "Click to Upload Resume"}
                </div>
                <div className={classes.fileFormatNotice}>
                  Accepted formats: PDF, DOC, DOCX (Max 10MB)
                </div>
                {resumeFile && (
                  <div className={classes.selectedFileName}>
                    ✓ Selected: {resumeFile.name} ({(resumeFile.size / 1024).toFixed(1)} KB)
                  </div>
                )}
              </div>

              <div className={classes.modalActions}>
                <Button
                  type="button"
                  outline
                  onClick={() => setIsApplyModalOpen(false)}
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={isSubmitting || !resumeFile}>
                  {isSubmitting ? "Submitting..." : "Submit Application"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
