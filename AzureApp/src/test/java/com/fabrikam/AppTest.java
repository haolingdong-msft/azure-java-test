package com.fabrikam;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest
{

    public void test() {
        int[] a = new int[10];
        int index = 0;
        for (int i = 0; i < 2 && index < a.length; i++, index++) {
            System.out.println(index);
        }
    }

    @Test
    public void testIndex() {
        test();
        test();

    }

    @Test
    public void testListRemove() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        for(Integer item : list) {
            if(item % 2 == 0) {
                list.remove(item);
            }
        }

//        System.out.println(list);
    }

}
