package com.fabrikam;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

public class AzureAppTest {
    @Test
    public void testStream() {
        List<List<Integer>> list = new ArrayList<>();
        list.add(Arrays.asList(1, 2));
        list.stream().flatMap(x -> x.stream());
        System.out.println(list);
    }

    @Test
    public void testMapRemove() {
        Map<String, String> map = new TreeMap<>();
        map.put("a", "b");
        map.remove("c");
        System.out.println(map);
    }

    @Test
    public void testMono() {
        inner();
        System.out.println("complete");
    }

    @Test
    public void testCountdownLatch() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            System.out.println("come to thread inner, count down");
            countDownLatch.countDown();
        });
        t.run();
        countDownLatch.await();
    }

    @Test
    public void testDynamicallyEmitValueToFlux() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Sinks.Many<Integer> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<Integer> flux = sink.asFlux();
        flux.subscribe((count) -> {
//            countDownLatch.countDown();
            System.out.println(count);
        });
        sink.tryEmitNext(0);
        sink.tryEmitNext(1);

//        countDownLatch.await();
    }

    private Mono<String> inner() {
        System.out.println("begin inner");
        Mono<String> res = Mono.delay(Duration.ofSeconds(1))
                .then(delayInner("first"))
                .then(Mono.just("second"))
                .map(test -> {
                    System.out.println("inner() second");
                    return test;
                });
        System.out.println("end inner");
        return res;
    }

    private Mono<String> delayInner(String text) {
        return Mono.delay(Duration.ofSeconds(5)).then(Mono.just(text)).map(test -> {
            System.out.println("delayInner() " + text);
            return test;
        });
    }

    @Test
    public void testMonoThen() {
        System.out.println("begin testMonoThen");
        String res = inner().block();
        System.out.println(res);
        System.out.println("end testMonoThen");
    }

    public Mono<ArrayList<String>> commitAsync() {
        System.out.println("begin concat()");
        Flux<String> res = Flux.concat(delayInner("first"), delayInner("second"));
        Mono<ArrayList<String>> ret = res.collect(() -> new ArrayList<String>(),
                (state, item) ->
                        state.add(item));
        System.out.println("end concat()");
        return ret;
    }

    public Mono<String> then() {
        System.out.println("begin then()");
        Mono<String> res = Mono.delay(Duration.ofSeconds(1))
                .then(commitAsync())
                .then(Mono.just("third"))
                .map(test -> {
                    System.out.println("then() third");
                    return test;
                });
        System.out.println("end then()");
        return res;
    }

    @Test
    public void testThenAndConcat() {
        System.out.println("begin testThenAndConcat()");
        then().block();
        System.out.println("end testThenAndConcat()");
    }

}
