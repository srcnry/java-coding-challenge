package com.crewmeister.cmcodingchallenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    // A small dedicated pool for the startup data-loading job. It only needs one thread
    // to kick off the load — the real parallelism is handled within the loader itself.
    @Bean("startupLoader")
    public Executor startupLoaderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("startup-loader-");
        executor.initialize();
        return executor;
    }
}
