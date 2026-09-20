import { IUser } from "../../authentication/contexts/AuthenticationContextProvider";

export interface IGroup {
  id: number;
  name: string;
  description?: string;
  coverPicture?: string;
  isPrivate: boolean;
  createdBy: IUser;
  creationDate: string;
}

export interface IGroupMember {
  id: number;
  user: IUser;
  role: "ADMIN" | "MEMBER";
  status: "ACTIVE" | "PENDING" | "BANNED";
  joinedDate: string;
}

export interface IGroupDetails {
  group: IGroup;
  memberCount: number;
  member: boolean;
  admin: boolean;
  memberStatus: "ACTIVE" | "PENDING" | "BANNED" | null;
}

export interface IGroupPost {
  id: number;
  content: string;
  picture?: string;
  author: IUser;
  creationDate: string;
  updatedDate?: string;
}

export interface IGroupPostComment {
  id: number;
  content: string;
  author: IUser;
  creationDate: string;
  updatedDate?: string;
}
