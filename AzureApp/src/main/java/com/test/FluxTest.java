// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.test;

import com.azure.core.util.logging.ClientLogger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class FluxTest {
    private static final ClientLogger logger = new ClientLogger(FluxTest.class);

    public static void main(String[] args) throws InterruptedException {

        // Test Schedulers
//        singleScheduler();

//        newSingleScheduler();

//        boundedElasticScheduler();

//        parallelScheduler();

        // Test interval, generate
//        testInterval();

//        testGenerate();

        testContext();

    }

    /**
     * Test no scheduler specified
     * the subscribers will be executed in the main thread
     *
     * @output [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - 1
     * [main] INFO com.test.FluxTest - 2
     * [main] INFO com.test.FluxTest - 3
     * [main] INFO com.test.FluxTest - end
     */
    private static void noScheduler() {
        logger.info("start");
        Flux.fromArray(new Integer[]{1, 2, 3})
                .subscribe(i -> logger.info(i + ""));
        logger.info("end");
    }

    /**
     * Test Schedulers.single().
     * Schedulers.single() will create a single thread to execute the subscribers, if there are multiple subscribers, all the subscribers will be executed in this single thread.
     * Schedulers.single() will create a daemon thread, which will not block main thread from exit.
     * So the behavior is the program will exit immediately. Unless you sleep at the end of the program.
     *
     * @output [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - end
     */
    private static void singleScheduler() {
        logger.info("start");
        Flux.fromArray(new Integer[]{1, 2, 3})
                .publishOn(Schedulers.single())
                .subscribe(i -> logger.info(i + ""));
        logger.info("end");
    }

    /**
     * Test Schedulers.newSingle().
     * Similar as Schedulers.single(), it will create a single thread to execute the subscribers, if there are multiple subscribers, all the subscribers will be executed in this single thread.
     * The difference between newSingle() and single() is: newSingle() will create a non-daemon thread, which will block main thread from exit.
     * So the program will not exit.
     *
     * @output [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - end
     * [test-1] INFO com.test.FluxTest - 1
     * [test-1] INFO com.test.FluxTest - 2
     * [test-1] INFO com.test.FluxTest - 3
     */
    private static void newSingleScheduler() {
        logger.info("start");
        Flux.fromArray(new Integer[]{1, 2, 3})
                .publishOn(Schedulers.newSingle("test"))
                .subscribe(i -> logger.info(i + ""));
        logger.info("end");
    }

    /**
     * Test Schedulers.boundedElastic().
     * Schedulers.boundedElastic() will create a bounded elastic thread pool to execute the subscribers.
     * If there are multiple subscribers, all the subscribers will be executed using one of the threads in the thread pool.
     * The output shows the two subscribers are using difference thread.
     * If we don't add sleep at the end, the program will exit, so we add a sleep at the end.
     *
     * @output [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - end
     * [boundedElastic-1] INFO com.test.FluxTest - 1 first subscriber
     * [boundedElastic-1] INFO com.test.FluxTest - 2 first subscriber
     * [boundedElastic-2] INFO com.test.FluxTest - 1 second subscriber
     * [boundedElastic-1] INFO com.test.FluxTest - 3 first subscriber
     * [boundedElastic-2] INFO com.test.FluxTest - 2 second subscriber
     * [boundedElastic-2] INFO com.test.FluxTest - 3 second subscriber
     */
    private static void boundedElasticScheduler() throws InterruptedException {
        logger.info("start");
        Flux<Integer> flux = Flux.fromArray(new Integer[]{1, 2, 3})
                .publishOn(Schedulers.boundedElastic());

        flux.subscribe(i -> logger.info(i + " first subscriber"));
        flux.subscribe(i -> logger.info(i + " second subscriber"));

        logger.info("end");
        Thread.sleep(2000);
    }

    /**
     * Test Schedulers.parallel()
     * The differences between boundedElastic() and parallel() is parallel() creates a fixed pool to execute subscribers. It creates as many workers as you have CPU cores.
     * If there are multiple subscribers, all the subscribers will be executed using one of the threads in the thread pool.
     *
     * @output [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - end
     * [parallel-2] INFO com.test.FluxTest - 1 second subscriber
     * [parallel-1] INFO com.test.FluxTest - 1 first subscriber
     * [parallel-2] INFO com.test.FluxTest - 2 second subscriber
     * [parallel-1] INFO com.test.FluxTest - 2 first subscriber
     * [parallel-2] INFO com.test.FluxTest - 3 second subscriber
     * [parallel-1] INFO com.test.FluxTest - 3 first subscriber
     */
    private static void parallelScheduler() throws InterruptedException {
        logger.info("start");
        Flux<Integer> flux = Flux.fromArray(new Integer[]{1, 2, 3})
                .publishOn(Schedulers.parallel());

        flux.subscribe(i -> logger.info(i + " first subscriber"));
        flux.subscribe(i -> logger.info(i + " second subscriber"));

        logger.info("end");
        Thread.sleep(2000);
    }

    /**
     * Default scheduler for Flux.interval is Scheduler.parallel(), so we need to sleep at the end of the program to avoid main thread from exit.
     * We can also change scheduler in Flux.interval() like this: Flux.interval(Duration.ofMillis(300), Schedulers.newSingle("test"))
     *
     * @output [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - end
     * [parallel-1] INFO com.test.FluxTest - 0
     * [parallel-1] INFO com.test.FluxTest - 1
     * [parallel-1] INFO com.test.FluxTest - 2
     */
    private static void testInterval() throws InterruptedException {
        logger.info("start");
        Flux.interval(Duration.ofMillis(300))
                .take(3)
                .subscribe(i -> logger.info(i + ""));
        logger.info("end");
        Thread.sleep(2000);
    }

    /**
     * Flux.generate() can be used to programmatically create sequence, you can decide what to emit next.
     * It will execute the call back until sink.complete() is called.
     * The program will not exit if we don't call sink.complete().
     * By default it runs on main thread.
     *
     * @output
     * [main] INFO com.test.FluxTest - start
     * [main] INFO com.test.FluxTest - 3 x 0 = 0
     * [main] INFO com.test.FluxTest - 3 x 1 = 3
     * [main] INFO com.test.FluxTest - 3 x 2 = 6
     * [main] INFO com.test.FluxTest - 3 x 3 = 9
     * [main] INFO com.test.FluxTest - end
     */
    private static void testGenerate() {
        logger.info("start");
        Flux<String> flux = Flux.generate(
            () -> 0,
            (state, sink) -> {
                sink.next("3 x " + state + " = " + 3 * state);
                if (state == 3) sink.complete();
                return state + 1;
            });
        flux.subscribe(i -> logger.info(i));
        logger.info("end");
    }

    private static void testContext() {
        String key = "message";
        Mono<String> r = Mono
                .deferContextual(ctx -> Mono.just("Hello " + ctx.get(key)))
                .contextWrite(ctx -> ctx.put(key, "Reactor"));
        System.out.println(r.block());
        System.out.println();
    }
}
