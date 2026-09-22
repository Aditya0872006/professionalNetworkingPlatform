import { useEffect, useState } from "react";
import { request } from "../../../../utils/api";
import { IExperience } from "../../types/profile";
import classes from "./Experience.module.scss";
import { ExperienceModal } from "./ExperienceModal";

interface ExperienceProps {
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

export function Experience({ userId, isOwner }: ExperienceProps) {
  const [experiences, setExperiences] = useState<IExperience[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedExperience, setSelectedExperience] = useState<IExperience | null>(null);

  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    request<IExperience[]>({
      endpoint: `/api/v1/profile/users/${userId}/experience`,
      onSuccess: (data) => {
        setExperiences(data || []);
        setLoading(false);
      },
      onFailure: (err) => {
        console.error("Failed to load experiences:", err);
        setLoading(false);
      },
    });
  }, [userId]);

  const handleCreate = async (data: Omit<IExperience, "id">) => {
    await request<IExperience>({
      endpoint: "/api/v1/profile/experience",
      method: "POST",
      body: JSON.stringify(data),
      onSuccess: (saved) => {
        setExperiences((prev) => [saved, ...prev]);
      },
      onFailure: (err) => console.error("Failed to add experience:", err),
    });
  };

  const handleUpdate = async (data: Omit<IExperience, "id">) => {
    if (!selectedExperience) return;
    await request<IExperience>({
      endpoint: `/api/v1/profile/experience/${selectedExperience.id}`,
      method: "PUT",
      body: JSON.stringify(data),
      onSuccess: (updated) => {
        setExperiences((prev) =>
          prev.map((exp) => (exp.id === updated.id ? updated : exp))
        );
      },
      onFailure: (err) => console.error("Failed to update experience:", err),
    });
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this experience entry?")) return;
    await request<void>({
      endpoint: `/api/v1/profile/experience/${id}`,
      method: "DELETE",
      onSuccess: () => {
        setExperiences((prev) => prev.filter((exp) => exp.id !== id));
      },
      onFailure: (err) => console.error("Failed to delete experience:", err),
    });
  };

  return (
    <div className={classes.experience}>
      {showModal && (
        <ExperienceModal
          show={showModal}
          onClose={() => {
            setShowModal(false);
            setSelectedExperience(null);
          }}
          onSubmit={selectedExperience ? handleUpdate : handleCreate}
          existing={selectedExperience}
        />
      )}

      <div className={classes.header}>
        <h2>Experience</h2>
        {isOwner && (
          <button
            onClick={() => {
              setSelectedExperience(null);
              setShowModal(true);
            }}
            title="Add Experience"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
              <path d="M256 80c0-17.7-14.3-32-32-32s-32 14.3-32 32l0 144L48 256c-17.7 0-32 14.3-32 32s14.3 32 32 32l144 0 0 144c0 17.7 14.3 32 32 32s32-14.3 32-32l0-144 144 0c17.7 0 32-14.3 32-32s-14.3-32-32-32l-144 0 0-144z" />
            </svg>
          </button>
        )}
      </div>

      {loading ? (
        <p className={classes.empty}>Loading experience...</p>
      ) : experiences.length === 0 ? (
        <p className={classes.empty}>No experience listed yet.</p>
      ) : (
        <div className={classes.list}>
          {experiences.map((exp) => {
            const start = formatDate(exp.startDate);
            const end = exp.endDate ? formatDate(exp.endDate) : "Present";
            const dateRange = start ? `${start} – ${end}` : end !== "Present" ? end : "";

            return (
              <div key={exp.id} className={classes.item}>
                <div className={classes.info}>
                  <h3>{exp.jobTitle}</h3>
                  <div className={classes.company}>{exp.companyName}</div>
                  {dateRange && <div className={classes.dates}>{dateRange}</div>}
                  {exp.description && <p className={classes.description}>{exp.description}</p>}
                </div>
                {isOwner && (
                  <div className={classes.itemActions}>
                    <button
                      onClick={() => {
                        setSelectedExperience(exp);
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
                      onClick={() => handleDelete(exp.id)}
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
