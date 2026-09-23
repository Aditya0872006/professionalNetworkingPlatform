import { useEffect, useState } from "react";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { IUser } from "../../../authentication/contexts/AuthenticationContextProvider";
import { AdminHeader } from "../../components/AdminHeader/AdminHeader";
import classes from "./AdminUsers.module.scss";

export function AdminUsers() {
  usePageTitle("Admin | User Management");
  const [users, setUsers] = useState<IUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [feedback, setFeedback] = useState("");

  const fetchUsers = async () => {
    setIsLoading(true);
    let url = "/api/v1/admin/users";
    const params = new URLSearchParams();
    if (roleFilter) params.append("role", roleFilter);
    if (statusFilter) params.append("status", statusFilter);
    const qs = params.toString();
    if (qs) url += `?${qs}`;

    await request<IUser[]>({
      endpoint: url,
      onSuccess: (data) => setUsers(data),
      onFailure: (err) => console.error("Error fetching admin users:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchUsers();
  }, [roleFilter, statusFilter]);

  const handleBlockUser = async (userId: string) => {
    if (!window.confirm("Are you sure you want to block this user account?")) return;
    await request({
      endpoint: `/api/v1/admin/users/${userId}/block`,
      method: "PUT",
      onSuccess: () => {
        setUsers((prev) =>
          prev.map((u) => (u.id === userId ? { ...u, status: "BLOCKED" } : u))
        );
        setFeedback("User has been blocked successfully.");
        setTimeout(() => setFeedback(""), 3500);
      },
      onFailure: (err) => alert(err),
    });
  };

  const handleUnblockUser = async (userId: string) => {
    await request({
      endpoint: `/api/v1/admin/users/${userId}/unblock`,
      method: "PUT",
      onSuccess: () => {
        setUsers((prev) =>
          prev.map((u) => (u.id === userId ? { ...u, status: "ACTIVE" } : u))
        );
        setFeedback("User has been unblocked successfully.");
        setTimeout(() => setFeedback(""), 3500);
      },
      onFailure: (err) => alert(err),
    });
  };

  const getRoleBadgeClass = (role?: string) => {
    switch (role) {
      case "ROLE_ADMIN":
        return classes.admin;
      case "ROLE_RECRUITER":
        return classes.recruiter;
      case "ROLE_USER":
      default:
        return classes.user;
    }
  };

  const formatRole = (role?: string) => {
    if (!role) return "User";
    return role.replace("ROLE_", "").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  };

  return (
    <div className={classes.root}>
      <AdminHeader title="User Directory & Access Control" />

      {feedback && (
        <div style={{ background: "#e8f5e9", color: "#2e7d32", padding: "0.75rem 1rem", borderRadius: "6px", marginBottom: "1rem" }}>
          ✓ {feedback}
        </div>
      )}

      <div className={classes.controlsCard}>
        <div className={classes.filters}>
          <div className={classes.filterGroup}>
            <label htmlFor="role">Role Filter:</label>
            <select
              id="role"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
            >
              <option value="">All Roles</option>
              <option value="ROLE_USER">Normal Users</option>
              <option value="ROLE_RECRUITER">Recruiters</option>
              <option value="ROLE_ADMIN">Administrators</option>
            </select>
          </div>

          <div className={classes.filterGroup}>
            <label htmlFor="status">Status Filter:</label>
            <select
              id="status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="BLOCKED">Blocked</option>
            </select>
          </div>
        </div>

        <div style={{ fontSize: "0.85rem", color: "#666" }}>
          Total displayed: <strong>{users.length}</strong>
        </div>
      </div>

      {isLoading ? (
        <Loader isInline />
      ) : (
        <div className={classes.usersTableWrapper}>
          <table>
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Location</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>
                    <div className={classes.userCell}>
                      <img
                        className={classes.avatar}
                        src={
                          u.profilePicture
                            ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${u.profilePicture}`
                            : "/avatar.svg"
                        }
                        alt=""
                      />
                      <div>
                        <div className={classes.name}>
                          {u.firstName || u.lastName
                            ? `${u.firstName || ""} ${u.lastName || ""}`.trim()
                            : "New User"}
                        </div>
                        {u.position && <div className={classes.headline}>{u.position}</div>}
                      </div>
                    </div>
                  </td>
                  <td>{u.email}</td>
                  <td>
                    <span className={`${classes.roleBadge} ${getRoleBadgeClass(u.role)}`}>
                      {formatRole(u.role)}
                    </span>
                  </td>
                  <td>
                    <span
                      className={`${classes.statusBadge} ${
                        u.status === "BLOCKED" ? classes.blocked : classes.active
                      }`}
                    >
                      {u.status || "ACTIVE"}
                    </span>
                  </td>
                  <td>{u.location || "-"}</td>
                  <td>
                    {u.role === "ROLE_ADMIN" ? (
                      <span style={{ fontSize: "0.75rem", color: "#999" }}>Admin Protected</span>
                    ) : u.status === "BLOCKED" ? (
                      <button
                        className={`${classes.actionBtn} ${classes.unblock}`}
                        onClick={() => handleUnblockUser(u.id)}
                      >
                        Unblock
                      </button>
                    ) : (
                      <button
                        className={`${classes.actionBtn} ${classes.block}`}
                        onClick={() => handleBlockUser(u.id)}
                      >
                        Block User
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", padding: "2rem", color: "#666" }}>
                    No users matching the selected filters.
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
