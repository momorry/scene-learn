package com.moli.scene.learn.service;

import com.moli.scene.learn.BaseTest;
import com.moli.scene.learn.common.dao.entity.TUsr;
import com.moli.scene.learn.common.dao.mapper.TUsrMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DelayDoubleDelCacheTest extends BaseTest {

    @Resource
    private UserService userService;
    @Resource
    private TUsrMapper tUsrMapper;

    /**
     * 数据库中已有ID=1，用户名称为李四的一条记录
     * 需求：
     * 帮我写一个100多线程并发读取ID=1的用户，1条线程中间修改ID=1的用户名称为张三的延迟双删除的用例。
     * 基于我的BaseTest的junit测试框架写，要求计算缓存一致的准确率
     * 应该在执行数据库更新操作后，统计期望与实际的用户名称是否相等，若不相等，则说明读的是旧值，则错误的计数器加1，然后用错误的次数除以总次数，才是错误率
     */
    @Test
    public void testDelayDoubleDelCache() throws InterruptedException {

        Long userId = 1L;
        String originalName = "李四";
        String updatedName = "张三";
        TUsr t = new TUsr();
        t.setId(1L);
        t.setUserName(originalName);
        tUsrMapper.updateById(t);

        // 写操作完成标记：写线程完成后置为 true，读线程据此判断期望值
        AtomicBoolean writeCompleted = new AtomicBoolean(false);
        // 写操作完成后的总读取次数
        AtomicInteger totalReadsAfterUpdate = new AtomicInteger(0);
        // 写操作完成后，仍读到旧值的错误次数
        AtomicInteger errorCount = new AtomicInteger(0);
        // 总读取次数（全程）
        AtomicInteger totalReads = new AtomicInteger(0);

        // 用于协调读写线程的启动时机
        CountDownLatch startLatch = new CountDownLatch(1);
        // 用于等待所有线程执行完毕
        CountDownLatch doneLatch = new CountDownLatch(101);

        // 读写线程共享的线程池
        ExecutorService readPool = Executors.newFixedThreadPool(100);

        // ===== 1. 启动 100 个读线程，持续读取 ID=1 的用户 =====
        for (int i = 0; i < 100; i++) {
            final int threadNo = i + 1;
            readPool.execute(() -> {
                try {
                    startLatch.await(); // 等待统一发令枪
                    // 每个读线程循环读取 100 次，每次间隔 20ms，总持续约 2 秒
                    for (int j = 0; j < 100; j++) {
                        TUsr tUsr = userService.queryUserById(userId);
                        totalReads.incrementAndGet();

                        // 写操作完成后，检查读到的是否为期望的新值
                        if (writeCompleted.get()) {
                            totalReadsAfterUpdate.incrementAndGet();
                            if (tUsr == null || !updatedName.equals(tUsr.getUserName())) {
                                // 写完后仍读到旧值或 null，说明缓存不一致
                                errorCount.incrementAndGet();
                            }
                        }
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("读线程{}被中断", threadNo, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // ===== 2. 启动 1 个写线程，在 500ms 后执行延迟双删除更新 =====
        Thread writeThread = new Thread(() -> {
            try {
                startLatch.await();
                // 等待 500ms，让读线程先充分读到旧缓存（李四），再执行更新
                Thread.sleep(500);
                log.info(">>> 写线程开始执行延迟双删除，将用户名从 {} 改为 {}", originalName, updatedName);
                userService.updateUserDelayDoubleDelete(userId, updatedName);
                // 标记写操作已完成，后续读线程将以「张三」为期望值
                writeCompleted.set(true);
                log.info(">>> 写线程更新完成，后续读取期望值: {}", updatedName);
                // 写完后继续等待一段时间，让读线程在更新后继续读取
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("写线程被中断", e);
            } finally {
                doneLatch.countDown();
            }
        }, "write-thread");
        writeThread.start();

        // ===== 3. 发令枪：所有线程同时开始 =====
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // ===== 4. 等待所有线程执行完毕 =====
        doneLatch.await(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        readPool.shutdown();

        // ===== 5. 统计并输出缓存错误率 =====
        int total = totalReads.get();
        int totalAfterUpdate = totalReadsAfterUpdate.get();
        int errors = errorCount.get();
        double errorRate = totalAfterUpdate > 0 ? (errors * 100.0 / totalAfterUpdate) : 0;
        double accuracy = 100.0 - errorRate;

        log.info("==================== 延迟双删除并发测试结果 ====================");
        log.info("全程总读取次数: {}", total);
        log.info("写操作完成后读取次数: {}", totalAfterUpdate);
        log.info("写操作完成后读到旧值(错误)次数: {}", errors);
        log.info("缓存错误率: {}%", String.format("%.2f", errorRate));
        log.info("缓存一致准确率: {}%", String.format("%.2f", accuracy));
        log.info("总耗时: {} ms", elapsed);
        log.info("===============================================================");

        // 断言：写操作完成后应有足够的读取次数
        org.junit.jupiter.api.Assertions.assertTrue(totalAfterUpdate > 0, "写操作完成后应有读取记录");
        // 断言：缓存错误率应低于 10%（延迟双删除策略下允许少量不一致）
        org.junit.jupiter.api.Assertions.assertTrue(errorRate < 10,
                String.format("缓存错误率 %.2f%% 超过预期 10%%", errorRate));
    }

}
