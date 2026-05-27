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
  const t = await getTranslations({ locale, namespace: "privacidad" });

  return {
    title: t("meta_title"),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/privacidad`,
      languages: buildAlternates("/privacidad"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("meta_description"),
      url: `/${locale}/privacidad`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function PrivacidadPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "privacidad" });

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
                <h2>{t("info_collected_title")}</h2>
                <p>{t("info_collected_body")}</p>
              </section>

              <section>
                <h2>{t("cookies_title")}</h2>
                <p>{t("cookies_body")}</p>
              </section>

              <section>
                <h2>{t("third_party_title")}</h2>
                <p>{t("third_party_body")}</p>
              </section>

              <section>
                <h2>{t("sharing_title")}</h2>
                <p>{t("sharing_body")}</p>
              </section>

              <section>
                <h2>{t("user_rights_title")}</h2>
                <p>{t("user_rights_body")}</p>
              </section>

              <section>
                <h2>{t("retention_title")}</h2>
                <p>{t("retention_body")}</p>
              </section>

              <section>
                <h2>{t("security_title")}</h2>
                <p>{t("security_body")}</p>
              </section>

              <section>
                <h2>{t("contact_title")}</h2>
                <p>
                  {t.rich("contact_body", {
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
