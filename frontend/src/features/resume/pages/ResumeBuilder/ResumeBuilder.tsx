import { useState, useEffect } from "react";
import { useAuthentication } from "../../../../features/authentication/contexts/AuthenticationContextProvider";
import { request } from "../../../../utils/api";
import classes from "./ResumeBuilder.module.scss";

interface ResumeResult {
  texFileUrl: string | null;
  pdfFileUrl: string | null;
  status: "PDF_READY" | "TEX_ONLY" | "NOT_FOUND";
  message: string;
}

const BASE_URL = import.meta.env.VITE_API_URL;

export function ResumeBuilder() {
  const { user } = useAuthentication();
  const [status, setStatus] = useState<"idle" | "loading" | "done" | "error">(
    "idle"
  );
  const [result, setResult] = useState<ResumeResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [isCheckingExisting, setIsCheckingExisting] = useState<boolean>(true);

  const profileIncomplete =
    !user?.firstName ||
    !user?.lastName ||
    !user?.position ||
    !user?.company ||
    !user?.location;

  useEffect(() => {
    setIsCheckingExisting(true);
    request<ResumeResult>({
      endpoint: "/api/v1/resume/latest",
      method: "GET",
      onSuccess: (data) => {
        setIsCheckingExisting(false);
        if (
          data &&
          data.status !== "NOT_FOUND" &&
          (data.pdfFileUrl || data.texFileUrl)
        ) {
          setResult(data);
          setStatus("done");
        } else {
          setStatus("idle");
        }
      },
      onFailure: () => {
        setIsCheckingExisting(false);
        setStatus("idle");
      },
    });
  }, [user?.id]);

  const handleGenerate = () => {
    setStatus("loading");
    setErrorMessage("");
    setResult(null);

    request<ResumeResult>({
      endpoint: "/api/v1/resume/generate",
      method: "POST",
      onSuccess: (data) => {
        setResult(data);
        setStatus("done");
      },
      onFailure: (error) => {
        setErrorMessage(error);
        setStatus("error");
      },
    });
  };

  return (
    <div className={classes.root}>
      <div className={classes.container}>
        {/* ── Header ───────────────────────────────────── */}
        <div className={classes.header}>
          <div className={classes.headerIcon}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              width="32"
              height="32"
            >
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm-1 1.5L18.5 9H13V3.5zM6 20V4h5v7h7v9H6z" />
              <path d="M8 12h8v1H8zm0 3h8v1H8zm0 3h5v1H8z" />
            </svg>
          </div>
          <div>
            <h1 className={classes.title}>AI Resume Builder</h1>
            <p className={classes.subtitle}>
              Generate a professional LaTeX resume from your profile — powered
              by Google Gemini AI
            </p>
          </div>
        </div>

        {/* ── Profile Incomplete Warning ────────────────── */}
        {profileIncomplete && (
          <div className={classes.warning}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              width="20"
              height="20"
            >
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
            </svg>
            <p>
              <strong>Improve your resume:</strong> Your profile is incomplete.
              Add your{" "}
              <strong>
                name, position, company, location, experience, and skills
              </strong>{" "}
              for a much better result.
            </p>
          </div>
        )}

        {/* ── Checking for existing resume ─────────────── */}
        {isCheckingExisting && (
          <div className={classes.loadingState}>
            <div className={classes.spinner} />
            <div className={classes.loadingText}>
              <p className={classes.loadingTitle}>Loading your resume...</p>
              <p className={classes.loadingSubtitle}>
                Checking for your previously generated resume...
              </p>
            </div>
          </div>
        )}

        {!isCheckingExisting && (
          <>
            {/* ── How It Works ─────────────────────────────── */}
            {status === "idle" && (
          <div className={classes.howItWorks}>
            <h2>How it works</h2>
            <div className={classes.steps}>
              <div className={classes.step}>
                <span className={classes.stepNumber}>1</span>
                <div>
                  <strong>Profile Analysis</strong>
                  <p>
                    We read your experience, education, skills, and projects
                    from your profile.
                  </p>
                </div>
              </div>
              <div className={classes.step}>
                <span className={classes.stepNumber}>2</span>
                <div>
                  <strong>AI Generation</strong>
                  <p>
                    Google Gemini AI crafts a polished resume using Jake's
                    Overleaf template.
                  </p>
                </div>
              </div>
              <div className={classes.step}>
                <span className={classes.stepNumber}>3</span>
                <div>
                  <strong>Download</strong>
                  <p>
                    Download as PDF directly, or as a .tex file to edit on
                    Overleaf.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ── Generate Button ───────────────────────────── */}
        {(status === "idle" || status === "error") && (
          <button
            className={classes.generateBtn}
            onClick={handleGenerate}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              width="20"
              height="20"
            >
              <path d="M12 2a10 10 0 100 20A10 10 0 0012 2zm-1 14.5v-9l6 4.5-6 4.5z" />
            </svg>
            Generate My Resume
          </button>
        )}

        {/* ── Error ─────────────────────────────────────── */}
        {status === "error" && errorMessage && (
          <div className={classes.error}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
              width="18"
              height="18"
            >
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
            </svg>
            <p>{errorMessage}</p>
          </div>
        )}

        {/* ── Loading ───────────────────────────────────── */}
        {status === "loading" && (
          <div className={classes.loadingState}>
            <div className={classes.spinner} />
            <div className={classes.loadingText}>
              <p className={classes.loadingTitle}>
                Generating your resume...
              </p>
              <p className={classes.loadingSubtitle}>
                Gemini AI is crafting your LaTeX resume. This may take 10–20
                seconds.
              </p>
            </div>
          </div>
        )}

        {/* ── Success Result ────────────────────────────── */}
        {status === "done" && result && (
          <div className={classes.result}>
            {/* Status badge */}
            <div
              className={`${classes.statusBadge} ${
                result.status === "PDF_READY"
                  ? classes.badgeSuccess
                  : classes.badgeWarning
              }`}
            >
              {result.status === "PDF_READY" ? (
                <>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    width="16"
                    height="16"
                  >
                    <path d="M9 16.2l-3.5-3.5L4 14.2l5 5 10-10-1.4-1.4z" />
                  </svg>
                  PDF Ready!
                </>
              ) : (
                <>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    width="16"
                    height="16"
                  >
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
                  </svg>
                  LaTeX Generated
                </>
              )}
            </div>

            <p className={classes.resultMessage}>{result.message}</p>

            {/* PDF Preview (when PDF is available) */}
            {result.pdfFileUrl && (
              <div className={classes.pdfPreview}>
                <h3>Preview</h3>
                <iframe
                  src={`${BASE_URL}${result.pdfFileUrl}`}
                  title="Resume Preview"
                  className={classes.pdfFrame}
                />
              </div>
            )}

            {/* Download Buttons */}
            <div className={classes.downloadButtons}>
              {/* Always show Download Resume — PDF if available, otherwise the .tex file */}
              <a
                href={`${BASE_URL}${result.pdfFileUrl ?? result.texFileUrl}`}
                download={result.pdfFileUrl ? "resume.pdf" : "resume.tex"}
                className={classes.downloadPdfBtn}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                  width="18"
                  height="18"
                >
                  <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" />
                </svg>
                Download Resume
              </a>

              {/* Always show .tex download */}
              <a
                href={`${BASE_URL}${result.texFileUrl}`}
                download="resume.tex"
                className={classes.downloadTexBtn}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                  width="18"
                  height="18"
                >
                  <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" />
                </svg>
                Download .tex
              </a>
            </div>

            {/* Regenerate button */}
            <button className={classes.regenerateBtn} onClick={handleGenerate}>
              Regenerate Resume
            </button>
          </div>
        )}
          </>
        )}
      </div>
    </div>
  );
}
