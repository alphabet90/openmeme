<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

require_once SITE_ROOT . '/src/helpers.php';

final class SecurityHeadersTest extends TestCase
{
    public function testNonceIsStableWithinRequestAndUnguessable(): void
    {
        $nonce = csp_nonce();
        $this->assertSame($nonce, csp_nonce(), 'nonce must not change between layout blocks');
        $raw = base64_decode($nonce, true);
        $this->assertNotFalse($raw, 'nonce must be valid base64');
        $this->assertGreaterThanOrEqual(16, strlen($raw), 'nonce needs >=128 bits of entropy');
    }

    public function testPolicyLocksDownSourcesAndCarriesNonce(): void
    {
        $policy = csp_policy();
        $nonce = csp_nonce();

        $this->assertStringContainsString("default-src 'self'", $policy);
        $this->assertStringContainsString("script-src 'self' 'nonce-$nonce'", $policy);
        $this->assertStringContainsString("style-src 'self' 'nonce-$nonce' https://fonts.googleapis.com", $policy);
        $this->assertStringContainsString("font-src 'self' https://fonts.gstatic.com", $policy);
        $this->assertStringContainsString("connect-src 'self'", $policy);
        $this->assertStringContainsString("object-src 'none'", $policy);
        $this->assertStringContainsString("base-uri 'self'", $policy);
        $this->assertStringContainsString("form-action 'self'", $policy);
        $this->assertStringContainsString("frame-ancestors 'self'", $policy);
        $this->assertStringNotContainsString('unsafe-inline', $policy);
        $this->assertStringNotContainsString('unsafe-eval', $policy);
    }

    public function testImgSrcIsSelfOnlyWhenCdnUnset(): void
    {
        $this->assertStringContainsString("img-src 'self';", $this->policyFor(''));
    }

    public function testImgSrcIncludesCdnHostWhenConfigured(): void
    {
        $this->assertStringContainsString("img-src 'self' https://cdn.test;", $this->policyFor('https://cdn.test/'));
    }

    /**
     * CDN_URL is a constant fixed when config.php loads, so each branch
     * needs a fresh PHP process with a controlled environment.
     */
    private function policyFor(string $cdnUrl): string
    {
        $code = 'require $argv[1] . "/config.php";'
            . 'require $argv[1] . "/src/helpers.php";'
            . 'echo csp_policy();';
        $cmd = sprintf(
            'OPENMEME_CDN_URL=%s OPENMEME_BASE_URL=https://base.test %s -r %s %s 2>&1',
            escapeshellarg($cdnUrl),
            escapeshellarg(PHP_BINARY),
            escapeshellarg($code),
            escapeshellarg(SITE_ROOT)
        );
        exec($cmd, $lines, $exitCode);
        $this->assertSame(0, $exitCode, 'policy subprocess failed: ' . implode("\n", $lines));

        return implode("\n", $lines);
    }
}
