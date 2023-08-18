package org.prebid.cache.repository;

import org.prebid.cache.repository.memory.MemoryRepositoryConfiguration;
import org.prebid.cache.repository.memory.MemoryRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ReactiveMemoryRepositoryTestContext {

    @Bean
    @Primary
    public ReactiveRepository<?, ?> createRepository() {
        return new MemoryRepositoryImpl(memoryRepositoryConfiguration());
    }

    @Bean
    MemoryRepositoryConfiguration memoryRepositoryConfiguration() {
        return new MemoryRepositoryConfiguration(10);
    }
}
