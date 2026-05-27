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
  const t = await getTranslations({ locale, namespace: "terminos" });

  return {
    title: t("meta_title", { siteName: site.name }),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/terminos`,
      languages: buildAlternates("/terminos"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("og_description"),
      url: `/${locale}/terminos`,
    },
  };
}

export default async function TerminosPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "terminos" });

  const sections = [
    { title: t("section_1_title"), body: t("section_1_body") },
    { title: t("section_2_title"), body: t("section_2_body") },
    { title: t("section_3_title"), body: t("section_3_body") },
    { title: t("section_4_title"), body: t("section_4_body") },
    { title: t("section_5_title"), body: t("section_5_body") },
    { title: t("section_6_title"), body: t("section_6_body") },
    { title: t("section_7_title"), body: t("section_7_body") },
    { title: t("section_8_title"), body: t("section_8_body") },
  ];

  return (
    <>
      <Nav />
      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <article className={styles.article}>
              <p className={styles.eyebrow}>Legal</p>
              <h1 className={styles.title}>{t("title")}</h1>
              <p className={styles.lastUpdated}>{t("last_updated")}</p>
              <p className={styles.intro}>{t("intro")}</p>
              {sections.map((s, i) => (
                <section key={i} className={styles.section}>
                  <h2 className={styles.sectionTitle}>{s.title}</h2>
                  <p className={styles.sectionBody}>{s.body}</p>
                </section>
              ))}
            </article>
          </div>
        </main>
        <Footer />
      </div>
    </>
  );
}
