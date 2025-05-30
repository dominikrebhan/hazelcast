/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.config;

import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MergePolicyConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.internal.config.mergepolicies.ComplexCustomMergePolicy;
import com.hazelcast.spi.merge.MergingCosts;
import com.hazelcast.spi.merge.MergingExpirationTime;
import com.hazelcast.spi.merge.SplitBrainMergeTypes.MapMergeTypes;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the integration of the {@link MergePolicyValidator}
 * into the proxy creation of split-brain capable data structures.
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category({QuickTest.class, ParallelJVMTest.class})
public class MergePolicyValidatorCacheIntegrationTest extends AbstractMergePolicyValidatorIntegrationTest {

    @Override
    protected void addConfig(Config config, String name, MergePolicyConfig mergePolicyConfig) {
        CacheSimpleConfig cacheSimpleConfig = new CacheSimpleConfig();
        cacheSimpleConfig.setName(name);
        cacheSimpleConfig.setStatisticsEnabled(false);
        cacheSimpleConfig.getMergePolicyConfig().setPolicy(mergePolicyConfig.getPolicy());

        config.addCacheConfig(cacheSimpleConfig);
    }

    @Test
    public void testCache_withPutIfAbsentMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("putIfAbsent", putIfAbsentMergePolicy);

        hz.getCacheManager().getCache("putIfAbsent");
    }

    @Test
    public void testCache_withHyperLogLogMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("cardinalityEstimator", hyperLogLogMergePolicy);

        expectCardinalityEstimatorException(() -> hz.getCacheManager().getCache("cardinalityEstimator"));
    }

    @Test
    public void testCache_withHigherHitsMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("higherHits", higherHitsMergePolicy);

        hz.getCacheManager().getCache("higherHits");
    }

    @Test
    public void testCache_withInvalidMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("invalid", invalidMergePolicyConfig);

        expectedInvalidMergePolicyException(() -> hz.getCacheManager().getCache("invalid"));
    }

    @Test
    public void testCache_withExpirationTimeMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("expirationTime", expirationTimeMergePolicy);

        hz.getCacheManager().getCache("expirationTime");
    }

    /**
     * ICache provides only the required {@link MergingExpirationTime},
     * but not the required {@link MergingCosts} from the
     * {@link ComplexCustomMergePolicy}.
     * <p>
     * The thrown exception should contain the merge policy name
     * and the missing merge type.
     */
    @Test
    public void testCache_withComplexCustomMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("complexCustom", complexCustomMergePolicy);

        assertThatThrownBy(() -> hz.getCacheManager().getCache("complexCustom"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(complexCustomMergePolicy.getPolicy())
                .hasMessageContaining(MergingCosts.class.getName());
    }

    /**
     * ICache provides only some of the required merge types
     * of {@link MapMergeTypes}.
     * <p>
     * The thrown exception should contain the merge policy name
     * and the missing merge type.
     */
    @Test
    public void testCache_withCustomMapMergePolicyNoTypeVariable() {
        HazelcastInstance hz = getHazelcastInstance("customMapNoTypeVariable", customMapMergePolicyNoTypeVariable);

        assertThatThrownBy(() -> hz.getCacheManager().getCache("customMapNoTypeVariable"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(customMapMergePolicyNoTypeVariable.getPolicy())
                .hasMessageContaining(MapMergeTypes.class.getName());
    }

    /**
     * ICache provides only some of the required merge types
     * of {@link MapMergeTypes}.
     * <p>
     * The thrown exception should contain the merge policy name
     * and the missing merge type.
     */
    @Test
    public void testCache_withCustomMapMergePolicy() {
        HazelcastInstance hz = getHazelcastInstance("customMap", customMapMergePolicy);

        assertThatThrownBy(() -> hz.getCacheManager().getCache("customMap"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(customMapMergePolicy.getPolicy())
                .hasMessageContaining(MapMergeTypes.class.getName());
    }

    @Override
    protected void expectCardinalityEstimatorException(ThrowingCallable toRun) {
        assertThatThrownBy(toRun)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CardinalityEstimator");
    }

    @Override
    protected void expectedInvalidMergePolicyException(ThrowingCallable toRun) {
        assertThatThrownBy(toRun)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(invalidMergePolicyConfig.getPolicy());
    }
}
