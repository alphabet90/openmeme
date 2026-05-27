import type { Metadata } from "next";
import Link from "next/link";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";

import { site } from "@/lib/site";
import { buildAlternates } from "@/lib/i18n-utils";
import { localeOgMap, type Locale } from "@/i18n/routing";

import styles from "../legal.module.css";

export const revalidate = 86400;

type Props = { params: Promise<{ locale: Locale }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "contacto" });

  return {
    title: t("meta_title"),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/contacto`,
      languages: buildAlternates("/contacto"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("meta_description"),
      url: `/${locale}/contacto`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function ContactoPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "contacto" });

  return (
    <>
      <Nav />

      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <header className={styles.header}>
              <p className={styles.eyebrow}>{t("eyebrow")}</p>
              <h1 className={styles.title}>{t("title")}</h1>
            </header>

            <div className={styles.content}>
              <p>{t("description")}</p>

              <div className={styles.contactBlock}>
                <p className={styles.contactLabel}>{t("email_label")}</p>
                <a href="mailto:support@openmeme.io" className={styles.contactEmail}>
                  support@openmeme.io
                </a>
              </div>

              <p className={styles.responseTime}>{t("response_time")}</p>

              <p className={styles.legalNote}>
                {t.rich("legal_note", {
                  dmcaLink: (chunks) => (
                    <Link href={`/${locale}/dmca`}>{chunks}</Link>
                  ),
                })}
              </p>
            </div>
          </div>
        </main>

        <Footer />
      </div>
    </>
  );
}
