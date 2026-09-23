import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Input } from "../../../../components/Input/Input";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { request } from "../../../../utils/api";
import { Box } from "../../components/Box/Box";
import { Seperator } from "../../components/Seperator/Seperator";
import { useAuthentication } from "../../contexts/AuthenticationContextProvider";
import { useOauth } from "../../hooks/useOauth";
import classes from "./Login.module.scss";

type RoleOption = "ROLE_USER" | "ROLE_RECRUITER" | "ROLE_ADMIN";

interface IRecruiterStatusNotice {
  status: "PENDING" | "APPROVED" | "REJECTED" | "PERMANENTLY_REJECTED";
  rejectionCount: number;
  canReapply: boolean;
}

export function Login() {
  const [selectedRole, setSelectedRole] = useState<RoleOption>("ROLE_USER");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [recruiterNotice, setRecruiterNotice] = useState<IRecruiterStatusNotice | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isReapplying, setIsReapplying] = useState(false);

  const { login } = useAuthentication();
  const location = useLocation();
  const navigate = useNavigate();
  const { isOauthInProgress, oauthError, startOauth } = useOauth("login");
  usePageTitle("Login");

  const doLogin = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage("");
    setSuccessMessage("");
    setRecruiterNotice(null);

    const email = e.currentTarget.email.value;
    const password = e.currentTarget.password.value;

    try {
      const response = await login(email, password, selectedRole);

      if (response.role === "ROLE_RECRUITER" && response.recruiterStatus) {
        if (response.recruiterStatus === "PENDING") {
          setRecruiterNotice({
            status: "PENDING",
            rejectionCount: response.rejectionCount ?? 0,
            canReapply: false,
          });
          return;
        } else if (response.recruiterStatus === "REJECTED") {
          setRecruiterNotice({
            status: "REJECTED",
            rejectionCount: response.rejectionCount ?? 0,
            canReapply: response.canReapply ?? true,
          });
          return;
        } else if (response.recruiterStatus === "PERMANENTLY_REJECTED") {
          setRecruiterNotice({
            status: "PERMANENTLY_REJECTED",
            rejectionCount: response.rejectionCount ?? 5,
            canReapply: false,
          });
          return;
        }
      }

      if (selectedRole === "ROLE_ADMIN") {
        navigate("/admin");
      } else if (selectedRole === "ROLE_RECRUITER") {
        navigate("/recruiter/jobs");
      } else {
        const destination = location.state?.from || "/";
        navigate(destination);
      }
    } catch (error) {
      if (error instanceof Error) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("An unknown error occurred.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleReapply = async () => {
    setIsReapplying(true);
    setErrorMessage("");
    setSuccessMessage("");
    await request({
      endpoint: "/api/v1/recruiter/reapply",
      method: "POST",
      onSuccess: () => {
        setSuccessMessage("Your reapplication has been submitted successfully and is now pending admin review.");
        setRecruiterNotice({
          status: "PENDING",
          rejectionCount: recruiterNotice?.rejectionCount ?? 1,
          canReapply: false,
        });
      },
      onFailure: (err) => {
        setErrorMessage(err);
      },
    });
    setIsReapplying(false);
  };

  if (isOauthInProgress) {
    return <Loader isInline />;
  }

  return (
    <div className={classes.root}>
      <Box>
        <h1>Sign in</h1>
        <p>Stay updated on your professional world.</p>

        <div className={classes.roleSelector}>
          <button
            type="button"
            className={selectedRole === "ROLE_USER" ? classes.activeRole : ""}
            onClick={() => {
              setSelectedRole("ROLE_USER");
              setErrorMessage("");
              setRecruiterNotice(null);
            }}
          >
            User
          </button>
          <button
            type="button"
            className={selectedRole === "ROLE_RECRUITER" ? classes.activeRole : ""}
            onClick={() => {
              setSelectedRole("ROLE_RECRUITER");
              setErrorMessage("");
              setRecruiterNotice(null);
            }}
          >
            Recruiter
          </button>
          <button
            type="button"
            className={selectedRole === "ROLE_ADMIN" ? classes.activeRole : ""}
            onClick={() => {
              setSelectedRole("ROLE_ADMIN");
              setErrorMessage("");
              setRecruiterNotice(null);
            }}
          >
            Admin
          </button>
        </div>

        {recruiterNotice && recruiterNotice.status === "PENDING" && (
          <div className={`${classes.statusBox} ${classes.pending}`}>
            <strong>Account Pending Review:</strong> Your recruiter registration is currently under review by our administrators. You will be able to access the recruiter features once your profile is approved.
          </div>
        )}

        {recruiterNotice && recruiterNotice.status === "REJECTED" && (
          <div className={`${classes.statusBox} ${classes.rejected}`}>
            <strong>Application Needs Attention:</strong> Your recruiter application was not approved (Rejection {recruiterNotice.rejectionCount}/5).
            {recruiterNotice.canReapply && (
              <div>
                <p>You can reapply for administrative reconsideration below.</p>
                <button
                  type="button"
                  className={classes.reapplyBtn}
                  onClick={handleReapply}
                  disabled={isReapplying}
                >
                  {isReapplying ? "Submitting..." : "Reapply for Approval"}
                </button>
              </div>
            )}
          </div>
        )}

        {recruiterNotice && recruiterNotice.status === "PERMANENTLY_REJECTED" && (
          <div className={`${classes.statusBox} ${classes.permanentlyRejected}`}>
            <strong>Application Permanently Closed:</strong> Your recruiter application has reached the maximum of 5 rejections. New applications from this account are permanently disallowed.
          </div>
        )}

        {successMessage && <p className={classes.success}>{successMessage}</p>}
        {errorMessage && <p className={classes.error}>{errorMessage}</p>}

        <form onSubmit={doLogin}>
          <Input label="Email" type="email" id="email" onFocus={() => setErrorMessage("")} />
          <Input
            label="Password"
            type="password"
            id="password"
            onFocus={() => setErrorMessage("")}
          />

          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Signing in..." : `Sign in as ${selectedRole === "ROLE_ADMIN" ? "Admin" : selectedRole === "ROLE_RECRUITER" ? "Recruiter" : "User"}`}
          </Button>
          <Link to="/authentication/request-password-reset">Forgot password?</Link>
        </form>

        {selectedRole === "ROLE_USER" && (
          <>
            <Seperator>Or</Seperator>
            <div className={classes.register}>
              {oauthError && <p className={classes.error}>{oauthError}</p>}
              <Button
                outline
                onClick={() => {
                  startOauth();
                }}
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 488 512">
                  <path d="M488 261.8C488 403.3 391.1 504 248 504 110.8 504 0 393.2 0 256S110.8 8 248 8c66.8 0 123 24.5 166.3 64.9l-67.5 64.9C258.5 52.6 94.3 116.6 94.3 256c0 86.5 69.1 156.6 153.7 156.6 98.2 0 135-70.4 140.8-106.9H248v-85.3h236.1c2.3 12.7 3.9 24.9 3.9 41.4z" />
                </svg>
                Continue with Google
              </Button>
              New User? <Link to="/authentication/signup">Join now</Link>
            </div>
          </>
        )}

        {selectedRole === "ROLE_RECRUITER" && (
          <div className={classes.register} style={{ marginTop: "1rem" }}>
            Need a Recruiter account? <Link to="/authentication/signup?role=recruiter">Register as Recruiter</Link>
          </div>
        )}
      </Box>
    </div>
  );
}
