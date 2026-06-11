<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

final class EnvTest extends TestCase
{
    private string $envFile;

    protected function setUp(): void
    {
        $this->envFile = tempnam(sys_get_temp_dir(), 'omenv');
    }

    protected function tearDown(): void
    {
        @unlink($this->envFile);
    }

    public function testParsesKeyValuesSkippingCommentsAndQuotes(): void
    {
        file_put_contents($this->envFile, <<<ENV
        # a comment
        OM_TEST_PLAIN=hello
        OM_TEST_QUOTED="quoted value"
        OM_TEST_SINGLE='single value'

        not a kv line
        ENV);
        load_env([$this->envFile]);

        $this->assertSame('hello', env('OM_TEST_PLAIN'));
        $this->assertSame('quoted value', env('OM_TEST_QUOTED'));
        $this->assertSame('single value', env('OM_TEST_SINGLE'));
    }

    public function testRealEnvironmentWins(): void
    {
        putenv('OM_TEST_REAL=from-environment');
        file_put_contents($this->envFile, "OM_TEST_REAL=from-file\n");
        load_env([$this->envFile]);

        $this->assertSame('from-environment', env('OM_TEST_REAL'));
    }

    public function testDefaultUsedWhenMissingOrEmpty(): void
    {
        $this->assertSame('fallback', env('OM_TEST_DOES_NOT_EXIST', 'fallback'));
        putenv('OM_TEST_EMPTY=');
        $this->assertSame('fallback', env('OM_TEST_EMPTY', 'fallback'));
    }
}
