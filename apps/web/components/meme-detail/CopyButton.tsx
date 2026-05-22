"use client";

import { CopyIcon } from "@openmeme/ui";
import { useCopyImage } from "./useCopyImage";

type Props = {
  imageUrl: string;
  className?: string;
  ariaLabel: string;
};

export function CopyButton({ imageUrl, className, ariaLabel }: Props) {
  const { state, copy } = useCopyImage(imageUrl);

  return (
    <button
      type="button"
      className={className}
      onClick={copy}
      aria-label={state === "copied" ? `${ariaLabel} — copied` : ariaLabel}
      title={ariaLabel}
    >
      <CopyIcon size={14} />
    </button>
  );
}
