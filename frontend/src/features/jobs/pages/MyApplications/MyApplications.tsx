import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { IJobApplication } from "../../types";
import classes from "./MyApplications.module.scss";

export function MyApplications() {
  usePageTitle("My Applications | LinkedIn");
  const navigate = useNavigate();

  const [applications, setApplications] = useState<IJobApplication[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchApplications = async () => {
      setIsLoading(true);
      await request<IJobApplication[]>({
        endpoint: "/api/v1/jobs/my-applications",
        onSuccess: (data) => setApplications(data),
        onFailure: (err) => console.error("Error fetching applications:", err),
      });
      setIsLoading(false);
    };

    fetchApplications();
  }, []);

  const getStatusClass = (status: string) => {
    switch (status) {
      case "UNDER_REVIEW":
        return classes.underReview;
      case "SHORTLISTED":
        return classes.shortlisted;
      case "ACCEPTED":
        return classes.accepted;
      case "REJECTED":
        return classes.rejected;
      case "APPLIED":
      default:
        return classes.applied;
    }
  };

  const formatStatus = (status: string) => {
    return status ? status.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()) : "";
  };

  return (
    <div className={classes.root}>
      <div className={classes.header}>
        <div>
          <h1>My Job Applications</h1>
          <p>Track your submitted applications and hiring stages</p>
        </div>
        <Button outline onClick={() => navigate("/jobs")}>
          Explore More Jobs
        </Button>
      </div>

      {isLoading ? (
        <Loader isInline />
      ) : applications.length === 0 ? (
        <div className={classes.emptyState}>
          <h3>No Applications Submitted Yet</h3>
          <p>You haven't applied to any roles. Find your next opportunity in our jobs directory.</p>
          <Button onClick={() => navigate("/jobs")}>Browse Jobs</Button>
        </div>
      ) : (
        <div className={classes.applicationsList}>
          {applications.map((app) => (
            <div key={app.id} className={classes.appCard}>
              <div className={classes.info}>
                <h3
                  className={classes.jobTitle}
                  onClick={() => navigate(`/jobs/${app.jobId}`)}
                >
                  {app.jobTitle}
                </h3>
                <div className={classes.company}>{app.company}</div>
                <div className={classes.appliedDate}>
                  Applied on {new Date(app.appliedAt).toLocaleDateString()}
                </div>
              </div>

              <div className={classes.statusAndActions}>
                <span className={`${classes.statusBadge} ${getStatusClass(app.status)}`}>
                  {formatStatus(app.status)}
                </span>
                {app.resumeUrl && (
                  <a
                    href={`${import.meta.env.VITE_API_URL}/api/v1/storage/${app.resumeUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={classes.resumeLink}
                  >
                    📄 View Resume
                  </a>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
