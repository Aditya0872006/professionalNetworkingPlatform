import { FormEvent, useState } from "react";
import { Button } from "../../../../components/Button/Button";
import { IProject } from "../../types/profile";
import classes from "./ProjectModal.module.scss";

interface ProjectModalProps {
  show: boolean;
  onClose: () => void;
  onSubmit: (data: Omit<IProject, "id">) => Promise<void>;
  existing?: IProject | null;
}

export function ProjectModal({ show, onClose, onSubmit, existing }: ProjectModalProps) {
  const [projectName, setProjectName] = useState(existing?.projectName ?? "");
  const [projectUrl, setProjectUrl] = useState(existing?.projectUrl ?? "");
  const [description, setDescription] = useState(existing?.description ?? "");
  const [submitting, setSubmitting] = useState(false);

  if (!show) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!projectName.trim()) return;

    setSubmitting(true);
    try {
      await onSubmit({
        projectName: projectName.trim(),
        projectUrl: projectUrl.trim() || null,
        description: description.trim() || null,
      });
      onClose();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={classes.overlay} onClick={onClose}>
      <div className={classes.modal} onClick={(e) => e.stopPropagation()}>
        <header>
          <h2>{existing ? "Edit Project" : "Add Project"}</h2>
          <button onClick={onClose}>✕</button>
        </header>
        <form onSubmit={handleSubmit}>
          <div className={classes.formGroup}>
            <label>Project Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Portfolio Website, E-commerce App"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              autoFocus
            />
          </div>

          <div className={classes.formGroup}>
            <label>Project URL</label>
            <input
              type="url"
              placeholder="https://github.com/username/project"
              value={projectUrl}
              onChange={(e) => setProjectUrl(e.target.value)}
            />
          </div>

          <div className={classes.formGroup}>
            <label>Description</label>
            <textarea
              placeholder="Describe what the project does, tech stack used, etc."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className={classes.actions}>
            <Button size="small" outline type="button" onClick={onClose}>
              Cancel
            </Button>
            <Button size="small" type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Save"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
