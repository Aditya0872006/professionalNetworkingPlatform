import { FormEvent, useState } from "react";
import { Button } from "../../../../components/Button/Button";
import { IEducation } from "../../types/profile";
import classes from "./EducationModal.module.scss";

interface EducationModalProps {
  show: boolean;
  onClose: () => void;
  onSubmit: (data: Omit<IEducation, "id">) => Promise<void>;
  existing?: IEducation | null;
}

export function EducationModal({ show, onClose, onSubmit, existing }: EducationModalProps) {
  const [institution, setInstitution] = useState(existing?.institution ?? "");
  const [degree, setDegree] = useState(existing?.degree ?? "");
  const [fieldOfStudy, setFieldOfStudy] = useState(existing?.fieldOfStudy ?? "");
  const [startDate, setStartDate] = useState(existing?.startDate ?? "");
  const [endDate, setEndDate] = useState(existing?.endDate ?? "");
  const [isCurrent, setIsCurrent] = useState(!existing?.endDate && !!existing?.startDate);
  const [submitting, setSubmitting] = useState(false);

  if (!show) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!institution.trim()) return;

    setSubmitting(true);
    try {
      await onSubmit({
        institution: institution.trim(),
        degree: degree.trim() || null,
        fieldOfStudy: fieldOfStudy.trim() || null,
        startDate: startDate || null,
        endDate: isCurrent ? null : endDate || null,
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
          <h2>{existing ? "Edit Education" : "Add Education"}</h2>
          <button onClick={onClose}>✕</button>
        </header>
        <form onSubmit={handleSubmit}>
          <div className={classes.formGroup}>
            <label>School / University *</label>
            <input
              type="text"
              required
              placeholder="e.g. Stanford University"
              value={institution}
              onChange={(e) => setInstitution(e.target.value)}
              autoFocus
            />
          </div>

          <div className={classes.formGroup}>
            <label>Degree</label>
            <input
              type="text"
              placeholder="e.g. Bachelor's of Science"
              value={degree}
              onChange={(e) => setDegree(e.target.value)}
            />
          </div>

          <div className={classes.formGroup}>
            <label>Field of Study</label>
            <input
              type="text"
              placeholder="e.g. Computer Science"
              value={fieldOfStudy}
              onChange={(e) => setFieldOfStudy(e.target.value)}
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
                <label>End Date (or expected)</label>
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
              id="currentStudy"
              checked={isCurrent}
              onChange={(e) => setIsCurrent(e.target.checked)}
            />
            <label htmlFor="currentStudy">I am currently studying here</label>
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
