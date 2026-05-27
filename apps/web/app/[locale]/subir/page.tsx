import type { Metadata } from "next";
import Script from "next/script";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";
import { UploadIcon } from "@openmeme/ui";

import { breadcrumbJsonLd } from "@/lib/seo";
import { site } from "@/lib/site";
import { buildAlternates } from "@/lib/i18n-utils";
import { localeOgMap, type Locale } from "@/i18n/routing";

import styles from "./page.module.css";

type Props = {
  params: Promise<{ locale: Locale }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  return {
    title: t("meta_title"),
    description: t("meta_description", { siteName: site.name }),
    alternates: {
      canonical: `/${locale}/subir`,
      languages: buildAlternates("/subir"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("og_description", { siteName: site.name }),
      url: `/${locale}/subir`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function SubirPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  const breadcrumbs = [
    { name: t("breadcrumb_inicio"), href: "/" },
    { name: t("breadcrumb_subir"), href: "/subir" },
  ];

  return (
    <>
      <Nav />

      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <div className={styles.hero}>
              <div className={styles.iconWrap}>
                <UploadIcon size={48} />
              </div>
              <h1 className={styles.comingSoon}>{t("coming_soon_title")}</h1>
              <p className={styles.description}>{t("coming_soon_desc", { siteName: site.name })}</p>
            </div>
          </div>
        </main>

        <Footer />
      </div>

      <Script id="ld-breadcrumb" type="application/ld+json" strategy="beforeInteractive"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(breadcrumbJsonLd(breadcrumbs, locale)),
        }}
      />
    </>
  );
}
