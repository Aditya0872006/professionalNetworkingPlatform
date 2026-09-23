import { useEffect, useState } from "react";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { AdminHeader } from "../../components/AdminHeader/AdminHeader";
import { IRecruiterProfile, RecruiterStatus } from "../../types";
import classes from "./AdminRecruiters.module.scss";

export function AdminRecruiters() {
  usePageTitle("Admin | Recruiter Applications");
  const [recruiters, setRecruiters] = useState<IRecruiterProfile[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("PENDING");
  const [feedback, setFeedback] = useState("");

  const fetchRecruiters = async () => {
    setIsLoading(true);
    let url = "/api/v1/admin/recruiters";
    if (statusFilter) {
      url += `?status=${statusFilter}`;
    }

    await request<IRecruiterProfile[]>({
      endpoint: url,
      onSuccess: (data) => setRecruiters(data),
      onFailure: (err) => console.error("Error fetching recruiter applications:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchRecruiters();
  }, [statusFilter]);

  const handleApprove = async (profileId: number) => {
    await request({
      endpoint: `/api/v1/admin/recruiters/${profileId}/approve`,
      method: "PUT",
      onSuccess: () => {
        setRecruiters((prev) =>
          prev.map((r) =>
            r.id === profileId ? { ...r, status: "APPROVED" as RecruiterStatus } : r
          )
        );
        setFeedback("Recruiter application approved. Recruiter has been notified.");
        setTimeout(() => setFeedback(""), 4000);
      },
      onFailure: (err) => alert(err),
    });
  };

  const handleReject = async (profileId: number) => {
    if (!window.confirm("Are you sure you want to reject this recruiter application?")) return;
    await request({
      endpoint: `/api/v1/admin/recruiters/${profileId}/reject`,
      method: "PUT",
      onSuccess: () => {
        setRecruiters((prev) =>
          prev.map((r) => {
            if (r.id === profileId) {
              const newCount = r.rejectionCount + 1;
              const newStatus: RecruiterStatus =
                newCount >= 5 ? "PERMANENTLY_REJECTED" : "REJECTED";
              return { ...r, status: newStatus, rejectionCount: newCount };
            }
            return r;
          })
        );
        setFeedback("Recruiter application rejected. Rejection recorded.");
        setTimeout(() => setFeedback(""), 4000);
      },
      onFailure: (err) => alert(err),
    });
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case "APPROVED":
        return classes.approved;
      case "REJECTED":
        return classes.rejected;
      case "PERMANENTLY_REJECTED":
        return classes.permanentlyRejected;
      case "PENDING":
      default:
        return classes.pending;
    }
  };

  return (
    <div className={classes.root}>
      <AdminHeader title="Recruiter Applications Review" />

      {feedback && (
        <div style={{ background: "#e8f5e9", color: "#2e7d32", padding: "0.75rem 1rem", borderRadius: "6px", marginBottom: "1rem" }}>
          ✓ {feedback}
        </div>
      )}

      <div className={classes.controlsCard}>
        <div className={classes.filterGroup}>
          <label htmlFor="status">Application Status:</label>
          <select
            id="status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">All Applications</option>
            <option value="PENDING">Pending Review (Action Required)</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="PERMANENTLY_REJECTED">Permanently Rejected (5/5)</option>
          </select>
        </div>

        <div style={{ fontSize: "0.85rem", color: "#666" }}>
          Total records: <strong>{recruiters.length}</strong>
        </div>
      </div>

      {isLoading ? (
        <Loader isInline />
      ) : recruiters.length === 0 ? (
        <div className={classes.emptyState}>
          <h3>No Recruiter Applications</h3>
          <p>No applications match the selected status filter.</p>
        </div>
      ) : (
        <div className={classes.recruitersList}>
          {recruiters.map((r) => (
            <div key={r.id} className={classes.recruiterCard}>
              <div className={classes.infoSection}>
                <h3 className={classes.companyTitle}>{r.companyName}</h3>
                <div className={classes.recruiterName}>
                  👤 {r.user.firstName} {r.user.lastName} ({r.user.email})
                </div>

                <div className={classes.metaRow}>
                  {r.companyLocation && <span>📍 {r.companyLocation}</span>}
                  {r.companyEmail && <span>📧 {r.companyEmail}</span>}
                  {r.companyWebsite && (
                    <a href={r.companyWebsite} target="_blank" rel="noopener noreferrer">
                      🌐 {r.companyWebsite}
                    </a>
                  )}
                  <span>📅 Applied: {new Date(r.createdAt).toLocaleDateString()}</span>
                </div>

                {r.companyDescription && (
                  <div className={classes.description}>
                    <strong>About Company:</strong> {r.companyDescription}
                  </div>
                )}
              </div>

              <div className={classes.statusSection}>
                <span className={`${classes.statusBadge} ${getStatusBadgeClass(r.status)}`}>
                  {r.status.replace(/_/g, " ")}
                </span>

                <span className={classes.rejectionCounter}>
                  Rejections: <strong>{r.rejectionCount} / 5</strong>
                </span>

                {r.status === "PENDING" && (
                  <div className={classes.actions}>
                    <button
                      className={classes.approveBtn}
                      onClick={() => handleApprove(r.id)}
                    >
                      ✓ Approve
                    </button>
                    <button
                      className={classes.rejectBtn}
                      onClick={() => handleReject(r.id)}
                    >
                      ✗ Reject
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
