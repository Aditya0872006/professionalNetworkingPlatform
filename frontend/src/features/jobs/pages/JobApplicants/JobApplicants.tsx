import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { ApplicationStatus, IJobApplication } from "../../types";
import classes from "./JobApplicants.module.scss";

export function JobApplicants() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  usePageTitle("Job Applicants | LinkedIn");

  const [applicants, setApplicants] = useState<IJobApplication[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [jobTitle, setJobTitle] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");

  const fetchApplicants = async () => {
    setIsLoading(true);
    await request<IJobApplication[]>({
      endpoint: `/api/v1/jobs/${jobId}/applicants`,
      onSuccess: (data) => {
        setApplicants(data);
        if (data.length > 0) {
          setJobTitle(data[0].jobTitle);
        }
      },
      onFailure: (err) => console.error("Error fetching applicants:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchApplicants();
  }, [jobId]);

  const handleStatusChange = async (applicationId: number, newStatus: ApplicationStatus) => {
    await request({
      endpoint: `/api/v1/jobs/applications/${applicationId}/status?status=${newStatus}`,
      method: "PUT",
      onSuccess: () => {
        setApplicants((prev) =>
          prev.map((app) =>
            app.id === applicationId ? { ...app, status: newStatus } : app
          )
        );
        setFeedbackMessage(`Application status updated to ${newStatus.replace(/_/g, " ")}.`);
        setTimeout(() => setFeedbackMessage(""), 4000);
      },
      onFailure: (err) => alert(err),
    });
  };

  return (
    <div className={classes.root}>
      <div className={classes.header}>
        <div>
          <span className={classes.backLink} onClick={() => navigate("/recruiter/jobs")}>
            ← Back to Recruiter Jobs
          </span>
          <h1>Candidate Applicants</h1>
          <p>
            {jobTitle ? `Candidates for ${jobTitle}` : `Job #${jobId}`} • {applicants.length} total applicant{applicants.length === 1 ? "" : "s"}
          </p>
        </div>
        <Button outline onClick={() => navigate("/recruiter/jobs")}>
          Back to Dashboard
        </Button>
      </div>

      {feedbackMessage && (
        <div style={{ background: "#e8f5e9", color: "#2e7d32", padding: "0.75rem 1rem", borderRadius: "6px", marginBottom: "1rem" }}>
          ✓ {feedbackMessage}
        </div>
      )}

      {isLoading ? (
        <Loader isInline />
      ) : applicants.length === 0 ? (
        <div className={classes.emptyState}>
          <h3>No Applicants Yet</h3>
          <p>No candidates have submitted an application for this role yet. Check back soon!</p>
          <Button outline onClick={() => navigate("/recruiter/jobs")}>
            Return to Job Board
          </Button>
        </div>
      ) : (
        <div className={classes.applicantsList}>
          {applicants.map((app) => (
            <div key={app.id} className={classes.applicantCard}>
              <div className={classes.candidateProfile}>
                <img
                  className={classes.avatar}
                  src={
                    app.applicantProfilePicture
                      ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${app.applicantProfilePicture}`
                      : "/avatar.svg"
                  }
                  alt={app.applicantName}
                />
                <div className={classes.details}>
                  <h3>{app.applicantName}</h3>
                  <div className={classes.title}>
                    {app.applicantPosition || "Candidate"}
                  </div>
                  <div className={classes.meta}>
                    <span>📧 {app.applicantEmail}</span>
                    {app.applicantLocation && <span>📍 {app.applicantLocation}</span>}
                  </div>

                  {app.skills && app.skills.length > 0 && (
                    <div className={classes.skillsList}>
                      {app.skills.map((skill, idx) => (
                        <span key={idx} className={classes.skillChip}>
                          {skill}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className={classes.actionControls}>
                <label style={{ fontSize: "0.75rem", fontWeight: 600, color: "#666" }}>
                  Application Status
                </label>
                <select
                  className={classes.statusSelect}
                  value={app.status}
                  onChange={(e) =>
                    handleStatusChange(app.id, e.target.value as ApplicationStatus)
                  }
                >
                  <option value="APPLIED">Applied</option>
                  <option value="UNDER_REVIEW">Under Review</option>
                  <option value="SHORTLISTED">Shortlisted</option>
                  <option value="ACCEPTED">Accepted / Offered</option>
                  <option value="REJECTED">Rejected</option>
                </select>

                {app.resumeUrl && (
                  <a
                    href={`${import.meta.env.VITE_API_URL}/api/v1/storage/${app.resumeUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={classes.resumeBtn}
                  >
                    📥 Download Resume
                  </a>
                )}

                <span className={classes.appliedTime}>
                  Applied: {new Date(app.appliedAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
