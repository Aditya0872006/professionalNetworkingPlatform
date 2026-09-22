import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import {
  IUser,
  useAuthentication,
} from "../../../authentication/contexts/AuthenticationContextProvider";
import { RightSidebar } from "../../../feed/components/RightSidebar/RightSidebar";
import { About } from "../../components/About/About";
import { Activity } from "../../components/Activity/Activity";
import { Header } from "../../components/Header/Header";
import { Experience } from "../../components/Experience/Experience";
import { Education } from "../../components/Education/Education";
import { Skills } from "../../components/Skills/Skills";
import { Projects } from "../../components/Projects/Projects";
import classes from "./Profile.module.scss";

export function Profile() {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const { user: authUser, setUser: setAuthUser } = useAuthentication();
  const [user, setUser] = useState<IUser | null>(null);

  usePageTitle(user ? `${user.firstName} ${user.lastName}` : "Profile");

  useEffect(() => {
    setLoading(true);
    if (id == authUser?.id) {
      setUser(authUser);
      setLoading(false);
    } else {
      request<IUser>({
        endpoint: `/api/v1/authentication/users/${id}`,
        onSuccess: (data) => {
          setUser(data);
          setLoading(false);
        },
        onFailure: (error) => console.log(error),
      });
    }
  }, [authUser, id]);

  if (loading) {
    return <Loader />;
  }

  const isOwner = Boolean(authUser?.id && user?.id && authUser.id === user.id);

  return (
    <div className={classes.profile}>
      <section className={classes.main}>
        <Header user={user} authUser={authUser} onUpdate={(user) => setAuthUser(user)} />
        <About user={user} authUser={authUser} onUpdate={(user) => setAuthUser(user)} />
        <Activity authUser={authUser} user={user} id={id} />

        <Experience userId={user?.id} isOwner={isOwner} />
        <Education userId={user?.id} isOwner={isOwner} />
        <Skills userId={user?.id} isOwner={isOwner} />
        <Projects userId={user?.id} isOwner={isOwner} />
      </section>
      <div className={classes.sidebar}>
        <RightSidebar />
      </div>
    </div>
  );
}

