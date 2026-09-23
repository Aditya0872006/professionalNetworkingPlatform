import { useEffect, useState } from "react";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { AdminHeader } from "../../components/AdminHeader/AdminHeader";
import { IAdminPost } from "../../types";
import classes from "./AdminPosts.module.scss";

export function AdminPosts() {
  usePageTitle("Admin | Feed Moderation");
  const [posts, setPosts] = useState<IAdminPost[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [feedback, setFeedback] = useState("");

  const fetchPosts = async () => {
    setIsLoading(true);
    await request<IAdminPost[]>({
      endpoint: "/api/v1/admin/posts",
      onSuccess: (data) => setPosts(data),
      onFailure: (err) => console.error("Error fetching admin posts:", err),
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchPosts();
  }, []);

  const handleDeletePost = async (postId: number) => {
    if (!window.confirm("Are you sure you want to remove this post from the platform? The author will be notified.")) {
      return;
    }

    await request({
      endpoint: `/api/v1/admin/posts/${postId}`,
      method: "DELETE",
      onSuccess: () => {
        setPosts((prev) => prev.filter((p) => p.id !== postId));
        setFeedback("Post was removed. The author has been sent a notification.");
        setTimeout(() => setFeedback(""), 4000);
      },
      onFailure: (err) => alert(err),
    });
  };

  return (
    <div className={classes.root}>
      <AdminHeader title="Platform Feed & Post Moderation" />

      {feedback && (
        <div style={{ background: "#e8f5e9", color: "#2e7d32", padding: "0.75rem 1rem", borderRadius: "6px", marginBottom: "1rem" }}>
          ✓ {feedback}
        </div>
      )}

      <div className={classes.headerCard}>
        <h2>All Platform Publications</h2>
        <span>Total: <strong>{posts.length}</strong></span>
      </div>

      {isLoading ? (
        <Loader isInline />
      ) : posts.length === 0 ? (
        <div className={classes.emptyState}>
          <h3>No Publications Found</h3>
          <p>No community posts have been published yet.</p>
        </div>
      ) : (
        <div className={classes.postsList}>
          {posts.map((post) => (
            <div key={post.id} className={classes.postCard}>
              <div className={classes.contentSection}>
                <div className={classes.authorInfo}>
                  <img
                    className={classes.avatar}
                    src={
                      post.author?.profilePicture
                        ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${post.author.profilePicture}`
                        : "/avatar.svg"
                    }
                    alt=""
                  />
                  <div>
                    <div className={classes.name}>
                      {post.author?.firstName || ""} {post.author?.lastName || ""}
                    </div>
                    <div className={classes.email}>{post.author?.email}</div>
                  </div>
                </div>

                <div className={classes.text}>{post.content}</div>

                {post.picture && (
                  <img
                    className={classes.postImage}
                    src={`${import.meta.env.VITE_API_URL}/api/v1/storage/${post.picture}`}
                    alt="Post attachment"
                  />
                )}

                <div className={classes.date}>
                  Published: {new Date(post.creationDate).toLocaleString()}
                </div>
              </div>

              <div className={classes.actions}>
                <button
                  className={classes.deleteBtn}
                  onClick={() => handleDeletePost(post.id)}
                >
                  Delete Post
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
