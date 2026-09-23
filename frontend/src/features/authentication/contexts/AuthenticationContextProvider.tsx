import { createContext, Dispatch, SetStateAction, useContext, useEffect, useState } from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { Loader } from "../../../components/Loader/Loader";
import { request } from "../../../utils/api";

export interface IAuthenticationResponse {
  token?: string;
  message?: string;
  role?: "ROLE_USER" | "ROLE_RECRUITER" | "ROLE_ADMIN";
  recruiterStatus?: "PENDING" | "APPROVED" | "REJECTED" | "PERMANENTLY_REJECTED";
  rejectionCount?: number;
  canReapply?: boolean;
}

export interface IUser {
  id: string;
  email: string;
  emailVerified: boolean;
  role?: "ROLE_USER" | "ROLE_RECRUITER" | "ROLE_ADMIN";
  status?: "ACTIVE" | "BLOCKED";
  firstName?: string;
  lastName?: string;
  company?: string;
  position?: string;
  location?: string;
  profileComplete: boolean;
  profilePicture?: string;
  coverPicture?: string;
  about?: string;
}

export interface IRecruiterRegistrationData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  companyName: string;
  companyEmail?: string;
  companyWebsite?: string;
  companyDescription?: string;
  companyLocation?: string;
  position?: string;
  location?: string;
}

interface IAuthenticationContextType {
  user: IUser | null;
  setUser: Dispatch<SetStateAction<IUser | null>>;
  login: (email: string, password: string, role?: string) => Promise<IAuthenticationResponse>;
  logout: () => void;
  signup: (email: string, password: string) => Promise<void>;
  signupRecruiter: (data: IRecruiterRegistrationData) => Promise<IAuthenticationResponse>;
  ouathLogin: (code: string, page: "login" | "signup") => Promise<void>;
}

const AuthenticationContext = createContext<IAuthenticationContextType | null>(null);

export function useAuthentication() {
  return useContext(AuthenticationContext)!;
}

export function AuthenticationContextProvider() {
  const location = useLocation();
  const [user, setUser] = useState<IUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isOnAuthPage =
    location.pathname === "/authentication/login" ||
    location.pathname === "/authentication/signup" ||
    location.pathname === "/authentication/request-password-reset";

  const login = async (email: string, password: string, role?: string): Promise<IAuthenticationResponse> => {
    let result: IAuthenticationResponse = {};
    await request<IAuthenticationResponse>({
      endpoint: "/api/v1/authentication/login",
      method: "POST",
      body: JSON.stringify({ email, password, role }),
      onSuccess: (data) => {
        result = data;
        if (data.token) {
          localStorage.setItem("token", data.token);
        }
      },
      onFailure: (error) => {
        throw new Error(error);
      },
    });
    return result;
  };

  const signupRecruiter = async (data: IRecruiterRegistrationData): Promise<IAuthenticationResponse> => {
    let result: IAuthenticationResponse = {};
    await request<IAuthenticationResponse>({
      endpoint: "/api/v1/authentication/register-recruiter",
      method: "POST",
      body: JSON.stringify(data),
      onSuccess: (resp) => {
        result = resp;
        if (resp.token) {
          localStorage.setItem("token", resp.token);
        }
      },
      onFailure: (error) => {
        throw new Error(error);
      },
    });
    return result;
  };

  const ouathLogin = async (code: string, page: "login" | "signup") => {
    await request<IAuthenticationResponse>({
      endpoint: "/api/v1/authentication/oauth/google/login",
      method: "POST",
      body: JSON.stringify({ code, page }),
      onSuccess: ({ token }) => {
        if (token) {
          localStorage.setItem("token", token);
        }
      },
      onFailure: (error) => {
        throw new Error(error);
      },
    });
  };

  const signup = async (email: string, password: string) => {
    await request<IAuthenticationResponse>({
      endpoint: "/api/v1/authentication/register",
      method: "POST",
      body: JSON.stringify({ email, password }),
      onSuccess: ({ token }) => {
        if (token) {
          localStorage.setItem("token", token);
        }
      },
      onFailure: (error) => {
        throw new Error(error);
      },
    });
  };

  const logout = async () => {
    localStorage.removeItem("token");
    setUser(null);
  };

  useEffect(() => {
    if (user) {
      return;
    }
    setIsLoading(true);
    const fetchUser = async () => {
      await request<IUser>({
        endpoint: "/api/v1/authentication/users/me",
        onSuccess: (data) => setUser(data),
        onFailure: (error) => {
          console.log(error);
        },
      });
      setIsLoading(false);
    };

    fetchUser();
  }, [user, location.pathname]);

  if (isLoading) {
    return <Loader />;
  }

  if (!isLoading && !user && !isOnAuthPage) {
    return <Navigate to="/authentication/login" state={{ from: location.pathname }} />;
  }

  if (user && !user.emailVerified && location.pathname !== "/authentication/verify-email") {
    return <Navigate to="/authentication/verify-email" />;
  }

  if (user && user.emailVerified && location.pathname == "/authentication/verify-email") {
    console.log("here1");
    return <Navigate to="/" />;
  }

  if (
    user &&
    user.emailVerified &&
    !user.profileComplete &&
    (user.role === "ROLE_USER" || !user.role) &&
    !location.pathname.includes("/authentication/profile")
  ) {
    return <Navigate to={`/authentication/profile/${user.id}`} />;
  }

  if (
    user &&
    user.emailVerified &&
    user.profileComplete &&
    location.pathname.includes("/authentication/profile")
  ) {
    console.log("here2");
    return <Navigate to="/" />;
  }

  if (user && isOnAuthPage) {
    return <Navigate to={location.state?.from || "/"} />;
  }

  return (
    <AuthenticationContext.Provider
      value={{
        user,
        login,
        logout,
        signup,
        signupRecruiter,
        setUser,
        ouathLogin,
      }}
    >
      <Outlet />
    </AuthenticationContext.Provider>
  );
}
