<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

final class MeiliDocumentTest extends TestCase
{
    private function row(array $overrides = []): array
    {
        return $overrides + [
            'slug' => 'distracted-boyfriend',
            'title' => 'Distracted Boyfriend',
            'description' => 'A classic comparison meme',
            'tags' => 'comparison classic reaction',
            'category' => 'relatable',
            'score' => '5400',
            'created_at' => '2026-01-17T12:22:20Z',
            'image' => 'relatable/distracted-boyfriend.jpg',
            'width' => '1200',
            'height' => '800',
            'author' => 'u/someone',
            'subreddit' => 'memes',
            'title_es' => 'Novio distraído',
            'description_es' => 'Un clásico meme de comparación',
            'tags_es' => 'comparación clásico reacción',
        ];
    }

    public function testIdIsSlug(): void
    {
        $doc = meili_document($this->row());
        $this->assertSame('distracted-boyfriend', $doc['id']);
        $this->assertSame('distracted-boyfriend', $doc['slug']);
    }

    public function testBilingualFieldsAreMapped(): void
    {
        $doc = meili_document($this->row());
        $this->assertSame('Distracted Boyfriend', $doc['title_en']);
        $this->assertSame('Novio distraído', $doc['title_es']);
        $this->assertSame('A classic comparison meme', $doc['description_en']);
        $this->assertSame('Un clásico meme de comparación', $doc['description_es']);
    }

    public function testTagsSplitIntoArrays(): void
    {
        $doc = meili_document($this->row());
        $this->assertSame(['comparison', 'classic', 'reaction'], $doc['tags_en']);
        $this->assertSame(['comparación', 'clásico', 'reacción'], $doc['tags_es']);
    }

    public function testEmptyTagsBecomeEmptyArrays(): void
    {
        $doc = meili_document($this->row(['tags' => '', 'tags_es' => '  ']));
        $this->assertSame([], $doc['tags_en']);
        $this->assertSame([], $doc['tags_es']);
    }

    public function testUntranslatedMemeHasEmptyEsFields(): void
    {
        $doc = meili_document($this->row(['title_es' => '', 'description_es' => '', 'tags_es' => '']));
        $this->assertSame('', $doc['title_es']);
        $this->assertSame('', $doc['description_es']);
        $this->assertSame([], $doc['tags_es']);
    }

    public function testNumericFieldsAreInts(): void
    {
        $doc = meili_document($this->row());
        $this->assertSame(5400, $doc['score']);
        $this->assertSame(1200, $doc['width']);
        $this->assertSame(800, $doc['height']);
    }

    public function testCreatedTsIsUnixTimestampForSorting(): void
    {
        $doc = meili_document($this->row());
        $this->assertSame(strtotime('2026-01-17T12:22:20Z'), $doc['created_ts']);
    }

    public function testInvalidDateFallsBackToZero(): void
    {
        $doc = meili_document($this->row(['created_at' => 'not-a-date']));
        $this->assertSame(0, $doc['created_ts']);
        $doc = meili_document($this->row(['created_at' => '']));
        $this->assertSame(0, $doc['created_ts']);
    }
}
