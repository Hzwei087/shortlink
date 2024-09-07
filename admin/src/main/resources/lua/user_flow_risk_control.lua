-- 设置用户访问频率限制的参数
local username = KEYS[1]
local timeWindow = tonumber(ARGV[1]) -- 时间窗口，单位：秒


-- 构造 Redis 中存储用户访问次数的键名
local accessKey = "short-link:user-flow-risk-control:" .. username

-- 原子递增访问次数，并获取递增后的值
local currentAccessCount = redis.call("INCR", accessKey)

-- -- 设置键的过期时间(已淘汰)
-- redis.call("EXPIRE", accessKey, timeWindow)


-- 设置键的过期时间
if currentAccessCount == 1 then
    redis.call("EXPIRE", accessKey, timeWindow)
end

-- 返回当前访问次数
return currentAccessCount



-- Redis 的 Lua 脚本中 KEYS 和 ARGV 的索引从 1 开始，而不是从 0 开始，这是 Lua 的设计决定和 Redis 实现的约定：
--
-- Lua 语言设计：Lua 的表（table）是以 1 为起始索引的。虽然 Lua 支持从 0 开始的索引（通过自定义表），但标准库和大多数 Lua 的用法都从 1 开始。因此，Redis 在 Lua 脚本中也遵循了这种设计。
--
-- Redis 的约定：Redis 的 Lua 脚本中，KEYS 数组用于存储传递给脚本的键名，而 ARGV 数组用于存储传递给脚本的附加参数。这种约定与 Lua 本身的表设计保持一致，即 KEYS 和 ARGV 数组的索引从 1 开始。