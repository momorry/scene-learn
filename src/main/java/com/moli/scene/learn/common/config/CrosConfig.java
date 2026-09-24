package com.moli.scene.learn.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CrosConfig {

    @Configuration
    public class CorsConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            //如果allowCredentials=true,那么allowedOriginPatterns不能为*，否则浏览器那边是会报错的
            registry.addMapping("/**")
                    //支持通配符
                    .allowedOriginPatterns("https://myapp.com", "https://www.myapp.com")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }
}
