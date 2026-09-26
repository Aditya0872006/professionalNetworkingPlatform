import { FormEvent, useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "../../../../components/Button/Button";
import { Input } from "../../../../components/Input/Input";
import { Loader } from "../../../../components/Loader/Loader";
import { usePageTitle } from "../../../../hooks/usePageTitle";
import { Box } from "../../components/Box/Box";
import { Seperator } from "../../components/Seperator/Seperator";
import { useAuthentication } from "../../contexts/AuthenticationContextProvider";
import { useOauth } from "../../hooks/useOauth";
import classes from "./Signup.module.scss";

type SignupType = "user" | "recruiter";

export function Signup() {
  const [searchParams] = useSearchParams();
  const [accountType, setAccountType] = useState<SignupType>(
    searchParams.get("role") === "recruiter" ? "recruiter" : "user"
  );
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [recruiterPendingSuccess, setRecruiterPendingSuccess] = useState(false);

  const { signup, signupRecruiter } = useAuthentication();
  const navigate = useNavigate();
  usePageTitle("Sign Up");
  const { isOauthInProgress, oauthError, startOauth } = useOauth("signup");

  useEffect(() => {
    if (searchParams.get("role") === "recruiter") {
      setAccountType("recruiter");
    }
  }, [searchParams]);

  // Real-time password criteria verification
  const hasMinLength = password.length >= 8;
  const hasLowercase = /[a-z]/.test(password);
  const hasUppercase = /[A-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecial = /[@#$%^&+=!_.-]/.test(password);
  const isPasswordValid = hasMinLength && hasLowercase && hasUppercase && hasNumber && hasSpecial;

  const doSignup = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage("");

    if (!isPasswordValid) {
      setErrorMessage("Please fulfill all password strength requirements before proceeding.");
      return;
    }

    setIsLoading(true);
    const form = e.currentTarget;

    try {
      if (accountType === "user") {
        const email = (form.elements.namedItem("email") as HTMLInputElement).value;
        await signup(email, password);
        navigate("/");
      } else {
        const email = (form.elements.namedItem("email") as HTMLInputElement).value;
        const firstName = (form.elements.namedItem("firstName") as HTMLInputElement).value;
        const lastName = (form.elements.namedItem("lastName") as HTMLInputElement).value;
        const companyName = (form.elements.namedItem("companyName") as HTMLInputElement).value;
        const companyEmail = (form.elements.namedItem("companyEmail") as HTMLInputElement)?.value;
        const companyWebsite = (form.elements.namedItem("companyWebsite") as HTMLInputElement)?.value;
        const companyLocation = (form.elements.namedItem("companyLocation") as HTMLInputElement)?.value;
        const companyDescription = (form.elements.namedItem("companyDescription") as HTMLTextAreaElement)?.value;
        const position = (form.elements.namedItem("position") as HTMLInputElement)?.value;

        if (!firstName || !lastName || !companyName) {
          setErrorMessage("Please complete all required recruiter and company fields.");
          setIsLoading(false);
          return;
        }

        await signupRecruiter({
          email,
          password,
          firstName,
          lastName,
          companyName,
          companyEmail,
          companyWebsite,
          companyDescription,
          companyLocation,
          position: position || "Recruiter",
          location: companyLocation,
        });

        setRecruiterPendingSuccess(true);
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

  if (isOauthInProgress) {
    return <Loader isInline />;
  }

  if (recruiterPendingSuccess) {
    return (
      <div className={classes.root}>
        <Box>
          <div className={classes.pendingSuccessCard}>
            <div className={classes.iconCircle}>✓</div>
            <h2>Recruiter Application Submitted!</h2>
            <p>
              Thank you for registering. Your recruiter account has been created and is currently <strong>PENDING</strong> administrative review.
              Our platform administrators will verify your company details and review your application.
            </p>
            <p style={{ fontSize: "0.85rem", color: "#777" }}>
              Once approved, you will be able to post job openings, manage applicants, and review candidate profiles.
            </p>
            <Button
              type="button"
              onClick={() => navigate("/authentication/login")}
            >
              I understand
            </Button>
          </div>
        </Box>
      </div>
    );
  }

  return (
    <div className={classes.root}>
      <Box>
        <h1>Sign up</h1>
        <p>Make the most of your professional life.</p>

        <div className={classes.roleToggle}>
          <button
            type="button"
            className={accountType === "user" ? classes.activeRole : ""}
            onClick={() => {
              setAccountType("user");
              setErrorMessage("");
            }}
          >
            Join as User
          </button>
          <button
            type="button"
            className={accountType === "recruiter" ? classes.activeRole : ""}
            onClick={() => {
              setAccountType("recruiter");
              setErrorMessage("");
            }}
          >
            Join as Recruiter
          </button>
        </div>

        {errorMessage && <p className={classes.error}>{errorMessage}</p>}

        <form onSubmit={doSignup}>
          {accountType === "recruiter" && (
            <>
              <div className={classes.sectionTitle}>Recruiter Details</div>
              <div className={classes.formRow}>
                <Input
                  type="text"
                  id="firstName"
                  name="firstName"
                  label="First Name *"
                  required
                  onFocus={() => setErrorMessage("")}
                />
                <Input
                  type="text"
                  id="lastName"
                  name="lastName"
                  label="Last Name *"
                  required
                  onFocus={() => setErrorMessage("")}
                />
              </div>
              <Input
                type="text"
                id="position"
                name="position"
                label="Job Title / Role (e.g. Senior Technical Recruiter)"
                onFocus={() => setErrorMessage("")}
              />

              <div className={classes.sectionTitle}>Company Information</div>
              <Input
                type="text"
                id="companyName"
                name="companyName"
                label="Company Name *"
                required
                onFocus={() => setErrorMessage("")}
              />
              <Input
                type="email"
                id="companyEmail"
                name="companyEmail"
                label="Official Company Email"
                onFocus={() => setErrorMessage("")}
              />
              <Input
                type="url"
                id="companyWebsite"
                name="companyWebsite"
                label="Company Website (e.g. https://example.com)"
                placeholder="https://"
                onFocus={() => setErrorMessage("")}
              />
              <Input
                type="text"
                id="companyLocation"
                name="companyLocation"
                label="Company Headquarters / Location"
                placeholder="e.g. San Francisco, CA"
                onFocus={() => setErrorMessage("")}
              />
              <div className={classes.textareaGroup}>
                <label htmlFor="companyDescription">Company Description</label>
                <textarea
                  id="companyDescription"
                  name="companyDescription"
                  rows={2}
                  placeholder="Briefly describe what your organization does..."
                  onFocus={() => setErrorMessage("")}
                />
              </div>

              <div className={classes.sectionTitle}>Account Credentials</div>
            </>
          )}

          <Input
            type="email"
            id="email"
            name="email"
            label="Account Email *"
            required
            onFocus={() => setErrorMessage("")}
          />

          <Input
            label="Password *"
            type="password"
            id="password"
            name="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            onFocus={() => setErrorMessage("")}
          />

          <div className={classes.passwordCriteria}>
            <div className={classes.criteriaTitle}>Password Requirements:</div>
            <ul>
              <li className={hasMinLength ? classes.met : ""}>
                <span className={classes.icon}>{hasMinLength ? "✓" : "○"}</span>
                At least 8 characters
              </li>
              <li className={hasLowercase ? classes.met : ""}>
                <span className={classes.icon}>{hasLowercase ? "✓" : "○"}</span>
                1 lowercase letter
              </li>
              <li className={hasUppercase ? classes.met : ""}>
                <span className={classes.icon}>{hasUppercase ? "✓" : "○"}</span>
                1 uppercase letter
              </li>
              <li className={hasNumber ? classes.met : ""}>
                <span className={classes.icon}>{hasNumber ? "✓" : "○"}</span>
                1 number
              </li>
              <li className={hasSpecial ? classes.met : ""}>
                <span className={classes.icon}>{hasSpecial ? "✓" : "○"}</span>
                1 special character (@#$%^&+=!_.-)
              </li>
            </ul>
          </div>

          <p className={classes.disclaimer}>
            By clicking Agree & Join or Continue, you agree to platform's{" "}
            <a href="#terms">User Agreement</a>, <a href="#privacy">Privacy Policy</a>, and{" "}
            <a href="#cookies">Cookie Policy</a>.
          </p>

          <Button disabled={isLoading || (password.length > 0 && !isPasswordValid)} type="submit">
            {isLoading ? "Submitting..." : accountType === "recruiter" ? "Submit Recruiter Application" : "Agree & Join"}
          </Button>
        </form>

        {accountType === "user" && (
          <>
            <Seperator>Or</Seperator>
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
          </>
        )}

        <div className={classes.register}>
          Already registered? <Link to="/authentication/login">Sign in</Link>
        </div>
      </Box>
    </div>
  );
}
