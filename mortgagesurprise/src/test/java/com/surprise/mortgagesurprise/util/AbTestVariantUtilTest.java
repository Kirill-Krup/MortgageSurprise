package com.surprise.mortgagesurprise.util;

import com.surprise.mortgagesurprise.model.entity.AbTestVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.surprise.mortgagesurprise.util.AbTestVariantUtil.calculateBucket;
import static com.surprise.mortgagesurprise.util.AbTestVariantUtil.pickVariant;
import static org.assertj.core.api.Assertions.assertThat;

class AbTestVariantUtilTest {

    private static AbTestVariant variant(int id, String name, int percent) {
        var v = new AbTestVariant("exp", name, percent, 0);
        v.setId(id);
        return v;
    }

    @Test
    @DisplayName("calculateBucket is stable and always within [0, 99]")
    void bucketIsStableAndInRange() {
        for (int i = 0; i < 1_000; i++) {
            String id = UUID.randomUUID().toString();
            int bucket = calculateBucket(id, "exp");
            assertThat(bucket).isBetween(0, 99);
            assertThat(calculateBucket(id, "exp")).isEqualTo(bucket);
        }
    }

    @Test
    @DisplayName("100/0 split always selects the 100% variant")
    void fullPercentAlwaysWins() {
        var a = variant(1, "a", 100);
        var b = variant(2, "b", 0);
        for (int i = 0; i < 500; i++) {
            assertThat(pickVariant(UUID.randomUUID().toString(), "exp", List.of(a, b))).contains(a);
        }
    }

    @Test
    @DisplayName("70/30 split is respected on a large sample")
    void distributionMatchesPercentages() {
        var a = variant(1, "a", 70);
        var b = variant(2, "b", 30);
        int countA = 0;
        int total = 10_000;
        for (int i = 0; i < total; i++) {
            if (pickVariant(UUID.randomUUID().toString(), "exp", List.of(a, b)).orElseThrow() == a) {
                countA++;
            }
        }
        assertThat(countA).isBetween(6_500, 7_500);
    }

    @Test
    @DisplayName("result does not depend on the order of the collection (DB returns an unordered Set)")
    void orderIndependent() {
        var a = variant(1, "a", 40);
        var b = variant(2, "b", 35);
        var c = variant(3, "c", 25);
        for (int i = 0; i < 500; i++) {
            String id = UUID.randomUUID().toString();
            var forward = new ArrayList<>(List.of(a, b, c));
            var reversed = new ArrayList<>(forward);
            Collections.reverse(reversed);
            assertThat(pickVariant(id, "exp", reversed)).isEqualTo(pickVariant(id, "exp", forward));
        }
    }

    @Test
    @DisplayName("empty or under-100% distribution yields no variant for uncovered buckets")
    void uncoveredBucketReturnsEmpty() {
        assertThat(pickVariant("any", "exp", List.of())).isEmpty();

        var partial = List.of(variant(1, "a", 10));
        boolean anyEmpty = false;
        for (int i = 0; i < 1_000 && !anyEmpty; i++) {
            anyEmpty = pickVariant(UUID.randomUUID().toString(), "exp", partial).isEmpty();
        }
        assertThat(anyEmpty).isTrue();
    }
}
