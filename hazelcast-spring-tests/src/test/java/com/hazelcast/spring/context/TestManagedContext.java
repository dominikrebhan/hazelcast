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

package com.hazelcast.spring.context;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spring.CustomSpringExtension;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.hazelcast.internal.util.ExceptionUtil.sneakyThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith({SpringExtension.class, CustomSpringExtension.class})
@ContextConfiguration(locations = {"managedContext-applicationContext-hazelcast.xml"})
class TestManagedContext {

    @Autowired
    private HazelcastInstance instance1;

    @Autowired
    private HazelcastInstance instance2;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DummyTransactionManager transactionManager;

    @Autowired
    private SomeBean bean;

    @BeforeAll
    @AfterAll
    static void start() {
        Hazelcast.shutdownAll();
    }

    @Test
    void testSerialization() {
        instance1.getMap("test").put(1L, new SomeValue());
        SomeValue value = (SomeValue) instance1.getMap("test").get(1L);
        assertNotNull(value.context);
        assertNotNull(value.someBean);
        assertEquals(context, value.context);
        assertEquals(bean, value.someBean);
        assertTrue(value.init);
    }

    @Test
    void testDistributedTask() throws Exception {
        SomeTask task = (SomeTask) context.getBean("someTask");
        Future<Long> future1 = instance1.getExecutorService("test").submit(task);
        assertEquals(bean.value, future1.get().longValue());

        Future<Long> future2 = instance1.getExecutorService("test").submitToMember(new SomeTask(),
                instance2.getCluster().getLocalMember());
        assertEquals(bean.value, future2.get().longValue());
    }

    @Test
    void testTransactionalTask() throws Exception {
        Future<?> future = instance1.getExecutorService("test").submitToMember(new SomeTransactionalTask(),
                instance2.getCluster().getLocalMember());
        future.get();
        assertTrue(transactionManager.isCommitted(), "transaction manager could not proxy the submitted task.");
    }

    @Test
    void testRunnableTask() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        instance1.getExecutorService("test").submitToMember(new SomeRunnableTask(),
                instance2.getCluster().getLocalMember(), new ExecutionCallback<>() {
                    public void onResponse(Object response) {
                        latch.countDown();
                    }

                    public void onFailure(Throwable t) {
                        error.set(t);
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        Throwable throwable = error.get();
        if (throwable != null) {
            sneakyThrow(throwable);
        }
    }

    @Test
    void testRunnableTask_withScheduledExecutor_onLocalMember() throws Exception {
        instance1.getScheduledExecutorService("test")
                .scheduleOnMember(new SomeRunnableTask(), instance1.getCluster().getLocalMember(), 0, TimeUnit.SECONDS)
                .get();
    }

    @Test
    void testRunnableTask_withScheduledExecutor_onRemoteMember() throws Exception {
        instance1.getScheduledExecutorService("test")
                .scheduleOnMember(new SomeRunnableTask(), instance2.getCluster().getLocalMember(), 0, TimeUnit.SECONDS)
                .get();
    }

    @Test
    void testCallableTask_withScheduledExecutor_onLocalMember() throws Exception {
        instance1.getScheduledExecutorService("test")
                .scheduleOnMember(new SomeCallableTask(), instance1.getCluster().getLocalMember(), 0, TimeUnit.SECONDS)
                .get();
    }

    @Test
    void testCallableTask_withScheduledExecutor_onRemoteMember() throws Exception {
        instance1.getScheduledExecutorService("test")
                .scheduleOnMember(new SomeCallableTask(), instance2.getCluster().getLocalMember(), 0, TimeUnit.SECONDS)
                .get();
    }

    @Test
    void testTransactionalRunnableTask() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        instance1.getExecutorService("test").submitToMember(new SomeTransactionalRunnableTask(),
                instance2.getCluster().getLocalMember(), new ExecutionCallback<>() {
                    public void onResponse(Object response) {
                        latch.countDown();
                    }

                    public void onFailure(Throwable t) {
                    }
                });
        latch.await(1, TimeUnit.MINUTES);
        assertTrue(transactionManager.isCommitted(), "transaction manager could not proxy the submitted task.");
    }

    @Test
    void testEntryProcessor() {
        IMap<Object, Object> map = instance1.getMap("testEntryProcessor");
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        map.put("key4", "value4");
        map.put("key5", "value5");

        Map<Object, Object> objectMap = map.executeOnEntries(new SomeEntryProcessor());
        assertEquals(5, objectMap.size());
        for (Object o : objectMap.values()) {
            assertEquals("notNull", o);
        }

        Object result = map.executeOnKey("key8", new SomeEntryProcessor());
        assertEquals("notNull", result);
    }
}
