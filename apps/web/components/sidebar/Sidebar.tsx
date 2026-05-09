import { CategoryList } from "./CategoryList";
import { TrendingList } from "./TrendingList";
import { UploadWidget } from "./UploadWidget";
import type { Category, TrendingTag } from "@openmeme/ui";
import styles from "./Sidebar.module.css";

type SidebarProps = {
  categories: Category[];
  trending: TrendingTag[];
  activeCategorySlug?: string;
};

export function Sidebar({
  categories,
  trending,
  activeCategorySlug,
}: SidebarProps) {
  return (
    <aside className={styles.sidebar} aria-label="Explorar">
      <CategoryList categories={categories} activeSlug={activeCategorySlug} />
      <TrendingList tags={trending} />
      <UploadWidget />
    </aside>
  );
}
