import { useEffect, useState } from "react";
import { request } from "../../../../utils/api";
import { IProject } from "../../types/profile";
import { ProjectModal } from "./ProjectModal";
import classes from "./Projects.module.scss";

interface ProjectsProps {
  userId: string | number | undefined;
  isOwner: boolean;
}


export function Projects({ userId, isOwner }: ProjectsProps) {
  const [projects, setProjects] = useState<IProject[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [selectedProject, setSelectedProject] = useState<IProject | null>(null);

  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    request<IProject[]>({
      endpoint: `/api/v1/profile/users/${userId}/projects`,
      onSuccess: (data) => {
        setProjects(data || []);
        setLoading(false);
      },
      onFailure: (err) => {
        console.error("Failed to load projects:", err);
        setLoading(false);
      },
    });
  }, [userId]);

  const handleCreate = async (data: Omit<IProject, "id">) => {
    await request<IProject>({
      endpoint: "/api/v1/profile/projects",
      method: "POST",
      body: JSON.stringify(data),
      onSuccess: (saved) => {
        setProjects((prev) => [saved, ...prev]);
      },
      onFailure: (err) => console.error("Failed to add project:", err),
    });
  };

  const handleUpdate = async (data: Omit<IProject, "id">) => {
    if (!selectedProject) return;
    await request<IProject>({
      endpoint: `/api/v1/profile/projects/${selectedProject.id}`,
      method: "PUT",
      body: JSON.stringify(data),
      onSuccess: (updated) => {
        setProjects((prev) =>
          prev.map((proj) => (proj.id === updated.id ? updated : proj))
        );
      },
      onFailure: (err) => console.error("Failed to update project:", err),
    });
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this project?")) return;
    await request<void>({
      endpoint: `/api/v1/profile/projects/${id}`,
      method: "DELETE",
      onSuccess: () => {
        setProjects((prev) => prev.filter((proj) => proj.id !== id));
      },
      onFailure: (err) => console.error("Failed to delete project:", err),
    });
  };

  return (
    <div className={classes.projects}>
      {showModal && (
        <ProjectModal
          show={showModal}
          onClose={() => {
            setShowModal(false);
            setSelectedProject(null);
          }}
          onSubmit={selectedProject ? handleUpdate : handleCreate}
          existing={selectedProject}
        />
      )}

      <div className={classes.header}>
        <h2>Projects</h2>
        {isOwner && (
          <button
            onClick={() => {
              setSelectedProject(null);
              setShowModal(true);
            }}
            title="Add Project"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
              <path d="M256 80c0-17.7-14.3-32-32-32s-32 14.3-32 32l0 144L48 256c-17.7 0-32 14.3-32 32s14.3 32 32 32l144 0 0 144c0 17.7 14.3 32 32 32s32-14.3 32-32l0-144 144 0c17.7 0 32-14.3 32-32s-14.3-32-32-32l-144 0 0-144z" />
            </svg>
          </button>
        )}
      </div>

      {loading ? (
        <p className={classes.empty}>Loading projects...</p>
      ) : projects.length === 0 ? (
        <p className={classes.empty}>No projects listed yet.</p>
      ) : (
        <div className={classes.list}>
          {projects.map((proj) => (
            <div key={proj.id} className={classes.item}>
              <div className={classes.info}>
                <div className={classes.titleRow}>
                  <h3>{proj.projectName}</h3>
                  {proj.projectUrl && (
                    <a
                      href={proj.projectUrl.startsWith("http") ? proj.projectUrl : `https://${proj.projectUrl}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={classes.projectLink}
                    >
                      <span>View</span>
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                        <path d="M320 0c-17.7 0-32 14.3-32 32s14.3 32 32 32h82.7L201.4 265.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0L448 109.3V192c0 17.7 14.3 32 32 32s32-14.3 32-32V32c0-17.7-14.3-32-32-32H320zM80 32C35.8 32 0 67.8 0 112V432c0 44.2 35.8 80 80 80H400c44.2 0 80-35.8 80-80V320c0-17.7-14.3-32-32-32s-32 14.3-32 32V432c0 8.8-7.2 16-16 16H80c-8.8 0-16-7.2-16-16V112c0-8.8 7.2-16 16-16H192c17.7 0 32-14.3 32-32s-14.3-32-32-32H80z" />
                      </svg>
                    </a>
                  )}
                </div>
                {proj.description && <p className={classes.description}>{proj.description}</p>}
              </div>
              {isOwner && (
                <div className={classes.itemActions}>
                  <button
                    onClick={() => {
                      setSelectedProject(proj);
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
                    onClick={() => handleDelete(proj.id)}
                    title="Delete"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 384 512">
                      <path d="M342.6 150.6c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0L192 210.7 86.6 105.4c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L146.7 256 41.4 361.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0L192 301.3 297.4 406.6c12.5 12.5 32.8 12.5 45.3 0s12.5-32.8 0-45.3L237.3 256 342.6 150.6z" />
                    </svg>
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
