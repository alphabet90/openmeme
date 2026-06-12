<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

require_once SITE_ROOT . '/src/helpers.php';

final class HelpersTest extends TestCase
{
    public function testMemeImgEncodesSegmentsButKeepsSlashes(): void
    {
        $meme = ['image' => 'reaction/cat meme #1.jpg'];
        $this->assertSame('/memes/reaction/cat%20meme%20%231.jpg', meme_img($meme));
    }

    public function testSameOriginWhenCdnUnset(): void
    {
        [$src, $abs] = $this->renderUrls('');

        $this->assertSame('/memes/reaction/cat.jpg', $src);
        $this->assertSame('https://base.test/memes/reaction/cat.jpg', $abs);
    }

    public function testCdnHostUsedWhenConfiguredAndTrailingSlashTrimmed(): void
    {
        [$src, $abs] = $this->renderUrls('https://cdn.test/');

        $this->assertSame('https://cdn.test/memes/reaction/cat.jpg', $src);
        $this->assertSame('https://cdn.test/memes/reaction/cat.jpg', $abs);
    }

    /**
     * CDN_URL is a constant fixed when config.php loads, so each branch
     * needs a fresh PHP process with a controlled environment.
     *
     * @return array{string, string} [meme_img_src(), meme_img_abs()]
     */
    private function renderUrls(string $cdnUrl): array
    {
        $code = 'require $argv[1] . "/config.php";'
            . 'require $argv[1] . "/src/helpers.php";'
            . '$m = ["image" => "reaction/cat.jpg"];'
            . 'echo meme_img_src($m), "\n", meme_img_abs($m);';
        $cmd = sprintf(
            'OPENMEME_CDN_URL=%s OPENMEME_BASE_URL=https://base.test %s -r %s %s 2>&1',
            escapeshellarg($cdnUrl),
            escapeshellarg(PHP_BINARY),
            escapeshellarg($code),
            escapeshellarg(SITE_ROOT)
        );
        exec($cmd, $lines, $exitCode);
        $this->assertSame(0, $exitCode, 'helper subprocess failed: ' . implode("\n", $lines));
        $this->assertCount(2, $lines, 'unexpected subprocess output: ' . implode("\n", $lines));

        return [$lines[0], $lines[1]];
    }
}
