import { FormEvent, useState } from "react";
import { Button } from "../../../../components/Button/Button";
import { IExperience } from "../../types/profile";
import classes from "./ExperienceModal.module.scss";

interface ExperienceModalProps {
  show: boolean;
  onClose: () => void;
  onSubmit: (data: Omit<IExperience, "id">) => Promise<void>;
  existing?: IExperience | null;
}

export function ExperienceModal({ show, onClose, onSubmit, existing }: ExperienceModalProps) {
  const [companyName, setCompanyName] = useState(existing?.companyName ?? "");
  const [jobTitle, setJobTitle] = useState(existing?.jobTitle ?? "");
  const [startDate, setStartDate] = useState(existing?.startDate ?? "");
  const [endDate, setEndDate] = useState(existing?.endDate ?? "");
  const [isCurrent, setIsCurrent] = useState(!existing?.endDate && !!existing?.startDate);
  const [description, setDescription] = useState(existing?.description ?? "");
  const [submitting, setSubmitting] = useState(false);

  if (!show) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!companyName.trim() || !jobTitle.trim()) return;

    setSubmitting(true);
    try {
      await onSubmit({
        companyName: companyName.trim(),
        jobTitle: jobTitle.trim(),
        startDate: startDate || null,
        endDate: isCurrent ? null : endDate || null,
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
          <h2>{existing ? "Edit Experience" : "Add Experience"}</h2>
          <button onClick={onClose}>✕</button>
        </header>
        <form onSubmit={handleSubmit}>
          <div className={classes.formGroup}>
            <label>Job Title *</label>
            <input
              type="text"
              required
              placeholder="e.g. Senior Software Engineer"
              value={jobTitle}
              onChange={(e) => setJobTitle(e.target.value)}
              autoFocus
            />
          </div>

          <div className={classes.formGroup}>
            <label>Company Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. Google, Microsoft"
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
            />
          </div>

          <div className={classes.dateRow}>
            <div className={classes.formGroup}>
              <label>Start Date</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            {!isCurrent && (
              <div className={classes.formGroup}>
                <label>End Date</label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            )}
          </div>

          <div className={classes.checkboxRow}>
            <input
              type="checkbox"
              id="currentJob"
              checked={isCurrent}
              onChange={(e) => setIsCurrent(e.target.checked)}
            />
            <label htmlFor="currentJob">I am currently working in this role</label>
          </div>

          <div className={classes.formGroup}>
            <label>Description</label>
            <textarea
              placeholder="What did you accomplish in this role?"
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
