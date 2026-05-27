import type { Metadata } from "next";
import Script from "next/script";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";
import { Sidebar } from "@/components/sidebar/Sidebar";
import { MemeListingGrid, SectionTitle } from "@openmeme/ui";

import { getCategories } from "@/lib/data/categories";
import { getMemeListing } from "@/lib/data/memes";
import { getTrending } from "@/lib/data/trending";
import { breadcrumbJsonLd } from "@/lib/seo";
import { site } from "@/lib/site";
import { buildAlternates, localePath } from "@/lib/i18n-utils";
import { localeOgMap, type Locale } from "@/i18n/routing";
import type { LocaleCode } from "@/lib/api";

import styles from "./page.module.css";

export const revalidate = 300;

const CLASSIC_FETCH_LIMIT = 500;
const CLASSIC_AGE_DAYS = 90;

type Props = {
  params: Promise<{ locale: Locale }>;
};

function isClassic(createdAt: string | undefined | null): boolean {
  if (!createdAt) return false;
  const cutoff = Date.now() - CLASSIC_AGE_DAYS * 24 * 60 * 60 * 1000;
  return new Date(createdAt).getTime() < cutoff;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "clasicos" });

  return {
    title: t("meta_title"),
    description: t("meta_description", { siteName: site.name }),
    alternates: {
      canonical: `/${locale}/memes/clasicos`,
      languages: buildAlternates("/memes/clasicos"),
    },
    openGraph: {
      title: t("og_title", { siteName: site.name }),
      description: t("meta_description", { siteName: site.name }),
      url: `/${locale}/memes/clasicos`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function ClasicosMemesPage({ params }: Props) {
  const { locale } = await params;
  const apiLocale = locale as LocaleCode;
  const t = await getTranslations({ locale, namespace: "clasicos" });

  const [apiListing, categories, trending] = await Promise.all([
    getMemeListing({ sort: "score", page: 0, limit: CLASSIC_FETCH_LIMIT, locale: apiLocale }).catch(() => null),
    getCategories(apiLocale).then((c) => c.slice(0, 10)).catch(() => []),
    getTrending(5, apiLocale).catch(() => []),
  ]);

  const listing = apiListing
    ? {
        ...apiListing,
        data: apiListing.data.filter((m) => isClassic(m.createdAt)),
      }
    : null;

  const breadcrumbs = [
    { name: t("breadcrumb_inicio"), href: "/" },
    { name: t("breadcrumb_clasicos"), href: "/memes/clasicos" },
  ];

  const listingWithHref = listing
    ? {
        ...listing,
        data: listing.data.map((m) => ({ ...m, href: localePath(locale, m.href) })),
      }
    : null;

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
                <SectionTitle id="clasicos-title" icon={<span>⭐</span>}>
                  {t("section_title")}
                </SectionTitle>
                {listingWithHref && listingWithHref.data.length > 0 ? (
                  <>
                    <MemeListingGrid
                      memes={listingWithHref.data}
                      ariaLabel={t("title")}
                      priorityCount={5}
                    />
                  </>
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
