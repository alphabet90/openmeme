import type { Meme } from "../types";
import { MemeCard } from "./MemeCard";
import styles from "./MemeListingGrid.module.css";

type MemeListingGridProps = {
  memes: Meme[];
  ariaLabel: string;
  /** First N cards get priority loading. */
  priorityCount?: number;
};

export function MemeListingGrid({
  memes,
  ariaLabel,
  priorityCount = 4,
}: MemeListingGridProps) {
  return (
    <ul className={styles.grid} aria-label={ariaLabel} role="list">
      {memes.map((m, i) => (
        <li key={m.id}>
          <MemeCard
            meme={m}
            priority={i < priorityCount}
            naturalSize
          />
        </li>
      ))}
    </ul>
  );
}
