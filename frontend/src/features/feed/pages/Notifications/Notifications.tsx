import { Dispatch, SetStateAction, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import {
  IUser,
  useAuthentication,
} from "../../../authentication/contexts/AuthenticationContextProvider";
import { LeftSidebar } from "../../components/LeftSidebar/LeftSidebar";
import { RightSidebar } from "../../components/RightSidebar/RightSidebar";
import { TimeAgo } from "../../components/TimeAgo/TimeAgo";
import classes from "./Notifications.module.scss";

enum NotificationType {
  LIKE = "LIKE",
  COMMENT = "COMMENT",
  GROUP_JOIN_REQUEST = "GROUP_JOIN_REQUEST",
  GROUP_JOIN_ACCEPTED = "GROUP_JOIN_ACCEPTED",
  RECRUITER_APPROVED = "RECRUITER_APPROVED",
  RECRUITER_REJECTED = "RECRUITER_REJECTED",
  RECRUITER_PERMANENTLY_REJECTED = "RECRUITER_PERMANENTLY_REJECTED",
  JOB_REMOVED = "JOB_REMOVED",
  POST_REMOVED = "POST_REMOVED",
  JOB_RECOMMENDATION = "JOB_RECOMMENDATION",
  APPLICATION_STATUS = "APPLICATION_STATUS",
  USER_BLOCKED = "USER_BLOCKED",
  USER_UNBLOCKED = "USER_UNBLOCKED",
}

export interface INotification {
  id: number;
  recipient: IUser;
  actor?: IUser | null;
  read: boolean;
  type: NotificationType | string;
  resourceId?: number;
  message?: string;
  creationDate: string;
}

export function Notifications() {
  usePageTitle("Notifications");
  const [notifications, setNotifications] = useState<INotification[]>([]);
  const { user } = useAuthentication();
  useEffect(() => {
    const fetchNotifications = async () => {
      await request<INotification[]>({
        endpoint: "/api/v1/notifications",
        onSuccess: setNotifications,
        onFailure: (error) => console.log(error),
      });
    };

    fetchNotifications();
  }, []);

  return (
    <div className={classes.root}>
      <div className={classes.left}>
        <LeftSidebar user={user} />
      </div>
      <div className={classes.center}>
        {notifications.map((notification) => (
          <Notification
            key={notification.id}
            notification={notification}
            setNotifications={setNotifications}
          />
        ))}
        {notifications.length === 0 && (
          <p
            style={{
              padding: "1rem",
            }}
          >
            No notifications
          </p>
        )}
      </div>
      <div className={classes.right}>
        <RightSidebar />
      </div>
    </div>
  );
}

function Notification({
  notification,
  setNotifications,
}: {
  notification: INotification;
  setNotifications: Dispatch<SetStateAction<INotification[]>>;
}) {
  const navigate = useNavigate();

  function markNotificationAsRead(notificationId: number) {
    request({
      endpoint: `/api/v1/notifications/${notificationId}`,
      method: "PUT",
      onSuccess: () => {
        setNotifications((prev) =>
          prev.map((n) =>
            n.id === notificationId ? { ...n, read: true } : n
          )
        );
      },
      onFailure: (error) => console.log(error),
    });
  }

  const handleClick = () => {
    markNotificationAsRead(notification.id);

    if (
      notification.type === NotificationType.GROUP_JOIN_REQUEST ||
      notification.type === NotificationType.GROUP_JOIN_ACCEPTED
    ) {
      if (notification.resourceId) navigate(`/groups/${notification.resourceId}`);
    } else if (
      notification.type === NotificationType.JOB_RECOMMENDATION ||
      notification.type === "JOB_RECOMMENDATION"
    ) {
      if (notification.resourceId) navigate(`/jobs/${notification.resourceId}`);
      else navigate("/jobs");
    } else if (
      notification.type === NotificationType.APPLICATION_STATUS ||
      notification.type === "APPLICATION_STATUS"
    ) {
      navigate("/jobs/my-applications");
    } else if (
      notification.type === NotificationType.RECRUITER_APPROVED ||
      notification.type === "RECRUITER_APPROVED"
    ) {
      navigate("/recruiter/jobs");
    } else if (
      notification.type === NotificationType.JOB_REMOVED ||
      notification.type === NotificationType.POST_REMOVED ||
      notification.type === NotificationType.USER_BLOCKED ||
      notification.type === NotificationType.USER_UNBLOCKED
    ) {
      // Informational notification — no redirect needed
    } else if (notification.resourceId) {
      navigate(`/posts/${notification.resourceId}`);
    }
  };

  const actorName = notification.actor
    ? `${notification.actor.firstName || ""} ${notification.actor.lastName || ""}`.trim()
    : "System";

  const avatarSrc = notification.actor?.profilePicture
    ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${notification.actor.profilePicture}`
    : "/avatar.svg";

  return (
    <button
      onClick={handleClick}
      className={
        notification.read ? classes.notification : `${classes.notification} ${classes.unread}`
      }
    >
      <img
        src={avatarSrc}
        alt=""
        className={classes.avatar}
      />

      <p
        style={{
          marginRight: "auto",
          textAlign: "left",
        }}
      >
        {notification.message ? (
          notification.message
        ) : (
          <>
            <strong>{actorName}</strong>{" "}
            {notification.type === NotificationType.LIKE
              ? "liked your post."
              : notification.type === NotificationType.COMMENT
              ? "commented on your post."
              : notification.type === NotificationType.GROUP_JOIN_REQUEST
              ? "requested to join your group."
              : notification.type === NotificationType.GROUP_JOIN_ACCEPTED
              ? "accepted your request to join the group."
              : "sent a notification."}
          </>
        )}
      </p>
      <TimeAgo date={notification.creationDate} />
    </button>
  );
}
