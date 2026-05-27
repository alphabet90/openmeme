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
  const t = await getTranslations({ locale, namespace: "dmca" });

  return {
    title: t("meta_title"),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/dmca`,
      languages: buildAlternates("/dmca"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("meta_description"),
      url: `/${locale}/dmca`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function DmcaPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "dmca" });

  return (
    <>
      <Nav />

      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <header className={styles.header}>
              <p className={styles.eyebrow}>{t("eyebrow")}</p>
              <h1 className={styles.title}>{t("title")}</h1>
              <p className={styles.meta}>{t("last_updated")}</p>
            </header>

            <div className={styles.content}>
              <section>
                <h2>{t("designated_agent_title")}</h2>
                <p>{t("designated_agent_body")}</p>
                <p>
                  <strong>Email: </strong>
                  <a href="mailto:support@openmeme.io">support@openmeme.io</a>
                </p>
              </section>

              <section>
                <h2>{t("notification_title")}</h2>
                <p>{t("notification_body")}</p>
              </section>

              <section>
                <h2>{t("counter_notice_title")}</h2>
                <p>{t("counter_notice_body")}</p>
              </section>

              <section>
                <h2>{t("repeat_infringer_title")}</h2>
                <p>{t("repeat_infringer_body")}</p>
              </section>

              <section>
                <h2>{t("contact_title")}</h2>
                <p>
                  {t.rich("contact_note", {
                    contactLink: (chunks) => (
                      <Link href={`/${locale}/contacto`}>{chunks}</Link>
                    ),
                  })}
                </p>
              </section>
            </div>
          </div>
        </main>

        <Footer />
      </div>
    </>
  );
}
