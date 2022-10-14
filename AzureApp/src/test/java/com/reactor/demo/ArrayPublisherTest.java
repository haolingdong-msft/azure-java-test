// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.reactor.demo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.ArrayList;
import java.util.List;


class ArrayPublisherTest extends PublisherVerification<Integer> {
    public ArrayPublisherTest() {
        super(new TestEnvironment());
    }

    @Test
    public void testArrayPublisherCorrectExecutionOrder() {
        ArrayPublisher<Integer> publisher = new ArrayPublisher<>(new Integer[]{1, 2, 3});
        List<String> calledMethods = new ArrayList<>();
        publisher.subscribe(new Subscriber<Integer>() {
            Subscription sub;

            @Override
            public void onSubscribe(Subscription subscription) {
                calledMethods.add("onSubscribe");
                subscription.request(3);
            }

            @Override
            public void onNext(Integer integer) {
                calledMethods.add("onNext " + integer);
            }

            @Override
            public void onError(Throwable throwable) {
                calledMethods.add("onError");
            }

            @Override
            public void onComplete() {
                calledMethods.add("onComplete");
            }
        });

        Assertions.assertThat(calledMethods).containsExactly("onSubscribe", "onNext 1", "onNext 2", "onNext 3", "onComplete");
    }

    // only get requested amount of data
    @Test
    public void testArrayPublisherBackpressure() {
        ArrayPublisher<Integer> publisher = new ArrayPublisher<>(new Integer[]{1, 2, 3});
        List<Integer> collected = new ArrayList<>();
        final Subscription[] subs = new Subscription[1];
        publisher.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subs[0] = subscription;
            }

            @Override
            public void onNext(Integer integer) {
                collected.add(integer);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
        subs[0].request(1);
        Assertions.assertThat(collected).containsExactly(1);
        subs[0].request(2);
        Assertions.assertThat(collected).containsExactly(1, 2, 3);

    }

    // test request() and onNext() cycle does not cause stack overflow
    @Test
    public void testArrayPublisherStackoverflow() {
        ArrayPublisher<Integer> publisher = new ArrayPublisher<>(new Integer[]{1, 2, 3, 4, 5, 6});

        List<Integer> collected = new ArrayList<>();
        publisher.subscribe(new Subscriber<Integer>() {
            Subscription sub;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.sub = subscription;
                sub.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                collected.add(integer);
                sub.request(1);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        Assertions.assertThat(collected).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Override
    public Publisher<Integer> createPublisher(long l) {
        return new ArrayPublisher<Integer>(new Integer[]{1, 2, 3, 4, 5, 6});
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }
}