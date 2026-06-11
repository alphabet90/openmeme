<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

final class MeiliSuggestTermTest extends TestCase
{
    public function testSpanishLocalePrefersSpanishTitle(): void
    {
        $hit = ['title_en' => 'Distracted Boyfriend', 'title_es' => 'Novio distraído'];
        $this->assertSame('Novio distraído', meili_suggest_term($hit, 'es'));
    }

    public function testEnglishLocalePrefersEnglishTitle(): void
    {
        $hit = ['title_en' => 'Distracted Boyfriend', 'title_es' => 'Novio distraído'];
        $this->assertSame('Distracted Boyfriend', meili_suggest_term($hit, 'en'));
    }

    public function testSpanishLocaleFallsBackToEnglishWhenUntranslated(): void
    {
        $hit = ['title_en' => 'Distracted Boyfriend', 'title_es' => ''];
        $this->assertSame('Distracted Boyfriend', meili_suggest_term($hit, 'es'));
    }

    public function testEnglishLocaleFallsBackToSpanishForEsOnlyMemes(): void
    {
        $hit = ['title_en' => '', 'title_es' => 'Che, qué onda'];
        $this->assertSame('Che, qué onda', meili_suggest_term($hit, 'en'));
    }

    public function testMissingTitlesYieldEmptyString(): void
    {
        $this->assertSame('', meili_suggest_term([], 'es'));
    }
}
