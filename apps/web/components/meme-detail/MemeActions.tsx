"use client";

import { useState, useCallback } from "react";
import { DownloadIcon, CopyIcon } from "@openmeme/ui";
import styles from "../../app/[locale]/memes/[category]/[slug]/page.module.css";

type Props = {
  imageUrl: string | null;
  downloadLabel: string;
  copyLabel: string;
  copiedLabel: string;
};

export function MemeActions({
  imageUrl,
  downloadLabel,
  copyLabel,
  copiedLabel,
}: Props) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    if (!imageUrl) return;
    try {
      await navigator.clipboard.writeText(imageUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback for older browsers
      const textarea = document.createElement("textarea");
      textarea.value = imageUrl;
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      try {
        document.execCommand("copy");
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch {
        // silently fail
      }
      document.body.removeChild(textarea);
    }
  }, [imageUrl]);

  return (
    <div className={styles.actions}>
      {imageUrl ? (
        <a
          href={imageUrl}
          target="_blank"
          rel="noopener noreferrer"
          className={styles.btnDownload}
          download
        >
          <DownloadIcon size={14} />
          {downloadLabel}
        </a>
      ) : null}
      {imageUrl ? (
        <button
          type="button"
          className={styles.btnSecondary}
          onClick={handleCopy}
          aria-label={copyLabel}
        >
          <CopyIcon size={14} />
          {copied ? copiedLabel : copyLabel}
        </button>
      ) : null}
    </div>
  );
}
