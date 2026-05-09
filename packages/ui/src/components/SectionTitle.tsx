import styles from "./SectionTitle.module.css";
import type { ReactNode } from "react";

type SectionTitleProps = {
  children: ReactNode;
  icon?: ReactNode;
  as?: "h1" | "h2" | "h3";
  id?: string;
};

export function SectionTitle({
  children,
  icon,
  as: As = "h2",
  id,
}: SectionTitleProps) {
  return (
    <As id={id} className={styles.title}>
      {icon ? <span className={styles.icon} aria-hidden="true">{icon}</span> : null}
      <span>{children}</span>
    </As>
  );
}
