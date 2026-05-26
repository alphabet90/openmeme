const { RedisStringsHandler } = require("@trieb.work/nextjs-turbo-redis-cache");

// Next.js expects cacheHandler to resolve to a class constructor.
// The package's default export is an instance (via singleton), so
// we re-export the underlying class directly.
class CacheHandler extends RedisStringsHandler {}

module.exports = CacheHandler;
module.exports.default = CacheHandler;
