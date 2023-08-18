package org.prebid.cache.functional

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.prebid.cache.functional.BaseSpec.Companion.prebidCacheConfig
import org.prebid.cache.functional.service.ApiException
import org.prebid.cache.functional.util.getRandomUuid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

class MemoryCacheSpec: ShouldSpec({
    should("throw an exception when cache record is absent in memory cache") {
        // Given: Prebid cache config
        val config = prebidCacheConfig.getBaseMemcacheConfig("true")
        val cachePrefix = config["cache.prefix"]

        // When: GET cache endpoint with random UUID is called
        val randomUuid = getRandomUuid()
        val exception = shouldThrowExactly<ApiException> { BaseSpec.getPrebidCacheApi(config).getCache(randomUuid) }

        // Then: Not Found exception is thrown
        assertSoftly {
            exception.statusCode shouldBe HttpStatus.NOT_FOUND.value()
            exception.responseBody shouldContain "\"message\":\"Resource Not Found: uuid $cachePrefix$randomUuid\""
        }
    }

    should("throw an exception when the cache is full") {
        // Given: Prebid cache config with max of 1 objects
        val config = prebidCacheConfig.getBaseMemcacheConfig("true", "1")
        val prebidCacheApi = BaseSpec.getPrebidCacheApi(config)

        // And: the cache is full
        prebidCacheApi.getCache(getRandomUuid())

        // When: Another entry is inserted into the cache
        val exception = shouldThrowExactly<ApiException> { prebidCacheApi.getCache(getRandomUuid()) }

        // Then: An internal server error is thrown
        assertSoftly {
            exception.statusCode shouldBe INTERNAL_SERVER_ERROR.value()
            exception.responseBody shouldContain "Cache is full"
        }
    }
})