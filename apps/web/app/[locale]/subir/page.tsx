import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";
import { buildAlternates } from "@/lib/i18n-utils";
import { localeOgMap, type Locale } from "@/i18n/routing";

import styles from "./page.module.css";

export const revalidate = 3600;

type Props = { params: Promise<{ locale: Locale }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  return {
    title: t("meta_title"),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/subir`,
      languages: buildAlternates("/subir"),
    },
    openGraph: {
      title: t("og_title"),
      description: t("og_description"),
      url: `/${locale}/subir`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function SubirPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  return (
    <>
      <Nav />
      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <h1>{t("title")}</h1>
            <p>{t("description")}</p>
          </div>
        </main>
      </div>
      <Footer />
    </>
  );
}
