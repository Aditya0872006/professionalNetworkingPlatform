import { useEffect, useState } from "react";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { AdminHeader } from "../../components/AdminHeader/AdminHeader";
import { IAdminJob } from "../../types";
import classes from "./AdminJobs.module.scss";

export function AdminJobs() {
  usePageTitle("Admin | Job Moderation");
  const [jobs, setJobs] = useState<IAdminJob[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [feedback, setFeedback] = useState("");

  const fetchJobs = async () => {
    setIsLoading(true);
    await request<IAdminJob[]>({
      endpoint: "/api/v1/admin/jobs",
      onSuccess: (data) => setJobs(data),
      onFailure: (err) => console.error("Error fetching admin jobs:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  const handleRemoveJob = async (jobId: number) => {
    const reason = window.prompt("Please enter the reason for removing this job posting:", "Violates platform job posting guidelines.");
    if (!reason) return;

    await request({
      endpoint: `/api/v1/admin/jobs/${jobId}?reason=${encodeURIComponent(reason)}`,
      method: "DELETE",
      onSuccess: () => {
        setJobs((prev) =>
          prev.map((j) =>
            j.id === jobId
              ? { ...j, status: "REMOVED", removalReason: reason }
              : j
          )
        );
        setFeedback("Job has been removed. Recruiter has been notified.");
        setTimeout(() => setFeedback(""), 4000);
      },
      onFailure: (err) => alert(err),
    });
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case "PUBLISHED":
        return classes.published;
      case "CLOSED":
        return classes.closed;
      case "REMOVED":
      default:
        return classes.removed;
    }
  };

  return (
    <div className={classes.root}>
      <AdminHeader title="Platform Job Moderation" />

      {feedback && (
        <div style={{ background: "#e8f5e9", color: "#2e7d32", padding: "0.75rem 1rem", borderRadius: "6px", marginBottom: "1rem" }}>
          ✓ {feedback}
        </div>
      )}

      <div className={classes.headerCard}>
        <h2>All Platform Job Openings</h2>
        <span>Total: <strong>{jobs.length}</strong></span>
      </div>

      {isLoading ? (
        <Loader isInline />
      ) : (
        <div className={classes.jobsTableWrapper}>
          <table>
            <thead>
              <tr>
                <th>Job Title</th>
                <th>Company</th>
                <th>Recruiter</th>
                <th>Location</th>
                <th>Status</th>
                <th>Date Posted</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => (
                <tr key={job.id}>
                  <td>
                    <strong>{job.title}</strong>
                    {job.removalReason && (
                      <div className={classes.removalInfo}>
                        Reason: {job.removalReason}
                      </div>
                    )}
                  </td>
                  <td>{job.company}</td>
                  <td>
                    {job.recruiter
                      ? `${job.recruiter.firstName || ""} ${job.recruiter.lastName || ""} (${job.recruiter.email})`
                      : "-"}
                  </td>
                  <td>{job.location || "Remote"}</td>
                  <td>
                    <span className={`${classes.statusBadge} ${getStatusClass(job.status)}`}>
                      {job.status}
                    </span>
                  </td>
                  <td>{new Date(job.createdAt).toLocaleDateString()}</td>
                  <td>
                    {job.status !== "REMOVED" ? (
                      <button
                        className={classes.removeBtn}
                        onClick={() => handleRemoveJob(job.id)}
                      >
                        Remove Job
                      </button>
                    ) : (
                      <span style={{ fontSize: "0.75rem", color: "#888" }}>Takedown Complete</span>
                    )}
                  </td>
                </tr>
              ))}
              {jobs.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "2rem", color: "#666" }}>
                    No jobs posted on the platform yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
