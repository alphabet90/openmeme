import type { Metadata } from "next";
import Script from "next/script";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";
import { Sidebar } from "@/components/sidebar/Sidebar";
import { SectionTitle } from "@openmeme/ui";

import { getCategories } from "@/lib/data/categories";
import { getMemeListing } from "@/lib/data/memes";
import { getTrending } from "@/lib/data/trending";
import { breadcrumbJsonLd } from "@/lib/seo";
import { site } from "@/lib/site";
import { buildAlternates, localePath } from "@/lib/i18n-utils";
import { localeOgMap, type Locale } from "@/i18n/routing";
import type { LocaleCode } from "@/lib/api";
import type { Meme } from "@openmeme/ui";

import { ShuffledMemeGrid } from "./shuffled-grid";
import styles from "./page.module.css";

export const revalidate = 300;

const PAGE_SIZE = 100;

type Props = {
  params: Promise<{ locale: Locale }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "aleatorio" });

  return {
    title: t("meta_title"),
    description: t("meta_description", { siteName: site.name }),
    alternates: {
      canonical: `/${locale}/memes/aleatorio`,
      languages: buildAlternates("/memes/aleatorio"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("meta_description", { siteName: site.name }),
      url: `/${locale}/memes/aleatorio`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function AleatorioMemesPage({ params }: Props) {
  const { locale } = await params;
  const apiLocale = locale as LocaleCode;
  const t = await getTranslations({ locale, namespace: "aleatorio" });

  const [listing, categories, trending] = await Promise.all([
    getMemeListing({ sort: "score", page: 0, limit: PAGE_SIZE, locale: apiLocale }).catch(() => null),
    getCategories(apiLocale).then((c) => c.slice(0, 10)).catch(() => []),
    getTrending(5, apiLocale).catch(() => []),
  ]);

  const breadcrumbs = [
    { name: t("breadcrumb_inicio"), href: "/" },
    { name: t("breadcrumb_aleatorio"), href: "/memes/aleatorio" },
  ];

  const memes: Meme[] = listing
    ? listing.data.map((m) => ({ ...m, href: localePath(locale, m.href) }))
    : [];

  return (
    <>
      <Nav />

      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            <nav aria-label="Migas de pan" className={styles.breadcrumbs}>
              <ol>
                {breadcrumbs.map((b, i) => (
                  <li key={b.href}>
                    {i < breadcrumbs.length - 1 ? (
                      <a href={localePath(locale, b.href)}>{b.name}</a>
                    ) : (
                      <span aria-current="page">{b.name}</span>
                    )}
                  </li>
                ))}
              </ol>
            </nav>

            <header className={styles.header}>
              <p className={styles.eyebrow}>{t("eyebrow")}</p>
              <h1 className={styles.title}>{t("title")}</h1>
              <p className={styles.sub}>{t("sub")}</p>
            </header>

            <div className={styles.layout}>
              <div className={styles.primary}>
                <SectionTitle id="aleatorio-title" icon={<span>⭐</span>}>
                  {t("section_title")}
                </SectionTitle>
                {memes.length > 0 ? (
                  <ShuffledMemeGrid memes={memes} ariaLabel={t("title")} />
                ) : (
                  <p className={styles.empty}>{t("empty")}</p>
                )}
              </div>

              <Sidebar categories={categories} trending={trending} />
            </div>
          </div>
        </main>

        <Footer />
      </div>

      <Script
        id="ld-breadcrumb"
        type="application/ld+json"
        strategy="beforeInteractive"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(breadcrumbJsonLd(breadcrumbs, locale)),
        }}
      />
    </>
  );
}
