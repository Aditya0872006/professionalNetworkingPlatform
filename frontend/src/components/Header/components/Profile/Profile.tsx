import { Dispatch, SetStateAction, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthentication } from "../../../../features/authentication/contexts/AuthenticationContextProvider";
import { Button } from "../../../Button/Button";
import classes from "./Profile.module.scss";

interface IProfileProps {
  showProfileMenu: boolean;
  setShowNavigationMenu: Dispatch<SetStateAction<boolean>>;
  setShowProfileMenu: Dispatch<SetStateAction<boolean>>;
}
export function Profile({
  showProfileMenu,
  setShowProfileMenu,
  setShowNavigationMenu,
}: IProfileProps) {
  const { logout, user } = useAuthentication();
  const ref = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setShowProfileMenu(false);
      }
    };

    document.addEventListener("click", handleClick);

    return () => document.removeEventListener("click", handleClick);
  }, [setShowProfileMenu]);

  return (
    <div className={classes.root} ref={ref}>
      <button
        className={classes.toggle}
        onClick={() => {
          setShowProfileMenu((prev) => !prev);
          if (window.innerWidth <= 1080) {
            setShowNavigationMenu(false);
          }
        }}
      >
        <img
          className={classes.avatar}
          src={
            user?.profilePicture
              ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${user?.profilePicture}`
              : "/avatar.svg"
          }
          alt=""
        />
        <div className={classes.name}>
          <div>{user?.firstName + " " + user?.lastName?.charAt(0) + "."}</div>
        </div>
      </button>

      {showProfileMenu ? (
        <div className={classes.menu}>
          <div className={classes.content}>
            <img
              className={`${classes.left} ${classes.avatar}`}
              src={
                user?.profilePicture
                  ? `${import.meta.env.VITE_API_URL}/api/v1/storage/${user?.profilePicture}`
                  : "/avatar.svg"
              }
              alt=""
            />
            <div className={classes.right}>
              <div className={classes.name}>
                {user?.firstName + " " + user?.lastName}
                {user?.role === "ROLE_ADMIN" && (
                  <span style={{ marginLeft: "6px", fontSize: "0.7rem", backgroundColor: "#e7f3fe", color: "#0a66c2", padding: "2px 6px", borderRadius: "10px", fontWeight: "bold" }}>
                    Admin
                  </span>
                )}
                {user?.role === "ROLE_RECRUITER" && (
                  <span style={{ marginLeft: "6px", fontSize: "0.7rem", backgroundColor: "#fef3c7", color: "#b45309", padding: "2px 6px", borderRadius: "10px", fontWeight: "bold" }}>
                    Recruiter
                  </span>
                )}
              </div>
              <div className={classes.title}>
                {user?.position ? `${user.position}${user.company ? ` at ${user.company}` : ""}` : user?.role === "ROLE_ADMIN" ? "Platform Administrator" : ""}
              </div>
            </div>
          </div>
          <div className={classes.links}>
            {user?.role === "ROLE_ADMIN" && (
              <Button
                size="small"
                className={classes.button}
                outline
                onClick={() => {
                  setShowProfileMenu(false);
                  navigate("/admin");
                }}
              >
                Admin Dashboard
              </Button>
            )}
            {user?.role === "ROLE_RECRUITER" && (
              <>
                <Button
                  size="small"
                  className={classes.button}
                  outline
                  onClick={() => {
                    setShowProfileMenu(false);
                    navigate("/recruiter/jobs");
                  }}
                >
                  Manage Jobs
                </Button>
                <Button
                  size="small"
                  className={classes.button}
                  outline
                  onClick={() => {
                    setShowProfileMenu(false);
                    navigate("/recruiter/jobs/create");
                  }}
                >
                  Post a Job
                </Button>
              </>
            )}
            <Button
              size="small"
              className={classes.button}
              outline
              onClick={() => {
                setShowProfileMenu(false);
                navigate("/profile/" + user?.id);
              }}
            >
              View Profile
            </Button>
            <Link
              to="/logout"
              onClick={(e) => {
                e.preventDefault();
                logout();
              }}
            >
              Sign Out
            </Link>
          </div>
        </div>
      ) : null}
    </div>
  );
}
