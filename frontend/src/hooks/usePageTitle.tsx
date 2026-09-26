// import { useEffect } from "react";

// export const usePageTitle = (title: string) => {
//   useEffect(() => {
//     document.title = "Professional networking platform | " + title;
//   }, [title]);
// };

import { useEffect } from "react";

export const usePageTitle = (title: string) => {
  useEffect(() => {
    // 1. Set the page title
    document.title = "Professional Networking Platform | " + title;

    // 2. Find the existing favicon link, or create a new one
    let link = document.querySelector<HTMLLinkElement>("link[rel~='icon']");
    if (!link) {
      link = document.createElement("link");
      link.rel = "icon";
      document.head.appendChild(link);
    }
    
    // 3. Set the logo path (pointing to public/logo.svg)
    link.type = "image/svg+xml";
    link.href = "/logo.svg";
    
  }, [title]);
};