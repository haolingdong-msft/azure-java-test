// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.fabrikam;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TrackOneDocsFilter {
    private static String timeStamp = new SimpleDateFormat("yyyy.MM.dd.hh.mm").format(new Date());

    enum TrackOneDocHeadersEnum {
        ID, WorkItemType, Title, AssignedTo, State, Tags, SourceUrl, BrowserUrl
    }

    public static void main(String[] args) throws IOException {
//        System.out.println(isSourceUrl404("https://github.com/MicrosoftDocs/azure-docs-pr/blob/master/articles/virtual-machines/windows/java.md"));
//        System.out.println(isBrowserUrl404("https://docs.microsoft.com/azure/developer/java/java-sdk-manage-virtual-machines"));
        // 1. read the list of docs being detected
        List<Map<TrackOneDocHeadersEnum, String>> tasks = parseTrackDocTaskList();
        // 2. Filter out 404 docs
        tasks = filterOut404Docs(tasks);
        // 3. output result
        outputResult(tasks);

    }

    private static List<Map<TrackOneDocHeadersEnum, String>> filterOut404Docs(List<Map<TrackOneDocHeadersEnum, String>> tasks) {
        List<Map<TrackOneDocHeadersEnum, String>> filteredTasks = new ArrayList<>();
        for (Map<TrackOneDocHeadersEnum, String> task : tasks) {
            String sourceUrl = task.get(TrackOneDocHeadersEnum.SourceUrl);
            String browserurl = task.get(TrackOneDocHeadersEnum.BrowserUrl);
            if (!isBrowserUrl404(browserurl)) {
                filteredTasks.add(task);
            }
        }
        System.out.println("Remaining tasks size after filtering out 404 docs: " + filteredTasks.size());
        return filteredTasks;
    }

    private static List<Map<TrackOneDocHeadersEnum, String>> parseTrackDocTaskList() throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please input file path of track 1 task list:");
        String filepath = scanner.nextLine();
        filepath = parseFileName(filepath);

        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(filepath)), "UTF-8");
        CSVParser csvParser = CSVFormat.DEFAULT.withHeader(TrackOneDocHeadersEnum.class).parse(isr);

        List<Map<TrackOneDocHeadersEnum, String>> tasks = new ArrayList<>();
        for (CSVRecord record : csvParser) {
            Map<TrackOneDocHeadersEnum, String> task = new HashMap<>();
            String id = record.get(TrackOneDocHeadersEnum.ID);
            String sourceUrl = record.get(TrackOneDocHeadersEnum.SourceUrl);
            String browserUrl = record.get(TrackOneDocHeadersEnum.BrowserUrl);

            task.put(TrackOneDocHeadersEnum.ID, id);
            task.put(TrackOneDocHeadersEnum.SourceUrl, sourceUrl);
            task.put(TrackOneDocHeadersEnum.BrowserUrl, browserUrl);

            tasks.add(task);

        }

        System.out.println("Total task number: " + tasks.size());
        return tasks;

    }


    private static boolean isSourceUrl404(String url) {
        System.setProperty("webdriver.edge.driver", "C:\\workspace\\edgedriver_win64\\msedgedriver.exe");
        System.setProperty("webdriver.chrome.driver", "C:\\workspace\\chromedriver_win32\\chromedriver.exe");

        WebDriver driver = new EdgeDriver();
        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            Thread.sleep(1000);
            if (driver.findElement(By.xpath("//img[contains(@alt, '404')]")) !=
                    null) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            driver.close();
        }

        return false;
    }

    private static boolean isBrowserUrl404(String url) {
        System.setProperty("webdriver.edge.driver", "C:\\workspace\\edgedriver_win64\\msedgedriver.exe");
        System.setProperty("webdriver.chrome.driver", "C:\\workspace\\chromedriver_win32\\chromedriver.exe");

        WebDriver driver = new EdgeDriver();
        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            Thread.sleep(1000);

//            System.out.println(driver.getCurrentUrl());
            if (driver.getCurrentUrl().equals("https://docs.microsoft.com/en-us/azure/developer/java/sdk/")) {
                return true;
            }

            if (driver.findElement(By.xpath("//h1[contains(text(), '404')]")) !=
                    null) {
                return true;
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            driver.close();
        }

        return false;
    }


    private static String parseFileName(String inputFileName) {
        String res = "";
        if (inputFileName.startsWith("\"") && inputFileName.endsWith("\"")) {
            res = inputFileName.substring(1, inputFileName.length() - 1);
        }
        return res;
    }

    private static void outputResult(List<Map<TrackOneDocHeadersEnum, String>> tasks) {
        for (Map<TrackOneDocHeadersEnum, String> task : tasks) {
            System.out.println(task.get(TrackOneDocHeadersEnum.BrowserUrl));
        }
        String outputFilePath =
                "C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\DocsToBeUpdatedList-" +
                        timeStamp + ".csv";
        try (CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader("ID", "Title", "SourceUrl", "BrowserUrl")
                .print(new File(outputFilePath), Charset.forName("UTF-8"))) {
            for (Map<TrackOneDocHeadersEnum, String> task : tasks) {
                csvPrinter.printRecord(Arrays.asList(task.get(TrackOneDocHeadersEnum.ID), task.get(TrackOneDocHeadersEnum.Title), task.get(TrackOneDocHeadersEnum.SourceUrl), task.get(TrackOneDocHeadersEnum.BrowserUrl)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
