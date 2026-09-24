-- 入参：KEYS[1]=红包库存Key  KEYS[2]=用户去重Key  KEYS[3]=红包剩余金额Key
-- 入参：ARGV[1]=用户ID  ARGV[2]=最小金额(1分)  ARGV[3]=当前剩余人数
local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local remainAmountKey = KEYS[3]
local userId = ARGV[1]
local minAmount = tonumber(ARGV[2])
local remainCount = tonumber(ARGV[3])

-- 1. 校验用户是否已经抢过（去重）
if redis.call('sismember', userSetKey, userId) == 1 then
    return -1 -- -1代表重复抢
end

-- 2. 校验库存是否充足
local stock = tonumber(redis.call('get', stockKey))
if stock <= 0 then
    return 0 -- 0代表已抢光
end

-- 3. 二倍均值计算本次抢到的金额（最后一个直接拿剩余金额）
local remainAmount = tonumber(redis.call('get', remainAmountKey))
local currentAmount
if remainCount == 1 then
    currentAmount = remainAmount
else
    -- 二倍均值公式：随机区间[1分, 剩余金额/剩余人数 * 2]
    local maxAmount = math.floor(remainAmount / remainCount * 2)
    currentAmount = math.random(minAmount, maxAmount)
end

-- 4. 原子扣减库存、扣减金额、标记用户已抢
redis.call('decr', stockKey)
redis.call('decrby', remainAmountKey, currentAmount)
redis.call('sadd', userSetKey, userId)

-- 返回抢到的金额（单位：分）
return currentAmount
