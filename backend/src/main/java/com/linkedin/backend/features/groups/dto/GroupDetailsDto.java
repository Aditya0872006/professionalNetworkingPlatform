package com.linkedin.backend.features.groups.dto;

import com.linkedin.backend.features.groups.model.Group;
import com.linkedin.backend.features.groups.model.GroupMemberRole;
import com.linkedin.backend.features.groups.model.GroupMemberStatus;

public class GroupDetailsDto {
    private Group group;
    private long memberCount;
    private boolean isMember;
    private boolean isAdmin;
    private GroupMemberStatus memberStatus; // null if not a member

    public GroupDetailsDto(Group group, long memberCount, boolean isMember, boolean isAdmin, GroupMemberStatus memberStatus) {
        this.group = group;
        this.memberCount = memberCount;
        this.isMember = isMember;
        this.isAdmin = isAdmin;
        this.memberStatus = memberStatus;
    }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public long getMemberCount() { return memberCount; }
    public void setMemberCount(long memberCount) { this.memberCount = memberCount; }

    public boolean isMember() { return isMember; }
    public void setMember(boolean member) { isMember = member; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public GroupMemberStatus getMemberStatus() { return memberStatus; }
    public void setMemberStatus(GroupMemberStatus memberStatus) { this.memberStatus = memberStatus; }
}
