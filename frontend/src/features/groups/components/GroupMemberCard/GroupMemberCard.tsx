import { Button } from "../../../../components/Button/Button";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { IGroupMember } from "../../types/groups";
import classes from "./GroupMemberCard.module.scss";

interface GroupMemberCardProps {
  member: IGroupMember;
  groupId: number;
  isCurrentUserAdmin: boolean;
  onUpdate: (updated: IGroupMember) => void;
  onRemove: (memberId: number) => void;
}

export function GroupMemberCard({
  member,
  groupId,
  isCurrentUserAdmin,
  onUpdate,
  onRemove,
}: GroupMemberCardProps) {
  const { user } = useAuthentication();
  const isSelf = member.user.id === user?.id;

  const base = `/api/v1/groups/${groupId}/members/${member.user.id}`;

  const act = (suffix: string, method: "PUT" | "DELETE", onSuccess: (d: IGroupMember) => void) =>
    request<IGroupMember>({
      endpoint: `${base}/${suffix}`,
      method,
      onSuccess,
      onFailure: console.error,
    });

  const statusBadge = () => {
    if (member.status === "PENDING") return <span className={classes.badgePending}>Pending</span>;
    if (member.status === "BANNED") return <span className={classes.badgeBanned}>Banned</span>;
    if (member.role === "ADMIN") return <span className={classes.badgeAdmin}>Admin</span>;
    return null;
  };

  return (
    <div className={classes.root}>
      <img
        className={classes.avatar}
        src={
          member.user.profilePicture
            ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${member.user.profilePicture}`
            : "/avatar.svg"
        }
        alt=""
      />
      <div className={classes.info}>
        <div className={classes.name}>
          {member.user.firstName} {member.user.lastName}
          {statusBadge()}
        </div>
        {member.user.position && member.user.company && (
          <div className={classes.meta}>
            {member.user.position} at {member.user.company}
          </div>
        )}
      </div>

      {isCurrentUserAdmin && !isSelf && (
        <div className={classes.actions}>
          {member.status === "PENDING" && (
            <>
              <Button size="small" onClick={() => act("accept", "PUT", onUpdate)}>
                Accept
              </Button>
              <Button
                size="small"
                outline
                onClick={() =>
                  request<void>({
                    endpoint: `${base}/reject`,
                    method: "DELETE",
                    onSuccess: () => onRemove(member.id),
                    onFailure: console.error,
                  })
                }
              >
                Reject
              </Button>
            </>
          )}
          {member.status === "ACTIVE" && member.role !== "ADMIN" && (
            <>
              <Button size="small" outline onClick={() => act("promote", "PUT", onUpdate)}>
                Make Admin
              </Button>
              <Button
                size="small"
                outline
                onClick={() =>
                  request<void>({
                    endpoint: base,
                    method: "DELETE",
                    onSuccess: () => onRemove(member.id),
                    onFailure: console.error,
                  })
                }
              >
                Remove
              </Button>
              <Button size="small" outline onClick={() => act("ban", "PUT", onUpdate)}>
                Ban
              </Button>
            </>
          )}
          {member.status === "BANNED" && (
            <Button size="small" outline onClick={() => act("unban", "PUT", onUpdate)}>
              Unban
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

export const GroupMemberCardFull = GroupMemberCard;
