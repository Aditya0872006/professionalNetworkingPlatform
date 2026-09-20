import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Input } from "../../../../components/Input/Input";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { GroupCard } from "../../components/GroupCard/GroupCard";
import { IGroupDetails } from "../../types/groups";
import classes from "./Groups.module.scss";

export function Groups() {
  usePageTitle("Groups");
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [groups, setGroups] = useState<IGroupDetails[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchGroups = async (q: string) => {
    setIsLoading(true);
    await request<IGroupDetails[]>({
      endpoint: `/api/v1/groups${q ? `?query=${encodeURIComponent(q)}` : ""}`,
      onSuccess: setGroups,
      onFailure: console.error,
    });
    setIsLoading(false);
  };

  useEffect(() => {
    fetchGroups("");
  }, []);

  useEffect(() => {
    const timeout = setTimeout(() => fetchGroups(query), 350);
    return () => clearTimeout(timeout);
  }, [query]);

  const handleJoin = (updated: IGroupDetails) => {
    setGroups((prev) =>
      prev.map((g) => (g.group.id === updated.group.id ? updated : g))
    );
  };

  return (
    <div className={classes.root}>
      <div className={classes.header}>
        <h1>Groups</h1>
        <div className={classes.searchBar}>
          <Input
            placeholder="Search groups…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            style={{ marginBlock: 0, flex: 1 }}
          />
        </div>
        <Button size="small" onClick={() => navigate("/groups/create")}>
          + Create Group
        </Button>
      </div>

      {isLoading ? (
        <p className={classes.empty}>Loading…</p>
      ) : groups.length === 0 ? (
        <p className={classes.empty}>No groups found.</p>
      ) : (
        <div className={classes.grid}>
          {groups.map((details) => (
            <GroupCard
              key={details.group.id}
              details={details}
              onJoin={handleJoin}
            />
          ))}
        </div>
      )}
    </div>
  );
}
