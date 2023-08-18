package org.prebid.cache.repository.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(prefix = "spring.memcache", name = {"maxObjects"})
@ConfigurationProperties(prefix = "spring.memcache")
public class MemoryRepositoryConfiguration {
    @NotNull
    private int maxObjects;
}
