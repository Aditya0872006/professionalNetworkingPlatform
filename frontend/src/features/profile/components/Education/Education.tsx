import { useEffect, useState } from "react";
import { request } from "../../../../utils/api";
import { IEducation } from "../../types/profile";
import classes from "./Education.module.scss";
import { EducationModal } from "./EducationModal";

interface EducationProps {
  userId: string | number | undefined;
  isOwner: boolean;
}


function formatDate(dateStr?: string | null): string {
  if (!dateStr) return "";
  try {
    const [year, month] = dateStr.split("-");
    if (!year) return dateStr;
    const date = new Date(Number(year), Number(month || 1) - 1);
    return date.toLocaleString("en-US", { month: "short", year: "numeric" });
  } catch {
    return dateStr;
  }
}

export function Education({ userId, isOwner }: EducationProps) {
  const [educations, setEducations] = useState<IEducation[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedEducation, setSelectedEducation] = useState<IEducation | null>(null);

  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    request<IEducation[]>({
      endpoint: `/api/v1/profile/users/${userId}/education`,
      onSuccess: (data) => {
        setEducations(data || []);
        setLoading(false);
      },
      onFailure: (err) => {
        console.error("Failed to load educations:", err);
        setLoading(false);
      },
    });
  }, [userId]);

  const handleCreate = async (data: Omit<IEducation, "id">) => {
    await request<IEducation>({
      endpoint: "/api/v1/profile/education",
      method: "POST",
      body: JSON.stringify(data),
      onSuccess: (saved) => {
        setEducations((prev) => [saved, ...prev]);
      },
      onFailure: (err) => console.error("Failed to add education:", err),
    });
  };

  const handleUpdate = async (data: Omit<IEducation, "id">) => {
    if (!selectedEducation) return;
    await request<IEducation>({
      endpoint: `/api/v1/profile/education/${selectedEducation.id}`,
      method: "PUT",
      body: JSON.stringify(data),
      onSuccess: (updated) => {
        setEducations((prev) =>
          prev.map((edu) => (edu.id === updated.id ? updated : edu))
        );
      },
      onFailure: (err) => console.error("Failed to update education:", err),
    });
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this education entry?")) return;
    await request<void>({
      endpoint: `/api/v1/profile/education/${id}`,
      method: "DELETE",
      onSuccess: () => {
        setEducations((prev) => prev.filter((edu) => edu.id !== id));
      },
      onFailure: (err) => console.error("Failed to delete education:", err),
    });
  };

  return (
    <div className={classes.education}>
      {showModal && (
        <EducationModal
          show={showModal}
          onClose={() => {
            setShowModal(false);
            setSelectedEducation(null);
          }}
          onSubmit={selectedEducation ? handleUpdate : handleCreate}
          existing={selectedEducation}
        />
      )}

      <div className={classes.header}>
        <h2>Education</h2>
        {isOwner && (
          <button
            onClick={() => {
              setSelectedEducation(null);
              setShowModal(true);
            }}
            title="Add Education"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
              <path d="M256 80c0-17.7-14.3-32-32-32s-32 14.3-32 32l0 144L48 256c-17.7 0-32 14.3-32 32s14.3 32 32 32l144 0 0 144c0 17.7 14.3 32 32 32s32-14.3 32-32l0-144 144 0c17.7 0 32-14.3 32-32s-14.3-32-32-32l-144 0 0-144z" />
            </svg>
          </button>
        )}
      </div>

      {loading ? (
        <p className={classes.empty}>Loading education...</p>
      ) : educations.length === 0 ? (
        <p className={classes.empty}>No education listed yet.</p>
      ) : (
        <div className={classes.list}>
          {educations.map((edu) => {
            const start = formatDate(edu.startDate);
            const end = edu.endDate ? formatDate(edu.endDate) : "Present";
            const dateRange = start ? `${start} – ${end}` : end !== "Present" ? end : "";

            const degreeAndField = [edu.degree, edu.fieldOfStudy]
              .filter(Boolean)
              .join(", ");

            return (
              <div key={edu.id} className={classes.item}>
                <div className={classes.info}>
                  <h3>{edu.institution}</h3>
                  {degreeAndField && <div className={classes.degree}>{degreeAndField}</div>}
                  {dateRange && <div className={classes.dates}>{dateRange}</div>}
                </div>
                {isOwner && (
                  <div className={classes.itemActions}>
                    <button
                      onClick={() => {
                        setSelectedEducation(edu);
                        setShowModal(true);
                      }}
                      title="Edit"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                        <path d="M362.7 19.3L314.3 67.7 444.3 197.7l48.4-48.4c25-25 25-65.5 0-90.5L453.3 19.3c-25-25-65.5-25-90.5 0zm-71 71L58.6 323.5c-10.4 10.4-18 23.3-22.2 37.4L1 481.2C-1.5 489.7 .8 498.8 7 505s15.3 8.5 23.7 6.1l120.3-35.4c14.1-4.2 27-11.8 37.4-22.2L421.7 220.3 291.7 90.3z" />
                      </svg>
                    </button>
                    <button
                      className={classes.deleteBtn}
                      onClick={() => handleDelete(edu.id)}
                      title="Delete"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 384 512">
                        <path d="M342.6 150.6c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0L192 210.7 86.6 105.4c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L146.7 256 41.4 361.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0L192 301.3 297.4 406.6c12.5 12.5 32.8 12.5 45.3 0s12.5-32.8 0-45.3L237.3 256 342.6 150.6z" />
                      </svg>
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
