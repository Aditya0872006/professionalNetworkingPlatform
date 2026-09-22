import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { useWebSocket } from "../../../ws/WebSocketContextProvider";
import { GroupMemberCardFull } from "../../components/GroupMemberCard/GroupMemberCard";
import { GroupPost } from "../../components/GroupPost/GroupPost";
import { GroupPostModal } from "../../components/GroupPostModal/GroupPostModal";
import { IGroupDetails, IGroupMember, IGroupPost } from "../../types/groups";
import classes from "./GroupDetail.module.scss";

type Tab = "posts" | "members";

export function GroupDetail() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);
  const navigate = useNavigate();
  const webSocketClient = useWebSocket();
  const { user } = useAuthentication();

  const [details, setDetails] = useState<IGroupDetails | null>(null);
  const [posts, setPosts] = useState<IGroupPost[]>([]);
  const [members, setMembers] = useState<IGroupMember[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showPostModal, setShowPostModal] = useState(false);
  const [activeTab, setActiveTab] = useState<Tab>("posts");
  const [error, setError] = useState("");

  usePageTitle(details?.group.name ?? "Group");

  // ─── Initial load ─────────────────────────────────────────────────────────

  useEffect(() => {
    const load = async () => {
      setIsLoading(true);
      await request<IGroupDetails>({
        endpoint: `/api/v1/groups/${groupId}`,
        onSuccess: setDetails,
        onFailure: (err) => { setError(err); setIsLoading(false); },
      });
      setIsLoading(false);
    };
    load();
  }, [groupId]);

  useEffect(() => {
    if (!details) return;
    if (!details.member && !details.admin && details.group.isPrivate) return;

    request<IGroupPost[]>({
      endpoint: `/api/v1/groups/${groupId}/posts`,
      onSuccess: setPosts,
      onFailure: console.error,
    });
  }, [groupId, details]);

  useEffect(() => {
    if (!details?.admin) return;
    request<IGroupMember[]>({
      endpoint: `/api/v1/groups/${groupId}/members`,
      onSuccess: setMembers,
      onFailure: console.error,
    });
  }, [groupId, details?.admin]);

  // ─── WebSocket: new group posts ───────────────────────────────────────────

  useEffect(() => {
    const sub = webSocketClient?.subscribe(`/topic/groups/${groupId}/posts`, (msg) => {
      const post: IGroupPost = JSON.parse(msg.body);
      setPosts((prev) => {
        if (prev.some((p) => p.id === post.id)) return prev;
        return [post, ...prev];
      });
    });
    return () => sub?.unsubscribe();
  }, [groupId, webSocketClient]);

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/members`,
      (msg) => {
        const { action, member } = JSON.parse(msg.body);
        if (action === "ADD" || action === "UPDATE") {
          setMembers((prev) => {
            const idx = prev.findIndex((m) => m.id === member.id);
            if (idx === -1) return [...prev, member];
            return prev.map((m) => (m.id === member.id ? member : m));
          });
        } else if (action === "REMOVE") {
          setMembers((prev) => prev.filter((m) => m.id !== member.id));
        }
      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, webSocketClient]);

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/visibility`,
      (msg) => {
        const isPrivate: boolean = JSON.parse(msg.body);
        setDetails((prev) =>
          prev ? { ...prev, group: { ...prev.group, isPrivate } } : prev
        );
      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, webSocketClient]);

  // ─── WebSocket: personal membership status (ban / unban / remove) ─────────

  useEffect(() => {
    if (!user?.id) return;
    const sub = webSocketClient?.subscribe(
      `/topic/users/${user.id}/groups/${groupId}/membership`,
      (msg) => {
        const { status } = JSON.parse(msg.body) as { status: string };
        if (status === "REMOVED") {
          navigate("/groups");
        } else if (status === "BANNED") {
          setDetails((prev) =>
            prev
              ? { ...prev, member: false, admin: false, memberStatus: "BANNED" as const }
              : prev
          );
        } else if (status === "ACTIVE") {
          // Unbanned — restore as active member
          setDetails((prev) =>
            prev
              ? { ...prev, member: true, memberStatus: "ACTIVE" as const }
              : prev
          );
        } else if (status === "PROMOTED") {
          // Promoted to admin — show admin controls immediately
          setDetails((prev) =>
            prev
              ? { ...prev, member: true, admin: true, memberStatus: "ACTIVE" as const }
              : prev
          );
        }

      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, user?.id, webSocketClient, navigate]);

  // ─── Join / Leave ─────────────────────────────────────────────────────────

  const joinGroup = async () => {
    await request<IGroupMember>({
      endpoint: `/api/v1/groups/${groupId}/join`,
      method: "POST",
      onSuccess: (data) => {
        setDetails((prev) =>
          prev
            ? {
              ...prev,
              member: data.status === "ACTIVE",
              memberStatus: data.status,
              memberCount: data.status === "ACTIVE" ? prev.memberCount + 1 : prev.memberCount,
            }
            : prev
        );
      },
      onFailure: console.error,
    });
  };

  const leaveGroup = async () => {
    if (!window.confirm("Are you sure you want to leave this group?")) return;
    await request<void>({
      endpoint: `/api/v1/groups/${groupId}/leave`,
      method: "DELETE",
      onSuccess: () => navigate("/groups"),
      onFailure: console.error,
    });
  };

  const deleteGroup = async () => {
    if (!window.confirm("Are you sure you want to permanently delete this group? This cannot be undone.")) return;
    await request<void>({
      endpoint: `/api/v1/groups/${groupId}`,
      method: "DELETE",
      onSuccess: () => navigate("/groups"),
      onFailure: console.error,
    });
  };

  // ─── WebSocket: group deleted (redirect all viewers) ─────────────────────

  useEffect(() => {
    const sub = webSocketClient?.subscribe(`/topic/groups/${groupId}/deleted`, () => {
      navigate("/groups");
    });
    return () => sub?.unsubscribe();
  }, [groupId, webSocketClient, navigate]);


  const toggleVisibility = async () => {
    if (!details) return;
    await request<void>({
      endpoint: `/api/v1/groups/${groupId}?isPrivate=${!details.group.isPrivate}`,
      method: "PUT",
      onSuccess: () => {}, // WebSocket subscription on /visibility handles the state update
      onFailure: console.error,
    });
  };

  // ─── Create Post ──────────────────────────────────────────────────────────

  const createPost = async (formData: FormData) => {
    await request<IGroupPost>({
      endpoint: `/api/v1/groups/${groupId}/posts`,
      method: "POST",
      body: formData,
      contentType: "multipart/form-data",
      onSuccess: () => {}, // WebSocket subscription handles adding to list
      onFailure: (err) => { throw new Error(err); },
    });
  };

  // ─── Member management callbacks ──────────────────────────────────────────

  const handleMemberUpdate = (updated: IGroupMember) => {
    setMembers((prev) => prev.map((m) => (m.id === updated.id ? updated : m)));
    // Update memberCount if status changed from PENDING→ACTIVE
    if (updated.status === "ACTIVE") {
      setDetails((prev) =>
        prev ? { ...prev, memberCount: prev.memberCount + 1 } : prev
      );
    }
  };

  const handleMemberRemove = (memberId: number) => {
    setMembers((prev) => prev.filter((m) => m.id !== memberId));
  };

  if (isLoading) return <Loader isInline />;
  if (error) return <div className={classes.error}>{error}</div>;
  if (!details) return null;

  const { group, memberCount, member, admin, memberStatus } = details;
  const isMemberOrAdmin = member || admin;

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div className={classes.root}>
      {showPostModal && (
        <GroupPostModal
          groupId={groupId}
          showModal={showPostModal}
          setShowModal={setShowPostModal}
          onSubmit={createPost}
          title="Create a post"
        />
      )}

      {/* Cover + Group Info */}
      <div className={classes.header}>
        {group.coverPicture ? (
          <img
            className={classes.cover}
            src={`${import.meta.env.VITE_API_URL}/api/v1/storage/${group.coverPicture}`}
            alt={group.name}
          />
        ) : (
          <div className={classes.coverPlaceholder} />
        )}
        <div className={classes.info}>
          <div className={classes.titleRow}>
            <div>
              <h1>{group.name}</h1>
              <div className={classes.meta}>
                <span>{group.isPrivate ? "🔒 Private" : "🌐 Public"} Group</span>
                <span>·</span>
                <span>{memberCount} member{memberCount !== 1 ? "s" : ""}</span>
              </div>
            </div>
            <div className={classes.headerActions}>
              {isMemberOrAdmin && (
                <Button size="small" onClick={() => setShowPostModal(true)}>
                  + Post
                </Button>
              )}
              {!isMemberOrAdmin && memberStatus !== "BANNED" && (
                <Button
                  size="small"
                  onClick={joinGroup}
                  disabled={memberStatus === "PENDING"}
                >
                  {memberStatus === "PENDING" ? "Request Sent" : group.isPrivate ? "Request to Join" : "Join Group"}
                </Button>
              )}
              {isMemberOrAdmin && !admin && (
                <Button size="small" outline onClick={leaveGroup}>
                  Leave
                </Button>
              )}
              {admin && (
                <Button size="small" outline onClick={toggleVisibility}>
                  {group.isPrivate ? "Make Public" : "Make Private"}
                </Button>
              )}
              {admin && (
                <Button
                  size="small"
                  outline
                  onClick={deleteGroup}
                  style={{ color: "#d93025", borderColor: "#d93025" }}
                >
                  Delete Group
                </Button>
              )}
            </div>
          </div>
          {group.description && (
            <p className={classes.description}>{group.description}</p>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className={classes.tabs}>
        <button
          className={activeTab === "posts" ? classes.activeTab : ""}
          onClick={() => setActiveTab("posts")}
        >
          Posts
        </button>
        {admin && (
          <button
            className={activeTab === "members" ? classes.activeTab : ""}
            onClick={() => setActiveTab("members")}
          >
            Members {members.filter((m) => m.status === "PENDING").length > 0 &&
              <span className={classes.pendingBadge}>
                {members.filter((m) => m.status === "PENDING").length}
              </span>
            }
          </button>
        )}
      </div>

      {/* Tab content */}
      {activeTab === "posts" && (
        <div className={classes.feed}>
          {!isMemberOrAdmin && group.isPrivate ? (
            <div className={classes.locked}>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" fill="currentColor" width="2.5rem">
                <path d="M144 144v48H304V144c0-44.2-35.8-80-80-80s-80 35.8-80 80zM80 192V144C80 64.5 144.5 0 224 0s144 64.5 144 144v48h16c35.3 0 64 28.7 64 64V448c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V256c0-35.3 28.7-64 64-64H80z" />
              </svg>
              <p>This is a private group. Join to see posts.</p>
              {memberStatus !== "PENDING" && memberStatus !== "BANNED" && (
                <Button size="small" onClick={joinGroup}>Request to Join</Button>
              )}
            </div>
          ) : posts.length === 0 ? (
            <div className={classes.empty}>
              No posts yet.{isMemberOrAdmin ? " Be the first to post!" : ""}
            </div>
          ) : (
            posts.map((post) => (
              <GroupPost
                key={post.id}
                post={post}
                groupId={groupId}
                isAdmin={admin}
                setPosts={setPosts}
              />
            ))
          )}
        </div>
      )}

      {activeTab === "members" && admin && (
        <div className={classes.membersList}>
          {members.length === 0 ? (
            <p className={classes.empty}>No members yet.</p>
          ) : (
            members.map((m) => (
              <GroupMemberCardFull
                key={m.id}
                member={m}
                groupId={groupId}
                isCurrentUserAdmin={admin}
                onUpdate={handleMemberUpdate}
                onRemove={handleMemberRemove}
              />
            ))
          )}
        </div>
      )}
    </div>
  );
}
