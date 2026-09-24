package com.moli.scene.learn.controller;

import com.moli.scene.learn.common.base.ApiResult;
import com.moli.scene.learn.service.redpackage.RedPackMoneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 红包接口
 */
@RestController
@RequestMapping("/redpack")
@RequiredArgsConstructor
public class RedPackController {

    private final RedPackMoneyService redPackMoneyService;

    /**
     * 发红包
     *
     * @param senderId 发送者用户ID
     * @param count    红包个数
     * @param amount   红包总金额（单位：分）
     */
    @PostMapping("/delivery")
    public ApiResult<String> deliveryRedPack(@RequestParam Integer senderId,
                                             @RequestParam int count,
                                             @RequestParam int amount) {
        String redPackId = redPackMoneyService.deliveryRedPack(senderId, count, amount);
        if (redPackId == null) {
            return ApiResult.fail("发红包失败，请检查余额是否充足");
        }
        return ApiResult.ok(redPackId, "发红包成功");
    }

    /**
     * 抢红包
     *
     * @param redPackId 红包ID
     * @param userId    用户ID
     */
    @PostMapping("/acquire")
    public ApiResult<String> acquireRedPack(@RequestParam String redPackId,
                                            @RequestParam Integer userId) {
        Long result = redPackMoneyService.acquireRedPack(redPackId, userId);
        if (result == null) {
            return ApiResult.fail("抢红包异常，请稍后重试");
        }
        if (result == -1) {
            return ApiResult.fail("你已经抢过该红包了");
        }
        if (result == -2) {
            return ApiResult.fail("红包已抢光");
        }
        if (result == -3) {
            return ApiResult.fail("红包不存在");
        }
        return ApiResult.ok("抢到了 " + result + " 分（" + String.format("%.2f", result / 100.0) + " 元）");
    }

    /**
     * 查询用户余额
     */
    @GetMapping("/balance")
    public ApiResult<Map<String, Object>> queryBalance(@RequestParam Integer userId) {
        Long balance = redPackMoneyService.queryBalance(userId);
        return ApiResult.ok(Map.of(
                "userId", userId,
                "balance", balance,
                "balanceYuan", String.format("%.2f", balance / 100.0)
        ));
    }

    /**
     * 设置用户余额（测试用）
     */
    @PostMapping("/balance/set")
    public ApiResult<String> setBalance(@RequestParam Integer userId, @RequestParam long amount) {
        redPackMoneyService.setBalance(userId, amount);
        return ApiResult.ok("设置成功");
    }

    /**
     * 查询红包详情
     */
    @GetMapping("/detail")
    public ApiResult<Map<String, Object>> queryDetail(@RequestParam String redPackId) {
        Map<String, Object> detail = redPackMoneyService.queryRedPackDetail(redPackId);
        if (detail == null) {
            return ApiResult.fail("红包不存在");
        }
        return ApiResult.ok(detail);
    }
}
