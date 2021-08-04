package com.fabrikam;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AzureAppTest {
    @Test
    public void testStream() {
        List<List<Integer>> list = new ArrayList<>();
        list.add(Arrays.asList(1, 2));
        list.stream().flatMap(x -> x.stream());
        System.out.println(list);
    }

    @Test
    public void test
}
