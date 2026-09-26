import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { IJob, IJobRecommendation, RecommendationTier } from "../../types";
import classes from "./Jobs.module.scss";

export function Jobs() {
  usePageTitle("Jobs & AI Recommendations | LinkedIn");
  const { user } = useAuthentication();
  const navigate = useNavigate();

  // Active view tab: AI Recommendations vs All Opportunities
  const [activeTab, setActiveTab] = useState<"RECOMMENDED" | "ALL">(
    user?.role === "ROLE_USER" ? "RECOMMENDED" : "ALL"
  );

  // Recommendations state
  const [recommendations, setRecommendations] = useState<IJobRecommendation[]>([]);
  const [isLoadingRecs, setIsLoadingRecs] = useState(false);
  const [selectedTierFilter, setSelectedTierFilter] = useState<"ALL" | RecommendationTier>("ALL");
  const [selectedBreakdown, setSelectedBreakdown] = useState<IJobRecommendation | null>(null);

  // Standard job search state
  const [jobs, setJobs] = useState<IJob[]>([]);
  const [isLoadingJobs, setIsLoadingJobs] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [locationQuery, setLocationQuery] = useState("");
  const [employmentTypeFilter, setEmploymentTypeFilter] = useState("");
  const [workModeFilter, setWorkModeFilter] = useState("");

  const fetchRecommendations = async () => {
    if (user?.role !== "ROLE_USER") return;
    setIsLoadingRecs(true);
    await request<IJobRecommendation[]>({
      endpoint: "/api/v1/jobs/recommendations",
      onSuccess: (data) => setRecommendations(data || []),
      onFailure: (err) => console.error("Error fetching job recommendations:", err),
    });
    setIsLoadingRecs(false);
  };

  const fetchJobs = async () => {
    setIsLoadingJobs(true);
    let url = "/api/v1/jobs";
    const params = new URLSearchParams();
    if (searchQuery.trim()) params.append("query", searchQuery.trim());
    if (locationQuery.trim()) params.append("location", locationQuery.trim());
    const queryString = params.toString();
    if (queryString) url += `?${queryString}`;

    await request<IJob[]>({
      endpoint: url,
      onSuccess: (data) => setJobs(data || []),
      onFailure: (err) => console.error("Error fetching jobs:", err),
    });
    setIsLoadingJobs(false);
  };

  useEffect(() => {
    fetchJobs();
    if (user?.role === "ROLE_USER") {
      fetchRecommendations();
    }
  }, [user?.id, user?.role]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchJobs();
  };

  const handleClearFilters = () => {
    setSearchQuery("");
    setLocationQuery("");
    setEmploymentTypeFilter("");
    setWorkModeFilter("");
    request<IJob[]>({
      endpoint: "/api/v1/jobs",
      onSuccess: (data) => setJobs(data || []),
      onFailure: (err) => console.error(err),
    });
  };

  // Filter recommendations by tier
  const filteredRecommendations = recommendations.filter((rec) => {
    if (selectedTierFilter === "ALL") return true;
    return rec.recommendationTier === selectedTierFilter;
  });

  // Filter regular jobs list
  const filteredJobs = jobs.filter((job) => {
    if (employmentTypeFilter && job.employmentType !== employmentTypeFilter) return false;
    if (workModeFilter && job.workMode !== workModeFilter) return false;
    return true;
  });

  const getWorkModeClass = (mode: string) => {
    switch (mode) {
      case "REMOTE":
        return classes.remote;
      case "HYBRID":
        return classes.hybrid;
      case "ON_SITE":
      default:
        return classes.onSite;
    }
  };

  const getEmploymentTypeClass = (type: string) => {
    switch (type) {
      case "FULL_TIME":
        return classes.fullTime;
      case "PART_TIME":
        return classes.partTime;
      case "CONTRACT":
        return classes.contract;
      case "INTERNSHIP":
        return classes.internship;
      default:
        return "";
    }
  };

  const formatEmploymentType = (type: string) => {
    return type ? type.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()) : "";
  };

  const formatWorkMode = (mode: string) => {
    return mode ? mode.replace(/_/g, "-").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase()) : "";
  };

  const getTierBadgeClass = (tier: RecommendationTier) => {
    switch (tier) {
      case "TOP_MATCH":
        return classes.matchScoreTop;
      case "STRONG_MATCH":
        return classes.matchScoreStrong;
      case "GOOD_MATCH":
        return classes.matchScoreGood;
      case "EXPLORE":
      default:
        return classes.matchScoreExplore;
    }
  };

  const getTierCardBorderClass = (tier: RecommendationTier) => {
    switch (tier) {
      case "TOP_MATCH":
        return classes.topMatchBorder;
      case "STRONG_MATCH":
        return classes.strongMatchBorder;
      case "GOOD_MATCH":
        return classes.goodMatchBorder;
      default:
        return "";
    }
  };

  const formatTierLabel = (tier: RecommendationTier) => {
    switch (tier) {
      case "TOP_MATCH":
        return "⭐ Top Pick";
      case "STRONG_MATCH":
        return "🎯 Strong Fit";
      case "GOOD_MATCH":
        return "✨ Good Match";
      case "EXPLORE":
        return "🔍 Explore";
      default:
        return "";
    }
  };

  return (
    <div className={classes.root}>
      {/* Left Sidebar: Filters & Navigation */}
      <aside className={classes.leftSidebar}>
        {activeTab === "ALL" && (
          <div className={classes.filterCard}>
            <h3>Filter Opportunities</h3>
            <form onSubmit={handleSearchSubmit}>
              <div className={classes.filterGroup}>
                <label htmlFor="search">Search Keywords</label>
                <input
                  id="search"
                  type="text"
                  placeholder="Job title, skills, or company"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <div className={classes.filterGroup}>
                <label htmlFor="location">Location</label>
                <input
                  id="location"
                  type="text"
                  placeholder="City, State, or Country"
                  value={locationQuery}
                  onChange={(e) => setLocationQuery(e.target.value)}
                />
              </div>
              <div className={classes.filterGroup}>
                <label htmlFor="employmentType">Employment Type</label>
                <select
                  id="employmentType"
                  value={employmentTypeFilter}
                  onChange={(e) => setEmploymentTypeFilter(e.target.value)}
                >
                  <option value="">All Types</option>
                  <option value="FULL_TIME">Full Time</option>
                  <option value="PART_TIME">Part Time</option>
                  <option value="CONTRACT">Contract</option>
                  <option value="INTERNSHIP">Internship</option>
                </select>
              </div>
              <div className={classes.filterGroup}>
                <label htmlFor="workMode">Workplace Mode</label>
                <select
                  id="workMode"
                  value={workModeFilter}
                  onChange={(e) => setWorkModeFilter(e.target.value)}
                >
                  <option value="">All Workplace Types</option>
                  <option value="ON_SITE">On-site</option>
                  <option value="HYBRID">Hybrid</option>
                  <option value="REMOTE">Remote</option>
                </select>
              </div>
              <Button size="small" type="submit" style={{ width: "100%", marginBottom: "0.5rem" }}>
                Apply Search
              </Button>
              <button type="button" className={classes.clearBtn} onClick={handleClearFilters}>
                Reset Filters
              </button>
            </form>
          </div>
        )}

        <div className={classes.navCard}>
          <h4>Quick Shortcuts</h4>
          <ul>
            <li>
              <Link to="/jobs/my-applications">
                📋 My Job Applications
              </Link>
            </li>
            {user?.role === "ROLE_USER" && (
              <li>
                <Link to={`/profile/${user.id}`}>
                  👤 Manage Profile Signals
                </Link>
              </li>
            )}
            {user?.role === "ROLE_RECRUITER" && (
              <>
                <li>
                  <Link to="/recruiter/jobs">
                    💼 Recruiter Job Board
                  </Link>
                </li>
                <li>
                  <Link to="/recruiter/jobs/create">
                    ➕ Post a New Job
                  </Link>
                </li>
              </>
            )}
            {user?.role === "ROLE_ADMIN" && (
              <li>
                <Link to="/admin/jobs">
                  🛡️ Admin Job Moderation
                </Link>
              </li>
            )}
          </ul>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className={classes.mainContent}>
        <div className={classes.headerBar}>
          <div className={classes.titleSection}>
            <h1>Career Hub</h1>
            <p>
              Discover and match with premier career opportunities tailored to your professional background.
            </p>
          </div>
          <div className={classes.actionBtns}>
            {user?.role === "ROLE_RECRUITER" ? (
              <Button onClick={() => navigate("/recruiter/jobs/create")}>
                + Post a Job
              </Button>
            ) : (
              <Button outline onClick={() => navigate("/jobs/my-applications")}>
                My Applications
              </Button>
            )}
          </div>
        </div>

        {/* View Switcher Tabs */}
        {user?.role === "ROLE_USER" && (
          <div className={classes.tabsContainer}>
            <button
              className={`${classes.tabButton} ${activeTab === "RECOMMENDED" ? classes.activeTab : ""}`}
              onClick={() => setActiveTab("RECOMMENDED")}
            >
              <span>✨ AI Recommended for You</span>
              <span className={classes.tabCountBadge}>{recommendations.length}</span>
            </button>
            <button
              className={`${classes.tabButton} ${activeTab === "ALL" ? classes.activeTab : ""}`}
              onClick={() => setActiveTab("ALL")}
            >
              <span>🔍 All Opportunities</span>
              <span className={classes.tabCountBadge}>{jobs.length}</span>
            </button>
          </div>
        )}

        {/* --- VIEW 1: AI RECOMMENDATIONS --- */}
        {activeTab === "RECOMMENDED" && user?.role === "ROLE_USER" && (
          <>
            {/* AI Context Banner */}
            <div className={classes.aiBanner}>
              <div className={classes.aiBannerHeader}>
                <div className={classes.aiBannerTitle}>
                  <span className={classes.aiSparkleIcon}>✨</span>
                  <h2>AI Job Recommendation Engine</h2>
                </div>
                <button className={classes.refreshBtn} onClick={fetchRecommendations}>
                  🔄 Refresh AI Matches
                </button>
              </div>
              <p className={classes.aiBannerDesc}>
                Open positions ranked using multi-dimensional AI scoring across your verified <strong>Skills</strong>, <strong>Work Experience</strong>, <strong>Professional Headline</strong>, and <strong>Education</strong>.
              </p>

              {/* Match Tier Filter Chips */}
              <div className={classes.aiFilterChips}>
                <button
                  className={`${classes.chip} ${selectedTierFilter === "ALL" ? classes.activeChip : ""}`}
                  onClick={() => setSelectedTierFilter("ALL")}
                >
                  All Matches ({recommendations.length})
                </button>
                <button
                  className={`${classes.chip} ${selectedTierFilter === "TOP_MATCH" ? classes.activeChip : ""}`}
                  onClick={() => setSelectedTierFilter("TOP_MATCH")}
                >
                  ⭐ Top Picks 85%+ ({recommendations.filter((r) => r.recommendationTier === "TOP_MATCH").length})
                </button>
                <button
                  className={`${classes.chip} ${selectedTierFilter === "STRONG_MATCH" ? classes.activeChip : ""}`}
                  onClick={() => setSelectedTierFilter("STRONG_MATCH")}
                >
                  🎯 Strong Fit 70%+ ({recommendations.filter((r) => r.recommendationTier === "STRONG_MATCH").length})
                </button>
                <button
                  className={`${classes.chip} ${selectedTierFilter === "GOOD_MATCH" ? classes.activeChip : ""}`}
                  onClick={() => setSelectedTierFilter("GOOD_MATCH")}
                >
                  ✨ Good Fit 50%+ ({recommendations.filter((r) => r.recommendationTier === "GOOD_MATCH").length})
                </button>
              </div>
            </div>

            {isLoadingRecs ? (
              <Loader isInline />
            ) : filteredRecommendations.length === 0 ? (
              <div className={classes.emptyState}>
                <div className={classes.emptyIcon}>🎯</div>
                <h3>No Recommendations Matching Filter</h3>
                <p>
                  {recommendations.length === 0
                    ? "Add your verified skills, work experience history, and education to your profile to unlock personalized AI recommendations!"
                    : "Try selecting 'All Matches' to view all available recommended opportunities."}
                </p>
                {recommendations.length === 0 ? (
                  <Button onClick={() => navigate(`/profile/${user.id}`)}>
                    Enrich Your Profile
                  </Button>
                ) : (
                  <Button outline onClick={() => setSelectedTierFilter("ALL")}>
                    Show All Matches
                  </Button>
                )}
              </div>
            ) : (
              <div className={classes.jobsList}>
                {filteredRecommendations.map((rec) => {
                  const job = rec.job;
                  return (
                    <div
                      key={job.id}
                      className={`${classes.recommendedCard} ${getTierCardBorderClass(rec.recommendationTier)}`}
                      onClick={() => navigate(`/jobs/${job.id}`)}
                    >
                      <div className={classes.cardHeader}>
                        <div>
                          <h2 className={classes.jobTitle}>{job.title}</h2>
                          <p className={classes.companyName}>{job.company}</p>
                        </div>
                        <div className={classes.badges}>
                          <div className={`${classes.matchScoreBadge} ${getTierBadgeClass(rec.recommendationTier)}`}>
                            <span>{rec.matchPercentage}% Match</span>
                            <span className={classes.scoreTierPill}>{formatTierLabel(rec.recommendationTier)}</span>
                          </div>
                          <span className={`${classes.badge} ${getEmploymentTypeClass(job.employmentType)}`}>
                            {formatEmploymentType(job.employmentType)}
                          </span>
                          <span className={`${classes.badge} ${getWorkModeClass(job.workMode)}`}>
                            {formatWorkMode(job.workMode)}
                          </span>
                        </div>
                      </div>

                      <div className={classes.cardMeta}>
                        <span>📍 {job.location || "Remote"}</span>
                        {job.salary && <span>💰 {job.salary}</span>}
                        {job.minExperience != null && (
                          <span>
                            ⏳ {job.minExperience}
                            {job.maxExperience ? ` - ${job.maxExperience}` : "+"} yrs required (You have {rec.candidateExperienceYears} yrs)
                          </span>
                        )}
                        {job.deadline && (
                          <span>📅 Deadline: {new Date(job.deadline).toLocaleDateString()}</span>
                        )}
                      </div>

                      {/* AI Reasoning Insight Snippet */}
                      {rec.aiReasoning && (
                        <div className={classes.aiInsightBox}>
                          <span className={classes.aiInsightIcon}>💡</span>
                          <div className={classes.aiInsightContent}>
                            <strong>Why this job matches you:</strong> {rec.aiReasoning}
                          </div>
                        </div>
                      )}

                      {/* Match Highlights */}
                      {rec.matchHighlights && rec.matchHighlights.length > 0 && (
                        <ul className={classes.matchHighlightsList}>
                          {rec.matchHighlights.slice(0, 2).map((h, i) => (
                            <li key={i}>✓ {h}</li>
                          ))}
                        </ul>
                      )}

                      {/* Skills Row: Matched vs Missing */}
                      <div className={classes.skillsRow}>
                        {rec.matchedSkills.map((skill, index) => (
                          <span key={`matched-${index}`} className={classes.matchedSkillChip}>
                            ✓ {skill}
                          </span>
                        ))}
                        {rec.missingSkills.map((skill, index) => (
                          <span key={`missing-${index}`} className={classes.missingSkillChip} title="Skill not yet on your profile">
                            + {skill}
                          </span>
                        ))}
                      </div>

                      <div className={classes.cardFooter}>
                        <div className={classes.footerLeft}>
                          <span className={classes.applicantsCount}>
                            👥 {job.applicantCount || 0} {job.applicantCount === 1 ? "applicant" : "applicants"}
                          </span>
                        </div>
                        <div className={classes.footerRight}>
                          <button
                            type="button"
                            className={classes.breakdownBtn}
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedBreakdown(rec);
                            }}
                          >
                            📊 View AI Breakdown
                          </button>
                          {job.hasApplied ? (
                            <span className={classes.appliedTag}>✓ Applied</span>
                          ) : job.expired ? (
                            <span className={classes.expiredTag}>Closed / Expired</span>
                          ) : (
                            <Button
                              size="small"
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/jobs/${job.id}`);
                              }}
                            >
                              View & Apply
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </>
        )}

        {/* --- VIEW 2: ALL OPPORTUNITIES --- */}
        {(activeTab === "ALL" || user?.role !== "ROLE_USER") && (
          <>
            {isLoadingJobs ? (
              <Loader isInline />
            ) : filteredJobs.length === 0 ? (
              <div className={classes.emptyState}>
                <div className={classes.emptyIcon}>🔍</div>
                <h3>No Jobs Found</h3>
                <p>Try adjusting your search keywords or workplace filters to view more opportunities.</p>
                <Button outline onClick={handleClearFilters}>
                  Clear All Filters
                </Button>
              </div>
            ) : (
              <div className={classes.jobsList}>
                {filteredJobs.map((job) => (
                  <div
                    key={job.id}
                    className={classes.jobCard}
                    onClick={() => navigate(`/jobs/${job.id}`)}
                  >
                    <div className={classes.cardHeader}>
                      <div>
                        <h2 className={classes.jobTitle}>{job.title}</h2>
                        <p className={classes.companyName}>{job.company}</p>
                      </div>
                      <div className={classes.badges}>
                        <span className={`${classes.badge} ${getEmploymentTypeClass(job.employmentType)}`}>
                          {formatEmploymentType(job.employmentType)}
                        </span>
                        <span className={`${classes.badge} ${getWorkModeClass(job.workMode)}`}>
                          {formatWorkMode(job.workMode)}
                        </span>
                      </div>
                    </div>

                    <div className={classes.cardMeta}>
                      <span>📍 {job.location || "Remote"}</span>
                      {job.salary && <span>💰 {job.salary}</span>}
                      {job.minExperience != null && (
                        <span>
                          ⏳ {job.minExperience}
                          {job.maxExperience ? ` - ${job.maxExperience}` : "+"} yrs exp
                        </span>
                      )}
                      {job.deadline && (
                        <span>📅 Deadline: {new Date(job.deadline).toLocaleDateString()}</span>
                      )}
                    </div>

                    {job.requiredSkills && job.requiredSkills.length > 0 && (
                      <div className={classes.skillsRow}>
                        {job.requiredSkills.slice(0, 5).map((skill, index) => (
                          <span key={index} className={classes.skillChip}>
                            {skill}
                          </span>
                        ))}
                        {job.requiredSkills.length > 5 && (
                          <span className={classes.skillChip}>
                            +{job.requiredSkills.length - 5} more
                          </span>
                        )}
                      </div>
                    )}

                    <div className={classes.cardFooter}>
                      <span className={classes.applicantsCount}>
                        👥 {job.applicantCount || 0} {job.applicantCount === 1 ? "applicant" : "applicants"}
                      </span>
                      {job.hasApplied ? (
                        <span className={classes.appliedTag}>✓ Applied</span>
                      ) : job.expired ? (
                        <span className={classes.expiredTag}>Closed / Expired</span>
                      ) : (
                        <Button
                          size="small"
                          onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/jobs/${job.id}`);
                          }}
                        >
                          View & Apply
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {/* --- AI SCORE BREAKDOWN MODAL --- */}
        {selectedBreakdown && (
          <div className={classes.modalOverlay} onClick={() => setSelectedBreakdown(null)}>
            <div className={classes.modalContent} onClick={(e) => e.stopPropagation()}>
              <div className={classes.modalHeader}>
                <div className={classes.modalTitle}>
                  <h3>
                    <span>✨</span> AI Match Compatibility Report
                  </h3>
                  <p>
                    {selectedBreakdown.job.title} at {selectedBreakdown.job.company}
                  </p>
                </div>
                <button
                  type="button"
                  className={classes.closeBtn}
                  onClick={() => setSelectedBreakdown(null)}
                >
                  ✕
                </button>
              </div>

              <div className={classes.modalBody}>
                {/* Overall Score Card */}
                <div className={classes.overallScoreCard}>
                  <div className={classes.scoreCircle}>
                    <span className={classes.scoreNum}>{selectedBreakdown.matchPercentage}%</span>
                    <span className={classes.scoreText}>Match</span>
                  </div>
                  <div className={classes.scoreDetails}>
                    <h4>{formatTierLabel(selectedBreakdown.recommendationTier)} Compatibility</h4>
                    <p>{selectedBreakdown.aiReasoning}</p>
                  </div>
                </div>

                {/* 4 Multi-Dimensional Progress Bars */}
                <div>
                  <h4 className={classes.modalSectionTitle}>Multi-Dimensional Fit Analysis</h4>
                  <div className={classes.progressBarsContainer}>
                    <div className={classes.progressRow}>
                      <div className={classes.progressHeader}>
                        <div>
                          <span className={classes.labelName}>Skills Compatibility</span>
                          <span className={classes.labelWeight}>(40% Weight)</span>
                        </div>
                        <span className={classes.percentageVal}>
                          {selectedBreakdown.scoreBreakdown.skills}%
                        </span>
                      </div>
                      <div className={classes.progressBarBg}>
                        <div
                          className={`${classes.progressBarFill} ${classes.fillSkills}`}
                          style={{ width: `${selectedBreakdown.scoreBreakdown.skills}%` }}
                        />
                      </div>
                    </div>

                    <div className={classes.progressRow}>
                      <div className={classes.progressHeader}>
                        <div>
                          <span className={classes.labelName}>Work Experience Fit</span>
                          <span className={classes.labelWeight}>(25% Weight)</span>
                        </div>
                        <span className={classes.percentageVal}>
                          {selectedBreakdown.scoreBreakdown.experience}%
                        </span>
                      </div>
                      <div className={classes.progressBarBg}>
                        <div
                          className={`${classes.progressBarFill} ${classes.fillExp}`}
                          style={{ width: `${selectedBreakdown.scoreBreakdown.experience}%` }}
                        />
                      </div>
                    </div>

                    <div className={classes.progressRow}>
                      <div className={classes.progressHeader}>
                        <div>
                          <span className={classes.labelName}>Role & Profile Alignment</span>
                          <span className={classes.labelWeight}>(20% Weight)</span>
                        </div>
                        <span className={classes.percentageVal}>
                          {selectedBreakdown.scoreBreakdown.profile}%
                        </span>
                      </div>
                      <div className={classes.progressBarBg}>
                        <div
                          className={`${classes.progressBarFill} ${classes.fillProfile}`}
                          style={{ width: `${selectedBreakdown.scoreBreakdown.profile}%` }}
                        />
                      </div>
                    </div>

                    <div className={classes.progressRow}>
                      <div className={classes.progressHeader}>
                        <div>
                          <span className={classes.labelName}>Education Alignment</span>
                          <span className={classes.labelWeight}>(15% Weight)</span>
                        </div>
                        <span className={classes.percentageVal}>
                          {selectedBreakdown.scoreBreakdown.education}%
                        </span>
                      </div>
                      <div className={classes.progressBarBg}>
                        <div
                          className={`${classes.progressBarFill} ${classes.fillEdu}`}
                          style={{ width: `${selectedBreakdown.scoreBreakdown.education}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* Key Highlights Checklist */}
                <div>
                  <h4 className={classes.modalSectionTitle}>Match Strengths & Insights</h4>
                  <div className={classes.breakdownHighlights}>
                    <ul>
                      {selectedBreakdown.matchHighlights.map((highlight, idx) => (
                        <li key={idx}>✓ {highlight}</li>
                      ))}
                    </ul>
                  </div>
                </div>

                {/* Skills Comparison */}
                <div>
                  <h4 className={classes.modalSectionTitle}>Skills Breakdown</h4>
                  <div className={classes.skillsRow}>
                    {selectedBreakdown.matchedSkills.map((skill, idx) => (
                      <span key={`matched-modal-${idx}`} className={classes.matchedSkillChip}>
                        ✓ {skill} (Matched)
                      </span>
                    ))}
                    {selectedBreakdown.missingSkills.map((skill, idx) => (
                      <span key={`missing-modal-${idx}`} className={classes.missingSkillChip}>
                        + {skill} (Requested)
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <div className={classes.modalFooter}>
                <Button outline size="small" onClick={() => setSelectedBreakdown(null)}>
                  Close
                </Button>
                <Button
                  size="small"
                  onClick={() => {
                    navigate(`/jobs/${selectedBreakdown.job.id}`);
                  }}
                >
                  View Full Job
                </Button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
