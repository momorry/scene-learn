-- 入参：KEYS[1]=用户余额Key  KEYS[2]=红包库存Key  KEYS[3]=红包剩余金额Key
-- 入参：ARGV[1]=红包总金额(分)  ARGV[2]=红包个数
local balanceKey = KEYS[1]
local stockKey = KEYS[2]
local remainAmountKey = KEYS[3]
local totalAmount = tonumber(ARGV[1])
local count = tonumber(ARGV[2])

-- 1. 校验余额是否充足
local balance = tonumber(redis.call('get', balanceKey))
if balance == nil or balance < totalAmount then
    return -1 -- -1 代表余额不足
end

-- 2. 校验红包参数合法性
if totalAmount <= 0 or count <= 0 then
    return -2 -- -2 代表参数非法
end

-- 3. 每个红包至少 1 分，总金额不能小于红包个数
if totalAmount < count then
    return -3 -- -3 代表金额不足以分配
end

-- 4. 原子扣减余额、初始化红包库存和剩余金额
redis.call('decrby', balanceKey, totalAmount)
redis.call('set', stockKey, count)
redis.call('set', remainAmountKey, totalAmount)

-- 返回红包 ID（使用 stockKey 中嵌入的 ID）
return 1
