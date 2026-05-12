"use client";

import { useState, useCallback } from "react";
import { CopyIcon } from "@openmeme/ui";

type Props = {
  text: string;
  className?: string;
  ariaLabel: string;
};

export function CopyButton({ text, className, ariaLabel }: Props) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      const textarea = document.createElement("textarea");
      textarea.value = text;
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
  }, [text]);

  return (
    <button
      type="button"
      className={className}
      onClick={handleCopy}
      aria-label={copied ? `${ariaLabel} — copied` : ariaLabel}
      title={ariaLabel}
    >
      <CopyIcon size={14} />
    </button>
  );
}
