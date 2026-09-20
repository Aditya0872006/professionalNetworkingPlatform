import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { request } from "../../../../utils/api";
import { IGroupDetails, IGroupMember } from "../../types/groups";
import classes from "./GroupCard.module.scss";

interface GroupCardProps {
  details: IGroupDetails;
  onJoin?: (updated: IGroupDetails) => void;
}

export function GroupCard({ details, onJoin }: GroupCardProps) {
  const { group, memberCount, member, admin, memberStatus } = details;
  const navigate = useNavigate();

  const handleJoin = async () => {
    await request<IGroupMember>({
      endpoint: `/api/v1/groups/${group.id}/join`,
      method: "POST",
      onSuccess: (data) => {
        const newStatus = data.status as "ACTIVE" | "PENDING" | "BANNED";
        onJoin?.({
          ...details,
          member: newStatus === "ACTIVE",
          memberStatus: newStatus,
        });
      },
      onFailure: (err) => console.error(err),
    });
  };

  const getActionButton = () => {
    if (member || admin) {
      return (
        <Button size="small" onClick={() => navigate(`/groups/${group.id}`)}>
          View Group
        </Button>
      );
    }
    if (memberStatus === "PENDING") {
      return (
        <Button size="small" outline disabled>
          Request Sent
        </Button>
      );
    }
    if (memberStatus === "BANNED") {
      return (
        <Button size="small" outline disabled>
          Banned
        </Button>
      );
    }
    return (
      <Button size="small" outline onClick={handleJoin}>
        {group.isPrivate ? "Request to Join" : "Join"}
      </Button>
    );
  };

  return (
    <div className={classes.root}>
      {group.coverPicture ? (
        <img
          className={classes.cover}
          src={`${import.meta.env.VITE_API_URL}/api/v1/storage/${group.coverPicture}`}
          alt={group.name}
        />
      ) : (
        <div className={classes.coverPlaceholder}>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 512">
            <path d="M96 128a128 128 0 1 1 256 0A128 128 0 1 1 96 128zM0 482.3C0 383.8 79.8 304 178.3 304l91.4 0C368.2 304 448 383.8 448 482.3c0 16.4-13.3 29.7-29.7 29.7L29.7 512C13.3 512 0 498.7 0 482.3zM609.3 512l-137.8 0c5.4-9.4 8.6-20.3 8.6-32l0-8c0-60.7-27.1-115.2-69.8-151.8c2.4-.1 4.7-.2 7.1-.2l61.4 0C578.8 320 640 381.2 640 456.9l0 22.1c0 18.4-14.9 33.1-33.3 33.1z" />
          </svg>
        </div>
      )}
      <div className={classes.body}>
        <div className={classes.name}>{group.name}</div>
        <div className={classes.meta}>
          <span>{memberCount} member{memberCount !== 1 ? "s" : ""}</span>
          <span className={classes.badge}>{group.isPrivate ? "Private" : "Public"}</span>
        </div>
        {group.description && (
          <div className={classes.description}>{group.description}</div>
        )}
        <div className={classes.actions}>{getActionButton()}</div>
      </div>
    </div>
  );
}
