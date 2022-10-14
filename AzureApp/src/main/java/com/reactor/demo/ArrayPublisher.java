// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.reactor.demo;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ArrayPublisher<T> implements Publisher<T> {
    private final T[] data;

    public ArrayPublisher(T[] input) {
        this.data = input;
    }


    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            int index = 0;
            int requested = 0;
            @Override
            public void request(long l) {
                // WIP(working in progress) pattern
                int initialRequested = requested;
                requested += l;
                if(initialRequested != 0) {
                    // make sure not causing stack overflow, onNext() calls request(), request() calls onNext(), onNext() calls request(), if the two functions never return, so it causes stack overflow.
                    // 如果是从最初的onNext()的request()调用进来的，initialRequested就是0
                    return;
                }

                int sent = 0;
                for(; sent < requested && index < data.length; index++, sent++) {
                    subscriber.onNext(data[index]);
                }
                requested -= sent;
                subscriber.onComplete();
            }

            @Override
            public void cancel() {

            }
        });

    }
}
