// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.test;


import org.junit.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public class LockTest {

    @Test
    public void testLockAcquire() {
        Semaphore lock = new Semaphore(0);
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSupplier() {
//        Supplier<Integer> provider = () -> {
//            return 1;
//        };
//        System.out.println(provider.get());
        String s = "testhi😉";
        System.out.println(s.getBytes(StandardCharsets.UTF_8).length);
    }

}
