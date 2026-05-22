"use client";

import { DownloadIcon, CopyIcon } from "@openmeme/ui";
import { useCopyImage } from "./useCopyImage";
import styles from "../../app/[locale]/memes/[category]/[slug]/page.module.css";

type Props = {
  imageUrl: string | null;
  downloadLabel: string;
  copyLabel: string;
  copiedLabel: string;
  copyErrorLabel: string;
};

export function MemeActions({
  imageUrl,
  downloadLabel,
  copyLabel,
  copiedLabel,
  copyErrorLabel,
}: Props) {
  const { state, copy } = useCopyImage(imageUrl ?? "");

  const label =
    state === "copied"
      ? copiedLabel
      : state === "error"
        ? copyErrorLabel
        : copyLabel;

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
          onClick={copy}
          aria-label={copyLabel}
        >
          <CopyIcon size={14} />
          {label}
        </button>
      ) : null}
    </div>
  );
}
