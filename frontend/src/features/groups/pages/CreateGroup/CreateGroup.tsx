import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Input } from "../../../../components/Input/Input";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { IGroup } from "../../types/groups";
import classes from "./CreateGroup.module.scss";

export function CreateGroup() {
  usePageTitle("Create Group");
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);
  const [coverPicture, setCoverPicture] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setCoverPicture(file);
    setPreviewUrl(file ? URL.createObjectURL(file) : null);
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!name.trim()) return;
    setIsLoading(true);
    setError("");

    const formData = new FormData();
    formData.append("name", name);
    if (description) formData.append("description", description);
    formData.append("isPrivate", String(isPrivate));
    if (coverPicture) formData.append("coverPicture", coverPicture);

    await request<IGroup>({
      endpoint: "/api/v1/groups",
      method: "POST",
      body: formData,
      contentType: "multipart/form-data",
      onSuccess: (group) => navigate(`/groups/${group.id}`),
      onFailure: (err) => {
        setError(err);
        setIsLoading(false);
      },
    });
  };

  return (
    <div className={classes.root}>
      <div className={classes.card}>
        <h1>Create a Group</h1>
        <form onSubmit={handleSubmit} className={classes.form}>
          <div className={classes.coverSection}>
            {previewUrl ? (
              <div className={classes.previewWrapper}>
                <img src={previewUrl} alt="cover preview" className={classes.preview} />
                <button
                  type="button"
                  className={classes.removeImage}
                  onClick={() => { setCoverPicture(null); setPreviewUrl(null); }}
                >
                  ✕ Remove
                </button>
              </div>
            ) : (
              <label className={classes.coverPlaceholder}>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" fill="currentColor" width="32" height="32">
                  <path d="M0 96C0 60.7 28.7 32 64 32l384 0c35.3 0 64 28.7 64 64l0 320c0 35.3-28.7 64-64 64L64 480c-35.3 0-64-28.7-64-64L0 96zM323.8 202.5c-4.5-6.6-11.9-10.5-19.8-10.5s-15.4 3.9-19.8 10.5l-87 127.6L170.7 297c-4.6-5.7-11.5-9-18.7-9s-14.2 3.3-18.7 9l-64 80c-5.8 7.2-6.9 17.1-2.9 25.4s12.4 13.6 21.6 13.6l96 0 32 0 208 0c8.9 0 17.1-4.9 21.2-12.8s3.6-17.4-1.4-24.7l-128-192z" />
                </svg>
                <span>Add a cover photo (optional)</span>
                <input type="file" accept="image/*" onChange={handleFileChange} style={{ display: "none" }} />
              </label>
            )}
          </div>

          <Input
            label="Group name *"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. React Developers India"
            required
          />

          <div className={classes.field}>
            <label>Description</label>
            <textarea
              className={classes.textarea}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What is this group about?"
              rows={3}
            />
          </div>

          <div className={classes.toggle}>
            <label className={classes.toggleLabel}>
              <input
                type="checkbox"
                checked={isPrivate}
                onChange={(e) => setIsPrivate(e.target.checked)}
              />
              <div>
                <span className={classes.toggleTitle}>Private Group</span>
                <span className={classes.toggleDesc}>
                  {isPrivate
                    ? "Members must request to join and be approved by an admin."
                    : "Anyone can join without approval."}
                </span>
              </div>
            </label>
          </div>

          {error && <p className={classes.error}>{error}</p>}

          <div className={classes.footer}>
            <Button type="button" outline onClick={() => navigate("/groups")}>
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading || !name.trim()}>
              {isLoading ? "Creating…" : "Create Group"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
