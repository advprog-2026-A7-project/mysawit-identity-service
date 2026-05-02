package com.mysawit.identity.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitBucketStoreTest {

    @Test
    void resolveBucketCreatesBucketWhenAbsent() {
        RateLimitBucketStore store = new RateLimitBucketStore();

        Bucket bucket = store.resolveBucket("1.2.3.4");

        assertThat(bucket).isNotNull();
        assertThat(bucket.getAvailableTokens()).isEqualTo(5);
    }

    @Test
    void resolveBucketReusesBucketForSameKey() {
        RateLimitBucketStore store = new RateLimitBucketStore();

        Bucket first = store.resolveBucket("1.2.3.4");
        first.tryConsume(1);
        Bucket second = store.resolveBucket("1.2.3.4");

        assertThat(second).isSameAs(first);
        assertThat(second.getAvailableTokens()).isEqualTo(4);
    }

    @Test
    void resolveBucketCreatesDistinctBucketsForDifferentKeys() {
        RateLimitBucketStore store = new RateLimitBucketStore();

        Bucket a = store.resolveBucket("1.1.1.1");
        Bucket b = store.resolveBucket("2.2.2.2");

        assertThat(a).isNotSameAs(b);
    }

    @Test
    void clearRemovesAllBuckets() {
        RateLimitBucketStore store = new RateLimitBucketStore();
        Bucket original = store.resolveBucket("1.2.3.4");
        original.tryConsume(5);
        assertThat(original.getAvailableTokens()).isZero();

        store.clear();

        Bucket fresh = store.resolveBucket("1.2.3.4");
        assertThat(fresh).isNotSameAs(original);
        assertThat(fresh.getAvailableTokens()).isEqualTo(5);
    }
}
