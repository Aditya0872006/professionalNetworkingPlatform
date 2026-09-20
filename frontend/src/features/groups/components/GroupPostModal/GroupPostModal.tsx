import { FormEvent, useState } from "react";
import { Button } from "../../../../components/Button/Button";
import { IGroupPost } from "../../types/groups";
import classes from "./GroupPostModal.module.scss";

interface GroupPostModalProps {
  groupId?: number;
  showModal: boolean;
  setShowModal: (show: boolean) => void;
  existingPost?: IGroupPost;
  onSubmit: (formData: FormData) => Promise<void>;
  title: string;
}

export function GroupPostModal({
  showModal,
  setShowModal,
  existingPost,
  onSubmit,
  title,
}: GroupPostModalProps) {
  const [content, setContent] = useState(existingPost?.content ?? "");
  const [picture, setPicture] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(
    existingPost?.picture
      ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${existingPost.picture}`
      : null
  );
  const [isLoading, setIsLoading] = useState(false);

  if (!showModal) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setPicture(file);
    if (file) {
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!content.trim()) return;
    setIsLoading(true);
    const formData = new FormData();
    formData.append("content", content);
    if (picture) formData.append("picture", picture);
    try {
      await onSubmit(formData);
      setShowModal(false);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={classes.overlay} onClick={() => setShowModal(false)}>
      <div className={classes.modal} onClick={(e) => e.stopPropagation()}>
        <div className={classes.header}>
          <h2>{title}</h2>
          <button className={classes.close} onClick={() => setShowModal(false)}>
            ✕
          </button>
        </div>
        <form onSubmit={handleSubmit} className={classes.form}>
          <textarea
            className={classes.textarea}
            placeholder="What do you want to share with the group?"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={5}
            required
          />
          {previewUrl && (
            <div className={classes.preview}>
              <img src={previewUrl} alt="preview" />
              <button
                type="button"
                className={classes.removeImage}
                onClick={() => {
                  setPicture(null);
                  setPreviewUrl(null);
                }}
              >
                ✕ Remove image
              </button>
            </div>
          )}
          <div className={classes.footer}>
            <label className={classes.fileLabel}>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" fill="currentColor" width="18" height="18">
                <path d="M0 96C0 60.7 28.7 32 64 32l384 0c35.3 0 64 28.7 64 64l0 320c0 35.3-28.7 64-64 64L64 480c-35.3 0-64-28.7-64-64L0 96zM323.8 202.5c-4.5-6.6-11.9-10.5-19.8-10.5s-15.4 3.9-19.8 10.5l-87 127.6L170.7 297c-4.6-5.7-11.5-9-18.7-9s-14.2 3.3-18.7 9l-64 80c-5.8 7.2-6.9 17.1-2.9 25.4s12.4 13.6 21.6 13.6l96 0 32 0 208 0c8.9 0 17.1-4.9 21.2-12.8s3.6-17.4-1.4-24.7l-128-192zM112 192a48 48 0 1 0 0-96 48 48 0 1 0 0 96z" />
              </svg>
              Add image
              <input type="file" accept="image/*" onChange={handleFileChange} style={{ display: "none" }} />
            </label>
            <Button type="submit" disabled={isLoading || !content.trim()} size="small">
              {isLoading ? "Posting…" : "Post"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
