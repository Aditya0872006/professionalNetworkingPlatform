import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { AdminHeader } from "../../components/AdminHeader/AdminHeader";
import { IAdminStats } from "../../types";
import classes from "./AdminDashboard.module.scss";

export function AdminDashboard() {
  usePageTitle("Admin Dashboard | Overview");
  const [stats, setStats] = useState<IAdminStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      setIsLoading(true);
      await request<IAdminStats>({
        endpoint: "/api/v1/admin/stats",
        onSuccess: (data) => setStats(data),
        onFailure: (err) => console.error("Error fetching admin stats:", err),
      });
      setIsLoading(false);
    };

    fetchStats();
  }, []);

  return (
    <div className={classes.root}>
      <AdminHeader title="Platform Analytics & Overview" />

      {isLoading ? (
        <Loader isInline />
      ) : stats ? (
        <>
          <div className={classes.statsGrid}>
            <div className={classes.statCard}>
              <div className={`${classes.iconBox} ${classes.blue}`}>👥</div>
              <div className={classes.statDetails}>
                <div className={classes.statNumber}>{stats.totalUsers.toLocaleString()}</div>
                <div className={classes.statLabel}>Active Users</div>
              </div>
            </div>

            <div className={classes.statCard}>
              <div className={`${classes.iconBox} ${classes.teal}`}>💼</div>
              <div className={classes.statDetails}>
                <div className={classes.statNumber}>{stats.totalRecruiters.toLocaleString()}</div>
                <div className={classes.statLabel}>Total Recruiters</div>
              </div>
            </div>

            <div className={classes.statCard}>
              <div className={`${classes.iconBox} ${classes.amber}`}>⏳</div>
              <div className={classes.statDetails}>
                <div className={classes.statNumber}>{stats.pendingRecruiterRequests.toLocaleString()}</div>
                <div className={classes.statLabel}>Pending Recruiter Reviews</div>
              </div>
            </div>

            <div className={classes.statCard}>
              <div className={`${classes.iconBox} ${classes.purple}`}>📋</div>
              <div className={classes.statDetails}>
                <div className={classes.statNumber}>{stats.totalJobs.toLocaleString()}</div>
                <div className={classes.statLabel}>Platform Jobs</div>
              </div>
            </div>

            <div className={classes.statCard}>
              <div className={`${classes.iconBox} ${classes.green}`}>💬</div>
              <div className={classes.statDetails}>
                <div className={classes.statNumber}>{stats.totalPosts.toLocaleString()}</div>
                <div className={classes.statLabel}>Feed Posts</div>
              </div>
            </div>

            <div className={classes.statCard}>
              <div className={`${classes.iconBox} ${classes.red}`}>🚫</div>
              <div className={classes.statDetails}>
                <div className={classes.statNumber}>{stats.blockedUsers.toLocaleString()}</div>
                <div className={classes.statLabel}>Blocked Accounts</div>
              </div>
            </div>
          </div>

          <div className={classes.quickActionsSection}>
            <h2>Administrative Operations</h2>
            <div className={classes.actionCards}>
              <Link to="/admin/users" className={classes.actionCard}>
                <h3>👥 Manage Users</h3>
                <p>Search, filter, and inspect user profiles or block/unblock accounts.</p>
              </Link>

              <Link to="/admin/recruiters" className={classes.actionCard}>
                <h3>💼 Recruiter Applications</h3>
                <p>Review incoming recruiter requests, approve or reject applications.</p>
              </Link>

              <Link to="/admin/jobs" className={classes.actionCard}>
                <h3>📋 Moderate Jobs</h3>
                <p>Audit posted openings and take down policy-violating listings.</p>
              </Link>

              <Link to="/admin/posts" className={classes.actionCard}>
                <h3>💬 Moderate Feed Posts</h3>
                <p>Inspect community feed content and delete violating publications.</p>
              </Link>
            </div>
          </div>
        </>
      ) : (
        <p>Failed to load administrative analytics.</p>
      )}
    </div>
  );
}
