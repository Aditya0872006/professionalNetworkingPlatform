import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import { ApplicationLayout } from "./components/ApplicationLayout/ApplicationLayout";
import { AuthenticationLayout } from "./features/authentication/components/AuthenticationLayout/AuthenticationLayout";
import { AuthenticationContextProvider } from "./features/authentication/contexts/AuthenticationContextProvider";
import { Login } from "./features/authentication/pages/Login/Login";
import { Profile as LoginProfile } from "./features/authentication/pages/Profile/Profile";
import { ResetPassword } from "./features/authentication/pages/ResetPassword/ResetPassword";
import { Signup } from "./features/authentication/pages/Signup/Signup";
import { VerifyEmail } from "./features/authentication/pages/VerifyEmail/VerifyEmail";
import { Feed } from "./features/feed/pages/Feed/Feed";
import { Notifications } from "./features/feed/pages/Notifications/Notifications";
import { PostPage } from "./features/feed/pages/Post/Post";
import { CreateGroup } from "./features/groups/pages/CreateGroup/CreateGroup";
import { GroupDetail } from "./features/groups/pages/GroupDetail/GroupDetail";
import { Groups } from "./features/groups/pages/Groups/Groups";
import { Conversation } from "./features/messaging/pages/Conversation/Conversation";
import { Messaging } from "./features/messaging/pages/Messages/Messaging";
import { Connections } from "./features/networking/pages/Connections/Connections";
import { Invitations } from "./features/networking/pages/Invitations/Invitations";
import { Network } from "./features/networking/pages/Network/Network";
import { Posts } from "./features/profile/pages/Posts/Posts";
import { Profile } from "./features/profile/pages/Profile/Profile";

// Jobs Module
import { Jobs } from "./features/jobs/pages/Jobs/Jobs";
import { JobDetail } from "./features/jobs/pages/JobDetail/JobDetail";
import { MyApplications } from "./features/jobs/pages/MyApplications/MyApplications";
import { RecruiterJobs } from "./features/jobs/pages/RecruiterJobs/RecruiterJobs";
import { PostJob } from "./features/jobs/pages/PostJob/PostJob";
import { JobApplicants } from "./features/jobs/pages/JobApplicants/JobApplicants";

// Admin Module
import { AdminDashboard } from "./features/admin/pages/AdminDashboard/AdminDashboard";
import { AdminUsers } from "./features/admin/pages/AdminUsers/AdminUsers";
import { AdminRecruiters } from "./features/admin/pages/AdminRecruiters/AdminRecruiters";
import { AdminJobs } from "./features/admin/pages/AdminJobs/AdminJobs";
import { AdminPosts } from "./features/admin/pages/AdminPosts/AdminPosts";

import "./index.scss";

const router = createBrowserRouter([
  {
    element: <AuthenticationContextProvider />,
    children: [
      {
        path: "/",
        element: <ApplicationLayout />,
        children: [
          {
            index: true,
            element: <Feed />,
          },
          {
            path: "posts/:id",
            element: <PostPage />,
          },
          {
            path: "network",
            element: <Network />,
            children: [
              {
                index: true,
                element: <Navigate to="invitations" />,
              },
              {
                path: "invitations",
                element: <Invitations />,
              },
              {
                path: "connections",
                element: <Connections />,
              },
            ],
          },
          {
            path: "messaging",
            element: <Messaging />,
            children: [
              {
                path: "conversations/:id",
                element: <Conversation />,
              },
            ],
          },
          {
            path: "notifications",
            element: <Notifications />,
          },
          {
            path: "profile/:id",
            element: <Profile />,
          },
          {
            path: "profile/:id/posts",
            element: <Posts />,
          },
          {
            path: "groups",
            element: <Groups />,
          },
          {
            path: "groups/create",
            element: <CreateGroup />,
          },
          {
            path: "groups/:id",
            element: <GroupDetail />,
          },

          // Jobs Module Routes
          {
            path: "jobs",
            element: <Jobs />,
          },
          {
            path: "jobs/:id",
            element: <JobDetail />,
          },
          {
            path: "jobs/my-applications",
            element: <MyApplications />,
          },
          {
            path: "recruiter/jobs",
            element: <RecruiterJobs />,
          },
          {
            path: "recruiter/jobs/create",
            element: <PostJob />,
          },
          {
            path: "recruiter/jobs/:jobId/applicants",
            element: <JobApplicants />,
          },

          // Admin Module Routes
          {
            path: "admin",
            element: <AdminDashboard />,
          },
          {
            path: "admin/users",
            element: <AdminUsers />,
          },
          {
            path: "admin/recruiters",
            element: <AdminRecruiters />,
          },
          {
            path: "admin/jobs",
            element: <AdminJobs />,
          },
          {
            path: "admin/posts",
            element: <AdminPosts />,
          },
        ],
      },
      {
        path: "/authentication",
        element: <AuthenticationLayout />,
        children: [
          {
            path: "login",
            element: <Login />,
          },
          {
            path: "signup",
            element: <Signup />,
          },
          {
            path: "request-password-reset",
            element: <ResetPassword />,
          },
          {
            path: "verify-email",
            element: <VerifyEmail />,
          },
          {
            path: "profile/:id",
            element: <LoginProfile />,
          },
        ],
      },
      {
        path: "*",
        element: <Navigate to="/" />,
      },
    ],
  },
]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>
);
