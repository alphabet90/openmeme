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
  const t = await getTranslations({ locale, namespace: "terminos" });

  return {
    title: t("meta_title"),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/terminos`,
      languages: buildAlternates("/terminos"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("meta_description"),
      url: `/${locale}/terminos`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function TerminosPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "terminos" });

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
                <h2>{t("acceptance_title")}</h2>
                <p>{t("acceptance_body")}</p>
              </section>

              <section>
                <h2>{t("conduct_title")}</h2>
                <p>{t("conduct_body")}</p>
              </section>

              <section>
                <h2>{t("ownership_title")}</h2>
                <p>{t("ownership_body")}</p>
              </section>

              <section>
                <h2>{t("dmca_title")}</h2>
                <p>
                  {t.rich("dmca_body", {
                    dmcaLink: (chunks) => (
                      <Link href={`/${locale}/dmca`}>{chunks}</Link>
                    ),
                  })}
                </p>
              </section>

              <section>
                <h2>{t("liability_title")}</h2>
                <p>{t("liability_body")}</p>
              </section>

              <section>
                <h2>{t("governing_law_title")}</h2>
                <p>{t("governing_law_body")}</p>
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
