import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";
import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";
import { site } from "@/lib/site";
import { buildAlternates } from "@/lib/i18n-utils";
import { type Locale } from "@/i18n/routing";
import styles from "./page.module.css";

export const revalidate = 3600;

type Props = { params: Promise<{ locale: Locale }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "contacto" });

  return {
    title: t("meta_title", { siteName: site.name }),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/contacto`,
      languages: buildAlternates("/contacto"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("og_description"),
      url: `/${locale}/contacto`,
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
            <article className={styles.article}>
              <p className={styles.eyebrow}>{t("eyebrow")}</p>
              <h1 className={styles.title}>{t("title")}</h1>
              <p className={styles.description}>{t("description")}</p>
              <div className={styles.contactCard}>
                <h2 className={styles.contactHeading}>{t("email_heading")}</h2>
                <a
                  href={`mailto:${t("email_address")}`}
                  className={styles.emailLink}
                >
                  {t("email_address")}
                </a>
                <p className={styles.responseTime}>{t("response_time")}</p>
              </div>
            </article>
          </div>
        </main>
        <Footer />
      </div>
    </>
  );
}
