import { RedisStringsHandler } from "@trieb.work/nextjs-turbo-redis-cache";

// Next.js expects cacheHandler to resolve to a class constructor.
// The package's default export is an instance (via singleton), so
// we re-export the underlying class directly.
export default class CacheHandler extends RedisStringsHandler {}
