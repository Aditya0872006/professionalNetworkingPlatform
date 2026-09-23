import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { IJob } from "../../types";
import classes from "./Jobs.module.scss";

export function Jobs() {
  usePageTitle("Jobs | LinkedIn");
  const { user } = useAuthentication();
  const navigate = useNavigate();

  const [jobs, setJobs] = useState<IJob[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [locationQuery, setLocationQuery] = useState("");
  const [employmentTypeFilter, setEmploymentTypeFilter] = useState("");
  const [workModeFilter, setWorkModeFilter] = useState("");

  const fetchJobs = async () => {
    setIsLoading(true);
    let url = "/api/v1/jobs";
    const params = new URLSearchParams();
    if (searchQuery.trim()) params.append("query", searchQuery.trim());
    if (locationQuery.trim()) params.append("location", locationQuery.trim());
    const queryString = params.toString();
    if (queryString) url += `?${queryString}`;

    await request<IJob[]>({
      endpoint: url,
      onSuccess: (data) => setJobs(data),
      onFailure: (err) => console.error("Error fetching jobs:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchJobs();
  }, []);

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
      onSuccess: (data) => setJobs(data),
      onFailure: (err) => console.error(err),
    });
  };

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

  return (
    <div className={classes.root}>
      {/* Left Sidebar: Filters & Navigation */}
      <aside className={classes.leftSidebar}>
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

        <div className={classes.navCard}>
          <h4>Quick Shortcuts</h4>
          <ul>
            <li>
              <Link to="/jobs/my-applications">
                📋 My Job Applications
              </Link>
            </li>
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

      {/* Main Content: Job Listings */}
      <main className={classes.mainContent}>
        <div className={classes.headerBar}>
          <div className={classes.titleSection}>
            <h1>Explore Career Opportunities</h1>
            <p>
              {filteredJobs.length} {filteredJobs.length === 1 ? "opening" : "openings"} available matching your criteria
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

        {isLoading ? (
          <Loader isInline />
        ) : filteredJobs.length === 0 ? (
          <div className={classes.emptyState}>
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
                    <span>
                      📅 Deadline: {new Date(job.deadline).toLocaleDateString()}
                    </span>
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
      </main>
    </div>
  );
}
