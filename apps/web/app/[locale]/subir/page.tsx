import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";

import { site } from "@/lib/site";
import { buildAlternates, localePath } from "@/lib/i18n-utils";
import { localeOgMap, type Locale } from "@/i18n/routing";

import styles from "./page.module.css";

type Props = { params: Promise<{ locale: Locale }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  return {
    title: t("meta_title"),
    description: t("meta_description", { siteName: site.name }),
    alternates: {
      canonical: localePath(locale, "/subir"),
      languages: buildAlternates("/subir"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("og_description", { siteName: site.name }),
      url: localePath(locale, "/subir"),
      locale: localeOgMap[locale],
    },
  };
}

export default async function UploadPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  return (
    <>
      <Nav />

      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <header className={styles.header}>
              <p className={styles.eyebrow}>{t("eyebrow")}</p>
              <h1 className={styles.title}>{t("title")}</h1>
              <p className={styles.sub}>{t("sub")}</p>
            </header>

            <div className={styles.comingSoon}>
              <div className={styles.iconWrapper}>
                <svg
                  width="64"
                  height="64"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
              </div>
              <p className={styles.comingSoonText}>{t("coming_soon")}</p>
              <p className={styles.comingSoonDesc}>{t("coming_soon_desc")}</p>
            </div>
          </div>
        </main>

        <Footer />
      </div>
    </>
  );
}
