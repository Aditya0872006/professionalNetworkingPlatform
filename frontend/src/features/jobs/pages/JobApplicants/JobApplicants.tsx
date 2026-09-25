import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { ApplicationStatus, IJob, IJobApplication } from "../../types";
import classes from "./JobApplicants.module.scss";

interface IMatchResult {
  filename: string;
  similarity_score: number;
}

export function JobApplicants() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  usePageTitle("Job Applicants | LinkedIn");

  const [applicants, setApplicants] = useState<IJobApplication[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [jobTitle, setJobTitle] = useState("");
  const [feedbackMessage, setFeedbackMessage] = useState("");
  const [matchError, setMatchError] = useState("");

  const [matchResults, setMatchResults] = useState<IMatchResult[]>([]);
  const [isMatching, setIsMatching] = useState(false);
  const [sortByScore, setSortByScore] = useState(false);

  const fetchApplicants = async () => {
    setIsLoading(true);
    await request<IJobApplication[]>({
      endpoint: `/api/v1/jobs/${jobId}/applicants`,
      onSuccess: (data) => {
        setApplicants(data);
        if (data.length > 0 && !jobTitle) {
          setJobTitle(data[0].jobTitle);
        }
      },
      onFailure: (err) => console.error("Error fetching applicants:", err),
    });
    setIsLoading(false);
  };

  const fetchJob = async () => {
    await request<IJob>({
      endpoint: `/api/v1/jobs/${jobId}`,
      onSuccess: (data) => {
        setJobTitle(data.title);
      },
      onFailure: (err) => console.error("Error fetching job details:", err),
    });
  };

  useEffect(() => {
    fetchApplicants();
    fetchJob();
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

  const handleMatchResumes = async () => {
    setMatchError("");

    if (applicants.length === 0) {
      setMatchError("No applicants have submitted an application for this role yet.");
      return;
    }

    setIsMatching(true);

    await request<{ message: string; results: IMatchResult[] }>({
      endpoint: `/api/v1/jobs/${jobId}/match-applicants`,
      onSuccess: (data) => {
        const results = data.results || [];
        setMatchResults(results);
        setFeedbackMessage(
          `AI resume matching complete! Successfully identified top ${results.length} matching candidate${
            results.length === 1 ? "" : "s"
          }.`
        );
        setTimeout(() => setFeedbackMessage(""), 5000);
      },
      onFailure: (err) => {
        setMatchError(err || "Failed to evaluate candidates. Please try again.");
      },
    });

    setIsMatching(false);
  };

  const getApplicantMatch = (resumeUrl?: string) => {
    if (!resumeUrl || matchResults.length === 0) return null;
    const index = matchResults.findIndex((r) => r.filename === resumeUrl);
    if (index === -1) return null;
    return {
      rank: index + 1,
      score: matchResults[index].similarity_score,
      percentage: Math.round(matchResults[index].similarity_score * 100),
    };
  };

  const topCandidates = matchResults
    .map((res, index) => {
      const applicant = applicants.find((a) => a.resumeUrl === res.filename);
      return {
        rank: index + 1,
        score: res.similarity_score,
        percentage: Math.round(res.similarity_score * 100),
        applicant,
        filename: res.filename,
      };
    })
    .filter((item) => !!item.applicant);

  const displayedApplicants = [...applicants].sort((a, b) => {
    if (!sortByScore) return 0;
    const matchA = getApplicantMatch(a.resumeUrl);
    const matchB = getApplicantMatch(b.resumeUrl);
    const scoreA = matchA ? matchA.score : -1;
    const scoreB = matchB ? matchB.score : -1;
    return scoreB - scoreA;
  });

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

        <div className={classes.headerActions}>
          <button
            type="button"
            className={classes.aiMatchButton}
            onClick={handleMatchResumes}
            disabled={isMatching || applicants.length === 0}
            title="Scan candidate resumes against the job description to find the best 3 matches"
          >
            {isMatching ? (
              <>
                <span className={classes.spinner} /> Matching Resumes...
              </>
            ) : (
              <>✨ Match Resumes with AI</>
            )}
          </button>

          <Button outline onClick={() => navigate("/recruiter/jobs")}>
            Back to Dashboard
          </Button>
        </div>
      </div>

      {feedbackMessage && (
        <div className={classes.successMessage}>
          ✓ {feedbackMessage}
        </div>
      )}

      {matchError && (
        <div className={classes.errorMessage}>
          ⚠️ {matchError}
        </div>
      )}

      {topCandidates.length > 0 && (
        <div className={classes.topMatchesSection}>
          <div className={classes.topMatchesHeader}>
            <div>
              <h2>🎯 Top 3 AI Matching Resumes</h2>
              <p>
                Candidates ranked by TF-IDF cosine similarity against the job description.
              </p>
            </div>
            <button
              type="button"
              className={classes.sortToggleBtn}
              onClick={() => setSortByScore((prev) => !prev)}
            >
              {sortByScore ? "↩ Reset List Order" : "⚡ Sort Applicants by Match Score"}
            </button>
          </div>

          <div className={classes.topMatchesGrid}>
            {topCandidates.map((item) => (
              <div
                key={item.rank}
                className={`${classes.matchCard} ${classes[`rank${item.rank}`]}`}
              >
                <div className={classes.matchCardTop}>
                  <span className={`${classes.rankBadge} ${classes[`badgeRank${item.rank}`]}`}>
                    #{item.rank} Match
                  </span>
                  <span className={classes.scorePill}>
                    {item.percentage}% Score ({item.score})
                  </span>
                </div>

                <div className={classes.matchCandidateInfo}>
                  <img
                    className={classes.matchAvatar}
                    src={
                      item.applicant?.applicantProfilePicture
                        ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${item.applicant.applicantProfilePicture}`
                        : "/avatar.svg"
                    }
                    alt={item.applicant?.applicantName}
                  />
                  <div>
                    <h4>{item.applicant?.applicantName}</h4>
                    <p className={classes.matchTitle}>
                      {item.applicant?.applicantPosition || "Candidate"}
                    </p>
                    <span className={classes.matchEmail}>
                      📧 {item.applicant?.applicantEmail}
                    </span>
                  </div>
                </div>

                {item.applicant?.skills && item.applicant.skills.length > 0 && (
                  <div className={classes.matchSkills}>
                    {item.applicant.skills.slice(0, 4).map((skill, idx) => (
                      <span key={idx} className={classes.matchSkillChip}>
                        {skill}
                      </span>
                    ))}
                    {item.applicant.skills.length > 4 && (
                      <span className={classes.matchSkillChip}>
                        +{item.applicant.skills.length - 4}
                      </span>
                    )}
                  </div>
                )}

                {item.applicant?.resumeUrl && (
                  <a
                    href={`${import.meta.env.VITE_API_URL}/api/v1/storage/${item.applicant.resumeUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={classes.matchResumeLink}
                  >
                    📥 View Resume
                  </a>
                )}
              </div>
            ))}
          </div>
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
          {displayedApplicants.map((app) => {
            const match = getApplicantMatch(app.resumeUrl);

            return (
              <div
                key={app.id}
                className={`${classes.applicantCard} ${
                  match ? classes[`matchedApplicantRank${match.rank}`] : ""
                }`}
              >
                {match && (
                  <div className={`${classes.cardMatchBanner} ${classes[`bannerRank${match.rank}`]}`}>
                    ✨ Top Match #{match.rank} • {match.percentage}% Match (Score: {match.score})
                  </div>
                )}

                <div className={classes.cardMain}>
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
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
