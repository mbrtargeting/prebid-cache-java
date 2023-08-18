package org.prebid.cache.repository.memory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.prebid.cache.exceptions.PayloadWrapperPropertyException;
import org.prebid.cache.exceptions.RepositoryException;
import org.prebid.cache.model.PayloadWrapper;
import org.prebid.cache.repository.ReactiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import java.lang.ref.SoftReference;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
@ConditionalOnProperty(prefix = "spring.memcache", name = {"maxObjects"})
public class MemoryRepositoryImpl implements ReactiveRepository<PayloadWrapper, String> {
    private final MemoryRepositoryConfiguration config;
    private final Map<String, SoftReference<PayloadWrapper>> cache;
    private final DelayQueue<CachedItem> cleaningQueue;
    private Thread cleaner;

    @Autowired
    public MemoryRepositoryImpl(final MemoryRepositoryConfiguration config) {
        this.config = config;
        this.cache = new ConcurrentHashMap<>();
        this.cleaningQueue = new DelayQueue<>();

        initCleaner();
    }

    @Override
    public Mono<PayloadWrapper> save(PayloadWrapper wrapper) {
        long expiry;
        String normalizedId;

        try {
            expiry = wrapper.getExpiry();
            normalizedId = wrapper.getNormalizedId();
        } catch (PayloadWrapperPropertyException e) {
            log.error("Exception occured while getting payload wrapper property: '{}', cause: '{}'",
                    ExceptionUtils.getMessage(e), ExceptionUtils.getMessage(e));
            return Mono.empty();
        }

        if (cache.size() == config.getMaxObjects()) {
            log.warn("Cache is at max size of: '{}' objects", config.getMaxObjects());
            return Mono.error(new RepositoryException("Cache is full! Max size is " + config.getMaxObjects()));
        }

        return Mono.create(sink -> {
            var expiryTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(Duration.ofSeconds(expiry));
            var ref = new SoftReference<>(wrapper);
            this.cache.put(normalizedId, ref);
            this.cleaningQueue.put(new CachedItem(normalizedId, ref, expiryTime));
            sink.success(wrapper);
        });
    }

    @Override
    public Mono<PayloadWrapper> findById(String id) {
        var result = Optional.ofNullable(this.cache.get(id)).map(SoftReference::get).orElse(null);
        return Mono.justOrEmpty(result);
    }

    @PreDestroy
    public void destroy() {
        this.cleaner.interrupt();
    }

    private void initCleaner() {
        this.cleaner = new Thread(() -> {
            CachedItem cached;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    cached = this.cleaningQueue.take();
                    this.cache.remove(cached.key, cached.ref);
                    log.debug("Removing key: '{}' from cache, item is expired!", cached.key);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        this.cleaner.setName("memory-cache-cleaner");
        this.cleaner.setDaemon(true);
        this.cleaner.start();
    }

    private record CachedItem(
            @Getter String key,
            @Getter SoftReference<PayloadWrapper> ref,
            long expiryTime
    ) implements Delayed {

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            return Long.compare(expiryTime, ((CachedItem) o).expiryTime);
        }
    }
}
