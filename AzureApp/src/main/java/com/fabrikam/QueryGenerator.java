// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.fabrikam;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class QueryGenerator {


    public static void main(String[] args) {
//        List<String> input1 = readFromStdin(true, ",");
//        System.out.println("---------------------------");
//        List<String> input2 = readFromStdin(false, null);
//
//        Set<String> res1 = new TreeSet<>(input1);
//        Set<String> res2 = new TreeSet<>(input2);
//
//        compare(res1, res2);
//        StringBuilder output1 = buildOutput(new ArrayList<>(res1),  "", "", "\n", true);
//        StringBuilder output2 = buildOutput(new ArrayList<>(res2),  "", "", "\n", true);
//
//        System.out.println("===============================");
//        System.out.println(output1);
//        System.out.println("===============================");
//        System.out.println(output2);
        concatInputUsingComma();
    }

    private static StringBuilder buildOutput(List<String> lines, String prefix, String suffix, String concatSeperator, boolean isToLowerCase) {
        StringBuilder output = new StringBuilder();
        for(String line: lines) {
            output.append(prefix);
            output.append(isToLowerCase ? line.toLowerCase() : line);
            output.append(suffix);
            output.append(concatSeperator);
        }
        if(output.length()  > 0) {
            output.deleteCharAt(output.length() - 1);
        }

//        System.out.println("======================output===================");
//        System.out.println(output);

        return output;
    }

    private static Set<String> convertInputToSet(StringBuilder sb, String seperator) {
        Set<String> res = new TreeSet<>();
        for(String item : sb.toString().split(seperator)) {
            res.add(item.trim());
        }
        return res;
    }

    private static List<String> readFromStdin(boolean needSeperator, String seperator) {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();

        List<String> lines = new ArrayList<>();
        int linecount = 0;


        while(!line.isEmpty()) {
            if(needSeperator) {
                for(String item : line.split(seperator)) {
                    lines.add(item);
                }
            } else {
                lines.add(line);
            }
            linecount++;
            line = scanner.nextLine();
        }

        System.out.println("Total linecount count: " + linecount);

        return lines;
    }

    static private List<String> splitting(String s, String seperator) {
        String[] words = s.split(seperator);
        List<String> res = new ArrayList<>();
        for(String word : words) {
            res.add(word);
        }
        return res;
    }

    private static void concatInputUsingComma() {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();

        int linecount = 0;
        StringBuilder query = new StringBuilder();
        while(!line.isEmpty()) {
            linecount++;
            query.append("\"");
            query.append(line.toLowerCase());
            query.append("\"");
            query.append(",");

            line = scanner.nextLine();
        }
        if(query.length()  > 0) {
            query.deleteCharAt(query.length() - 1);
        }
        System.out.println("Total linecount count: " + linecount);
        System.out.println("======================query-20211021===================");
        System.out.println(query);
    }

    private static void compare(Set<String> set1, Set<String> set2) {
        System.out.println("===========set1 count = " + set1.size());
        System.out.println("===========set2 count = " + set2.size());
        for(String item : set1) {
            if(set2.contains(item)) {
                System.out.println(item);
            }
        }
    }

}
