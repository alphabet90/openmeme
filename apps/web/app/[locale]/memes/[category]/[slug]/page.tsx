import type { Metadata } from "next";
import Link from "next/link";
import Script from "next/script";
import { notFound } from "next/navigation";
import { getTranslations } from "next-intl/server";

import { Nav } from "@/components/nav/Nav";
import { Footer } from "@/components/Footer";
import { MemeListingGrid, SectionTitle } from "@openmeme/ui";
import { CalendarIcon } from "@openmeme/ui";
import { MemeActions } from "@/components/meme-detail/MemeActions";
import { CopyButton } from "@/components/meme-detail/CopyButton";

import { getCategoryListing, getMeme } from "@/lib/data/memes";
import { breadcrumbJsonLd, memeImageObjectJsonLd } from "@/lib/seo";
import { site } from "@/lib/site";
import { buildAlternates, localePath } from "@/lib/i18n-utils";
import { localeLangMap, localeOgMap, type Locale } from "@/i18n/routing";
import type { LocaleCode } from "@/lib/api";

import styles from "./page.module.css";

export const revalidate = 3600;

type RouteParams = { locale: Locale; category: string; slug: string };
type Props = { params: Promise<RouteParams> };

function humanize(slug: string): string {
  return slug
    .split(/[-_]/)
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

function getInitials(name: string): string {
  return name.charAt(0).toUpperCase();
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale, category, slug } = await params;
  const apiLocale = locale as LocaleCode;
  const t = await getTranslations({ locale, namespace: "meme_detail" });

  const meme = await getMeme(category, slug, apiLocale);
  if (!meme) return { title: t("not_found_title") };

  const canonical = `/${locale}/memes/${meme.category}/${meme.slug}`;
  const description =
    meme.description ??
    t("meta_description_fallback", { title: meme.title, siteName: site.name });

  const images = meme.imageUrl
    ? [{ url: meme.imageUrl, alt: meme.title }]
    : [{ url: site.ogImage, alt: site.name }];

  return {
    title: meme.title,
    description,
    alternates: {
      canonical,
      languages: buildAlternates(`/memes/${meme.category}/${meme.slug}`),
    },
    openGraph: {
      title: meme.title,
      description,
      type: "article",
      url: canonical,
      images,
      locale: localeOgMap[locale],
    },
    twitter: {
      card: "summary_large_image",
      title: meme.title,
      description,
      images: meme.imageUrl ? [meme.imageUrl] : [site.ogImage],
    },
    keywords: meme.tags,
  };
}

export default async function MemeDetailPage({ params }: Props) {
  const { locale, category, slug } = await params;
  const apiLocale = locale as LocaleCode;
  const t = await getTranslations({ locale, namespace: "meme_detail" });

  const meme = await getMeme(category, slug, apiLocale);
  if (!meme) notFound();

  const related = await getCategoryListing(meme.category, {
    limit: 12,
    sort: "score",
    locale: apiLocale,
  });
  const relatedMemes = (related?.data ?? [])
    .filter((m) => m.slug !== meme.slug)
    .slice(0, 10)
    .map((m) => ({ ...m, href: localePath(locale, m.href) }));

  const categoryDisplay = humanize(meme.category);
  const breadcrumbs = [
    { name: t("breadcrumb_inicio"), href: "/" },
    { name: t("breadcrumb_categorias"), href: "/categorias" },
    { name: categoryDisplay, href: `/categorias/${meme.category}` },
    { name: meme.title, href: `/memes/${meme.category}/${meme.slug}` },
  ];

  const lang = localeLangMap[locale];

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
                      <Link href={localePath(locale, b.href)}>{b.name}</Link>
                    ) : (
                      <span aria-current="page">{b.name}</span>
                    )}
                  </li>
                ))}
              </ol>
            </nav>

            <article className={styles.hero}>
              <div
                className={styles.imageWrap}
                style={{ background: meme.placeholderGradient }}
              >
                {meme.imageUrl ? (
                  <img
                    src={meme.imageUrl}
                    alt={meme.description}
                    className={styles.image}
                    fetchPriority="high"
                  />
                ) : (
                  <span className={styles.glyph} aria-hidden="true">
                    {meme.placeholder}
                  </span>
                )}

                {meme.imageUrl ? (
                  <div className={styles.imageActions}>
                    <CopyButton
                      imageUrl={meme.imageUrl}
                      className={styles.imageActionBtn}
                      ariaLabel={t("copiar")}
                    />
                  </div>
                ) : null}
              </div>

              <div className={styles.info}>
                <h1 className={styles.title}>{meme.title}</h1>

                <div className={styles.metaRow}>
                  {meme.author ? (
                    <div className={styles.uploader}>
                      <div className={styles.uploaderAvatar}>
                        {getInitials(meme.author)}
                      </div>
                      <span className={styles.uploaderName}>
                        u/{meme.author}
                      </span>
                    </div>
                  ) : null}

                  {meme.author ? (
                    <span className={styles.metaDot} aria-hidden="true">
                      ●
                    </span>
                  ) : null}

                  <Link
                    href={localePath(
                      locale,
                      `/categorias/${meme.category}`,
                    )}
                    className={styles.metaCategory}
                  >
                    #{meme.category}
                  </Link>

                  <span className={styles.metaDot} aria-hidden="true">
                    ●
                  </span>

                  <span className={styles.metaDate}>
                    <CalendarIcon size={13} />
                    <time dateTime={meme.createdAt}>
                      {new Date(meme.createdAt).toLocaleDateString(lang, {
                        day: "2-digit",
                        month: "long",
                        year: "numeric",
                      })}
                    </time>
                  </span>
                </div>

                {meme.description ? (
                  <p className={styles.description}>{meme.description}</p>
                ) : null}

                {meme.tags.length ? (
                  <ul className={styles.tags} aria-label={t("etiquetas_label")}>
                    {meme.tags.map((tag) => (
                      <li key={tag}>
                        <Link
                          href={localePath(
                            locale,
                            `/buscar?q=${encodeURIComponent(tag)}`,
                          )}
                          className={styles.tag}
                        >
                          #{tag}
                        </Link>
                      </li>
                    ))}
                  </ul>
                ) : null}

                <MemeActions
                  imageUrl={meme.imageUrl}
                  downloadLabel={t("descargar")}
                  copyLabel={t("copiar")}
                  copiedLabel={t("copiado")}
                  copyErrorLabel={t("copy_error")}
                />
              </div>
            </article>

            {relatedMemes.length ? (
              <section
                aria-labelledby="related-title"
                className={styles.relatedWrap}
              >
                <SectionTitle id="related-title" icon={<span>🔥</span>}>
                  {t("related_title", { category: categoryDisplay })}
                </SectionTitle>
                <MemeListingGrid
                  memes={relatedMemes}
                  ariaLabel={t("related_aria", { category: categoryDisplay })}
                />
              </section>
            ) : null}
          </div>
        </main>

        <Footer />
      </div>

      <Script
        id="ld-meme"
        type="application/ld+json"
        strategy="beforeInteractive"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify(memeImageObjectJsonLd(meme, locale)),
        }}
      />
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
