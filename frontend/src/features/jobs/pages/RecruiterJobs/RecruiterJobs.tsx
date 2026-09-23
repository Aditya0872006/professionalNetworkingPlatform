import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { IJob } from "../../types";
import classes from "./RecruiterJobs.module.scss";

export function RecruiterJobs() {
  usePageTitle("Recruiter Job Dashboard | LinkedIn");
  const navigate = useNavigate();

  const [jobs, setJobs] = useState<IJob[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionError, setActionError] = useState("");

  const fetchMyJobs = async () => {
    setIsLoading(true);
    await request<IJob[]>({
      endpoint: "/api/v1/jobs/my-jobs",
      onSuccess: (data) => setJobs(data),
      onFailure: (err) => {
        console.error("Error fetching recruiter jobs:", err);
        setActionError(err);
      },
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchMyJobs();
  }, []);

  const handleCloseJob = async (jobId: number) => {
    if (!window.confirm("Are you sure you want to close applications for this job?")) return;
    await request({
      endpoint: `/api/v1/jobs/${jobId}/close`,
      method: "PUT",
      onSuccess: () => {
        setJobs((prev) =>
          prev.map((j) => (j.id === jobId ? { ...j, status: "CLOSED" } : j))
        );
      },
      onFailure: (err) => setActionError(err),
    });
  };

  const handlePublishJob = async (jobId: number) => {
    await request({
      endpoint: `/api/v1/jobs/${jobId}/publish`,
      method: "PUT",
      onSuccess: () => {
        setJobs((prev) =>
          prev.map((j) => (j.id === jobId ? { ...j, status: "PUBLISHED" } : j))
        );
      },
      onFailure: (err) => setActionError(err),
    });
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case "PUBLISHED":
        return classes.published;
      case "CLOSED":
        return classes.closed;
      case "REMOVED":
        return classes.removed;
      default:
        return "";
    }
  };

  return (
    <div className={classes.root}>
      <div className={classes.header}>
        <div>
          <h1>Recruiter Job Board</h1>
          <p>Manage your job postings, view candidates, and update opening statuses</p>
        </div>
        <Button onClick={() => navigate("/recruiter/jobs/create")}>
          + Post a New Job
        </Button>
      </div>

      {actionError && (
        <p style={{ color: "#d32f2f", background: "#fde8e8", padding: "0.8rem", borderRadius: "6px", marginBottom: "1rem" }}>
          {actionError}
        </p>
      )}

      {isLoading ? (
        <Loader isInline />
      ) : jobs.length === 0 ? (
        <div className={classes.emptyState}>
          <h3>No Job Postings Yet</h3>
          <p>Get started by creating your company's first job opportunity.</p>
          <Button onClick={() => navigate("/recruiter/jobs/create")}>
            Post Your First Job
          </Button>
        </div>
      ) : (
        <div className={classes.jobsList}>
          {jobs.map((job) => (
            <div key={job.id} className={classes.jobCard}>
              <div className={classes.info}>
                <h3 className={classes.title}>{job.title}</h3>
                <div className={classes.meta}>
                  <span>🏢 {job.company}</span>
                  <span>📍 {job.location || "Remote"}</span>
                  <span>📅 Posted: {new Date(job.createdAt).toLocaleDateString()}</span>
                  {job.deadline && (
                    <span>
                      ⏰ Deadline: {new Date(job.deadline).toLocaleDateString()}
                    </span>
                  )}
                </div>
                <div>
                  <span className={`${classes.statusTag} ${getStatusClass(job.status)}`}>
                    {job.status}
                  </span>
                </div>
              </div>

              <div className={classes.actions}>
                <Button
                  onClick={() => navigate(`/recruiter/jobs/${job.id}/applicants`)}
                >
                  Applicants ({job.applicantCount || 0})
                </Button>

                {job.status === "PUBLISHED" ? (
                  <Button
                    outline
                    onClick={() => handleCloseJob(job.id)}
                  >
                    Close Job
                  </Button>
                ) : job.status === "CLOSED" ? (
                  <Button
                    outline
                    onClick={() => handlePublishJob(job.id)}
                  >
                    Reopen
                  </Button>
                ) : null}

                <Button
                  outline
                  size="small"
                  onClick={() => navigate(`/jobs/${job.id}`)}
                >
                  View Public
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
