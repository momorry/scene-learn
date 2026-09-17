package com.moli.scene.learn.common.config.demo;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;
import java.util.Arrays;


//@Configuration
public class RedisClusterConfig {

    @Bean
    public LettuceConnectionFactory clusterConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration(Arrays.asList(
                "192.168.2.10:6379",
                "192.168.2.11:6379",
                "192.168.2.12:6379"
            ));
        clusterConfig.setPassword("cluster_pass");
        // 开启自适应刷新，集群拓扑变化时自动感知
        ClusterTopologyRefreshOptions refreshOptions = 
            ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30))  // 每30秒检查一次
                .enableAllAdaptiveRefreshTriggers()             // 检测到MOVED/ASK等自动立即刷新
                .build();
        
        LettuceClientConfiguration clientConfig = 
            LettuceClientConfiguration.builder()
                .clientOptions(ClusterClientOptions.builder()
                    .topologyRefreshOptions(refreshOptions)
                    .build())
                .build();
                
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
