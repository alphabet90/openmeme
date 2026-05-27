import { getLocale, getTranslations } from "next-intl/server";
import Link from "next/link";
import { SearchIcon, StarIcon, BoltIcon, SmileIcon, ShuffleIcon } from "@openmeme/ui";
import { localePath } from "@/lib/i18n-utils";
import type { Locale } from "@/i18n/routing";
import styles from "./Hero.module.css";

const filterLinks = [
  { id: "top",      href: "/memes/top",      Icon: StarIcon,    label: "filter_top" as const },
  { id: "nuevos",   href: "/memes/nuevos",   Icon: BoltIcon,    label: "filter_nuevos" as const },
  { id: "clasicos", href: "/memes/clasicos", Icon: SmileIcon,   label: "filter_clasicos" as const },
  { id: "random",   href: "/memes/aleatorio", Icon: ShuffleIcon, label: "filter_random" as const },
];

export async function Hero() {
  const locale = (await getLocale()) as Locale;
  const t = await getTranslations("hero");

  return (
    <section className={styles.hero} aria-labelledby="hero-title">
      <div className="container">
        <div className={styles.inner}>
          <div className={styles.content}>
            <p className={styles.eyebrow}>
              <span aria-hidden="true">🔥</span> {t("eyebrow")}
            </p>

            <h1 id="hero-title" className={styles.title}>
              <span>{t("title_line1")}</span>
              <span className={styles.highlight}>{t("title_highlight")}</span>
              <span>{t("title_line2")}</span>
              <span>{t("title_line3")}</span>
            </h1>

            <p className={styles.sub}>
              {t("sub")}
              <br />
              <a href={localePath(locale, "/manifiesto")}>{t("sub_link")}</a>
            </p>

            <form
              action={localePath(locale, "/buscar")}
              role="search"
              aria-label={t("search_label")}
              className={styles.search}
            >
              <label className={styles.searchField}>
                <span className="sr-only">{t("search_label")}</span>
                <SearchIcon size={16} />
                <input
                  type="search"
                  name="q"
                  placeholder={t("search_placeholder")}
                  autoComplete="off"
                />
              </label>
              <button type="submit" className={styles.searchBtn} aria-label={t("search_label")}>
                <SearchIcon size={16} />
              </button>
            </form>

            <div className={styles.pills}>
              {filterLinks.map(({ id, href, Icon, label }) => (
                <Link
                  key={id}
                  href={localePath(locale, href)}
                  className={styles.pill}
                >
                  <Icon size={12} />
                  <span>{t(label).toUpperCase()}</span>
                </Link>
              ))}
            </div>
          </div>

          <div className={styles.visual} aria-hidden="true">
            <div className={styles.dog}>🐕</div>
            <span className={`${styles.sticker} ${styles.stickerTop}`}>¿Qué mirá bobo?</span>
            <span className={`${styles.sticker} ${styles.stickerBlue}`}>Sin memes no hay paraíso</span>
            <span className={`${styles.sticker} ${styles.stickerGhost}`}>Yo los vi</span>
          </div>
        </div>
      </div>
    </section>
  );
}
