package com.moli.scene.learn.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

//@Component
public class RiskInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RiskInterceptor.class);

    /** 封禁阈值 */
    private static final int BAN_THRESHOLD = 35;
    /** 验证码阈值 */
    private static final int CHALLENGE_THRESHOLD = 20;

//    @Autowired
    private RedisTemplate<String, Object> redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String ip = getClientIp(request);
            String deviceId = request.getHeader("X-Device-Id");
            String userAgent = request.getHeader("User-Agent");

            int score = 0;

            // 规则1：同一IP频繁换UA（疑似代理池）
            String lastUA = (String) redis.opsForValue().get("ua:" + ip);
            if (lastUA != null && !lastUA.equals(userAgent)) {
                score += 20;
            }
            redis.opsForValue().set("ua:" + ip, userAgent, 10, TimeUnit.MINUTES);

            // 规则2：设备指纹异常（短时间内多个IP）
            if (deviceId != null) {
                String deviceKey = "device_ips:" + deviceId;
                Long ipCount = redis.opsForSet().size(deviceKey);
                if (ipCount != null && ipCount > 5) {
                    score += 15;
                }
                redis.opsForSet().add(deviceKey, ip);
                redis.expire(deviceKey, 10, TimeUnit.MINUTES);
            }

            if (log.isDebugEnabled()) {
                log.debug("风控评分: ip={}, deviceId={}, score={}", ip, deviceId, score);
            }

            // 根据分数做动作
            if (score >= BAN_THRESHOLD) {
                log.warn("风控拦截-临时封禁: ip={}, score={}", ip, score);
                return reject(response, "访问被拒绝");
            } else if (score >= CHALLENGE_THRESHOLD) {
                log.warn("风控拦截-需要验证: ip={}, score={}", ip, score);
                return reject(response, "需要验证");
            }
            return true;
        } catch (Exception e) {
            // Redis 异常时降级放行，避免影响正常请求
            log.error("风控拦截器异常，降级放行", e);
            return true;
        }
    }

    private boolean reject(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"" + msg + "\"}");
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理场景：取第一个非 unknown 的 IP
            for (String candidate : ip.split(",")) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty() && !"unknown".equalsIgnoreCase(trimmed)) {
                    return trimmed;
                }
            }
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
