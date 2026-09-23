import { NavLink } from "react-router-dom";
import classes from "./AdminHeader.module.scss";

interface IAdminHeaderProps {
  title?: string;
}

export function AdminHeader({ title = "Platform Administration" }: IAdminHeaderProps) {
  return (
    <div className={classes.root}>
      <div className={classes.topRow}>
        <div className={classes.titleSection}>
          <span className={classes.badge}>🛡️ Admin Control Panel</span>
          <h1>{title}</h1>
        </div>
      </div>

      <nav className={classes.navTabs}>
        <NavLink
          to="/admin"
          end
          className={({ isActive }) => `${classes.tab} ${isActive ? classes.active : ""}`}
        >
          📊 Dashboard Overview
        </NavLink>
        <NavLink
          to="/admin/users"
          className={({ isActive }) => `${classes.tab} ${isActive ? classes.active : ""}`}
        >
          👥 User Management
        </NavLink>
        <NavLink
          to="/admin/recruiters"
          className={({ isActive }) => `${classes.tab} ${isActive ? classes.active : ""}`}
        >
          💼 Recruiter Applications
        </NavLink>
        <NavLink
          to="/admin/jobs"
          className={({ isActive }) => `${classes.tab} ${isActive ? classes.active : ""}`}
        >
          📋 Job Moderation
        </NavLink>
        <NavLink
          to="/admin/posts"
          className={({ isActive }) => `${classes.tab} ${isActive ? classes.active : ""}`}
        >
          💬 Feed Moderation
        </NavLink>
      </nav>
    </div>
  );
}
