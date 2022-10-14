// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.test;

import java.util.concurrent.Semaphore;

public class LockTest {
    public static void main(String[] args) {
        System.out.println("lockTest here xxxxxxxx");
        Semaphore lock = new Semaphore(0);
        try {
            lock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
