import { Dispatch, FormEvent, SetStateAction, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Input } from "../../../../components/Input/Input";
import { request } from "../../../../utils/api";
import { useAuthentication } from "../../../authentication/contexts/AuthenticationContextProvider";
import { useWebSocket } from "../../../ws/WebSocketContextProvider";
import { IGroupPost, IGroupPostComment } from "../../types/groups";
import { GroupPostModal } from "../GroupPostModal/GroupPostModal";
import classes from "./GroupPost.module.scss";
import { IUser } from "../../../authentication/contexts/AuthenticationContextProvider";

interface GroupPostProps {
  post: IGroupPost;
  groupId: number;
  isAdmin: boolean;
  setPosts: Dispatch<SetStateAction<IGroupPost[]>>;
}

export function GroupPost({ post, groupId, isAdmin, setPosts }: GroupPostProps) {
  const { user } = useAuthentication();
  const navigate = useNavigate();
  const webSocketClient = useWebSocket();

  const [comments, setComments] = useState<IGroupPostComment[]>([]);
  const [showComments, setShowComments] = useState(false);
  const [likes, setLikes] = useState<IUser[]>([]);
  const [postLiked, setPostLiked] = useState<boolean | undefined>(undefined);
  const [commentContent, setCommentContent] = useState("");
  const [showMenu, setShowMenu] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [editingCommentContent, setEditingCommentContent] = useState("");

  // ─── WebSocket subscriptions ─────────────────────────────────────────────

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/posts/${post.id}/likes`,
      (msg) => {
        const updated: IUser[] = JSON.parse(msg.body);
        setLikes(updated);
        setPostLiked(updated.some((u) => u.id === user?.id));
      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, post.id, user?.id, webSocketClient]);

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/posts/${post.id}/comments`,
      (msg) => {
        const comment: IGroupPostComment = JSON.parse(msg.body);
        setComments((prev) => {
          const idx = prev.findIndex((c) => c.id === comment.id);
          if (idx === -1) return [...prev, comment];
          return prev.map((c) => (c.id === comment.id ? comment : c));
        });
      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, post.id, webSocketClient]);

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/posts/${post.id}/comments/delete`,
      (msg) => {
        const comment: IGroupPostComment = JSON.parse(msg.body);
        setComments((prev) => prev.filter((c) => c.id !== comment.id));
      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, post.id, webSocketClient]);

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/posts/${post.id}/delete`,
      () => setPosts((prev) => prev.filter((p) => p.id !== post.id))
    );
    return () => sub?.unsubscribe();
  }, [groupId, post.id, setPosts, webSocketClient]);

  useEffect(() => {
    const sub = webSocketClient?.subscribe(
      `/topic/groups/${groupId}/posts/${post.id}/edit`,
      (msg) => {
        const updated: IGroupPost = JSON.parse(msg.body);
        setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      }
    );
    return () => sub?.unsubscribe();
  }, [groupId, post.id, setPosts, webSocketClient]);

  // ─── Initial fetches ─────────────────────────────────────────────────────

  useEffect(() => {
    request<IUser[]>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}/likes`,
      onSuccess: (data) => {
        setLikes(data);
        setPostLiked(data.some((u) => u.id === user?.id));
      },
      onFailure: console.error,
    });
  }, [groupId, post.id, user?.id]);

  useEffect(() => {
    request<IGroupPostComment[]>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}/comments`,
      onSuccess: setComments,
      onFailure: console.error,
    });
  }, [groupId, post.id]);

  // ─── Actions ─────────────────────────────────────────────────────────────

  const like = () => {
    request<IGroupPost>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}/like`,
      method: "PUT",
      onSuccess: () => {},
      onFailure: console.error,
    });
  };

  const postComment = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!commentContent.trim()) return;
    await request<IGroupPostComment>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}/comments`,
      method: "POST",
      body: JSON.stringify({ content: commentContent }),
      onSuccess: () => setCommentContent(""),
      onFailure: console.error,
    });
  };

  const deleteComment = (commentId: number) => {
    request<void>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}/comments/${commentId}`,
      method: "DELETE",
      onSuccess: () => setComments((prev) => prev.filter((c) => c.id !== commentId)),
      onFailure: console.error,
    });
  };

  const saveEditComment = (commentId: number) => {
    request<IGroupPostComment>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}/comments/${commentId}`,
      method: "PUT",
      body: JSON.stringify({ content: editingCommentContent }),
      onSuccess: (data) => {
        setComments((prev) => prev.map((c) => (c.id === commentId ? data : c)));
        setEditingCommentId(null);
      },
      onFailure: console.error,
    });
  };

  const deletePost = () => {
    request<void>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}`,
      method: "DELETE",
      onSuccess: () => setPosts((prev) => prev.filter((p) => p.id !== post.id)),
      onFailure: console.error,
    });
  };

  const editPost = async (formData: FormData) => {
    await request<IGroupPost>({
      endpoint: `/api/v1/groups/${groupId}/posts/${post.id}`,
      method: "PUT",
      body: formData,
      contentType: "multipart/form-data",
      onSuccess: (data) => setPosts((prev) => prev.map((p) => (p.id === data.id ? data : p))),
      onFailure: (err) => { throw new Error(err); },
    });
  };

  const isOwner = post.author.id === user?.id;
  const canDelete = isOwner || isAdmin;

  const timeAgo = (dateStr: string) => {
    const diff = (Date.now() - new Date(dateStr).getTime()) / 1000;
    if (diff < 60) return "Just now";
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  return (
    <>
      {editing && (
        <GroupPostModal
          groupId={groupId}
          showModal={editing}
          setShowModal={setEditing}
          existingPost={post}
          onSubmit={editPost}
          title="Edit post"
        />
      )}
      <div className={classes.root}>
        <div className={classes.top}>
          <div className={classes.author}>
            <button onClick={() => navigate(`/profile/${post.author.id}`)}>
              <img
                className={classes.avatar}
                src={
                  post.author.profilePicture
                    ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${post.author.profilePicture}`
                    : "/avatar.svg"
                }
                alt=""
              />
            </button>
            <div>
              <div className={classes.name}>
                {post.author.firstName} {post.author.lastName}
              </div>
              <div className={classes.meta}>
                {post.author.position && post.author.company
                  ? `${post.author.position} at ${post.author.company}`
                  : post.author.position ?? post.author.company ?? ""}
              </div>
              <div className={classes.date}>
                {timeAgo(post.creationDate)}
                {post.updatedDate ? " · Edited" : ""}
              </div>
            </div>
          </div>
          {(isOwner || isAdmin) && (
            <div className={classes.menuWrapper}>
              <button
                className={`${classes.menuBtn} ${showMenu ? classes.active : ""}`}
                onClick={() => setShowMenu((p) => !p)}
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 512">
                  <path d="M64 360a56 56 0 1 0 0 112 56 56 0 1 0 0-112zm0-160a56 56 0 1 0 0 112 56 56 0 1 0 0-112zM120 96A56 56 0 1 0 8 96a56 56 0 1 0 112 0z" />
                </svg>
              </button>
              {showMenu && (
                <div className={classes.menu}>
                  {isOwner && (
                    <button onClick={() => { setEditing(true); setShowMenu(false); }}>
                      Edit
                    </button>
                  )}
                  {canDelete && (
                    <button onClick={deletePost}>Delete</button>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        <div className={classes.content}>{post.content}</div>
        {post.picture && (
          <img
            src={`${import.meta.env.VITE_API_URL}/api/v1/storage/${post.picture}`}
            alt=""
            className={classes.picture}
          />
        )}

        <div className={classes.stats}>
          {likes.length > 0 ? (
            <span>
              {postLiked ? "You" : `${likes[0].firstName} ${likes[0].lastName}`}
              {likes.length - 1 > 0 ? ` and ${likes.length - 1} other${likes.length - 1 !== 1 ? "s" : ""}` : ""} liked this
            </span>
          ) : <span />}
          {comments.length > 0 && (
            <button onClick={() => setShowComments((p) => !p)}>
              {comments.length} comment{comments.length !== 1 ? "s" : ""}
            </button>
          )}
        </div>

        <div className={classes.actions}>
          <button
            disabled={postLiked === undefined}
            onClick={like}
            className={postLiked ? classes.liked : ""}
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" fill="currentColor" width="18" height="18">
              <path d="M225.8 468.2l-2.5-2.3L48.1 303.2C17.4 274.7 0 234.7 0 192.8l0-3.3c0-70.4 50-130.8 119.2-144C158.6 37.9 198.9 47 231 69.6c9 6.4 17.4 13.8 25 22.3c4.2-4.8 8.7-9.2 13.5-13.3c3.7-3.2 7.5-6.2 11.5-9C313.1 47 353.4 37.9 392.8 45.4C462 58.6 512 119.1 512 189.5l0 3.3c0 41.9-17.4 81.9-48.1 110.4L288.7 465.9l-2.5 2.3c-8.2 7.6-19 11.9-30.2 11.9s-22-4.2-30.2-11.9z" />
            </svg>
            <span>{postLiked === undefined ? "…" : postLiked ? "Liked" : "Like"}</span>
          </button>
          <button onClick={() => setShowComments((p) => !p)} className={showComments ? classes.liked : ""}>
            <svg fill="currentColor" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="18" height="18">
              <path d="M123.6 391.3c12.9-9.4 29.6-11.8 44.6-6.4c26.5 9.6 56.2 15.1 87.8 15.1c124.7 0 208-80.5 208-160s-83.3-160-208-160S48 160.5 48 240c0 32 12.4 62.8 35.7 89.2c8.6 9.7 12.8 22.5 11.8 35.5c-1.4 18.1-5.7 34.7-11.3 49.4c17-7.9 31.1-16.7 39.4-22.7z" />
            </svg>
            <span>Comment</span>
          </button>
        </div>

        {showComments && (
          <div className={classes.comments}>
            <form onSubmit={postComment} className={classes.commentForm}>
              <Input
                placeholder="Write a comment…"
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
                style={{ marginBlock: 0 }}
              />
            </form>
            {comments.map((comment) => (
              <div key={comment.id} className={classes.comment}>
                <img
                  className={classes.commentAvatar}
                  src={
                    comment.author.profilePicture
                      ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${comment.author.profilePicture}`
                      : "/avatar.svg"
                  }
                  alt=""
                />
                <div className={classes.commentBody}>
                  <div className={classes.commentAuthor}>
                    {comment.author.firstName} {comment.author.lastName}
                  </div>
                  {editingCommentId === comment.id ? (
                    <div className={classes.commentEdit}>
                      <Input
                        value={editingCommentContent}
                        onChange={(e) => setEditingCommentContent(e.target.value)}
                        style={{ marginBlock: 0 }}
                      />
                      <button onClick={() => saveEditComment(comment.id)}>Save</button>
                      <button onClick={() => setEditingCommentId(null)}>Cancel</button>
                    </div>
                  ) : (
                    <div className={classes.commentContent}>{comment.content}</div>
                  )}
                  <div className={classes.commentMeta}>{timeAgo(comment.creationDate)}</div>
                </div>
                {(comment.author.id === user?.id || isAdmin) && editingCommentId !== comment.id && (
                  <div className={classes.commentActions}>
                    {comment.author.id === user?.id && (
                      <button onClick={() => { setEditingCommentId(comment.id); setEditingCommentContent(comment.content); }}>
                        Edit
                      </button>
                    )}
                    <button onClick={() => deleteComment(comment.id)}>Delete</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
