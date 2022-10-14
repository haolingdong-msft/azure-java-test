// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.fabrikam;

import com.microsoft.azure.kusto.data.ClientImpl;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NewCustomerDataAnalyzer {
    private static final InputStream IN = System.in;
    private static final PrintStream OUT = System.out;

    private static final int SUBSCRIPTION_PER_QUERY = 2000;

    enum CCIDHeadersEnum {
        CCID, CustomerName, SubscriptionId, Track1Request, Track2Request
    }

    enum KustoHeadersEnum {
        operationName, targetResourceProvider, userAgent, subscriptionId, requestCount
    }

    private static int[] dayOfMonthDelimiter = {10, 20};
    private static Map<String, String> subIdToCCIDMap = new HashMap<>();
    private static Map<String, Set<String>> ccidToSubIdsMap = new HashMap<>();
    private static Map<String, String> CCIDToCustomerMap = new HashMap<>();
    private static Set<String> subscriptionsSet = new HashSet<>();

    private static Map<String, Integer> RPMap = new HashMap<>();
    private static Map<String, Integer> userAgentMap = new HashMap<>();
    private static Map<String, Set<String>> userAgentVersionToSubscriptionListMap = new HashMap<>();
    private static Map<String, Set<String>> subIdToUserAgentVersionMap = new HashMap<>();
    private static Map<String, Integer> subIdAndVersionCallCountMap = new HashMap<>();
    private static Map<String, Set<String>> subscriptionIdToRPMap = new HashMap<>();

    private static String timeStamp = new SimpleDateFormat("yyyy.MM.dd.hh.mm").format(new Date());
    private static String kustoDownloadPath = "C:\\Users\\haolingdong\\Downloads";
    private static String generatedSummaryPath = "C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\generated_summary\\";

    public static void main(String[] args) throws IOException, ParseException {

//        executeKustoQuery("ARMProd", "");

//        analyze();

        // 0. input last day of the month, in format: 2021-09-30
        Scanner scanner = new Scanner(IN);
        OUT.println("Please input the last day of the month, format as yyyy-mm-dd, e.g. 2021-11-30");
        String lastDayOfMonth = scanner.nextLine();
//        String lastDayOfMonth = "2021-10-31";
        // 1. parse CCIDSub file
        parseCCIDSubFile();
        // 2. build query
        List<String> query = buildQuery(lastDayOfMonth);
        // 3. execute kusto query
        List<File> kustoResultFiles = executeKustoQueries(query);
//
//        File kustoResult = new File("C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\generated_summary\\kusto\\2021.10.27.12.32 - 6.csv");
////        List<File> results = Arrays.asList(kustoResult);
        // 4. parse kusto result file
        parseKustoResultFiles(kustoResultFiles);
//        parseKustoResultFiles("C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\generated_summary\\kusto\\2022.01.17.10.15\\");
//        parseKustoResultFiles("C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\generated_summary\\kusto\\2021.10.21");

        // 5. output as csv
        outputResult();

    }

    private static void parseCCIDSubFile() throws IOException {
        Scanner scanner = new Scanner(IN);

        OUT.println("Please input file path of CCID and subscription relation file path");
        String CCIDSubFilePath = scanner.nextLine();
        CCIDSubFilePath = parseFileName(CCIDSubFilePath);

        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(CCIDSubFilePath)), "GBK");
        CSVParser csvParserForCCIDFile = CSVFormat.DEFAULT.withHeader(CCIDHeadersEnum.class).parse(isr);

        int recordCount = 0;
//        int ccidCount = 0;
        for (CSVRecord record : csvParserForCCIDFile) {
            recordCount++;
            if (record.size() < csvParserForCCIDFile.getHeaderMap().size()) {
                continue;
            }
            String ccid = record.get(CCIDHeadersEnum.CCID);
            if (ccid.isEmpty()) {
                continue;
            }
            String subscriptionId = record.get(CCIDHeadersEnum.SubscriptionId);
            if(subscriptionId == null || subscriptionId.isEmpty()) {
                continue;
            }
            String customer = record.get(CCIDHeadersEnum.CustomerName);
            subIdToCCIDMap.put(subscriptionId, ccid);
            CCIDToCustomerMap.put(ccid, customer);
            Set<String> subs = ccidToSubIdsMap.getOrDefault(ccid, new HashSet<>());
            subs.add(subscriptionId);
            ccidToSubIdsMap.put(ccid, subs);
            subscriptionsSet.add(subscriptionId);
//            ccidCount = ccidToSubIdsMap.size();
//            if (ccidCount == 100) {
//                break;
//            }
        }

        OUT.println("Total record count in CCIDSub file: " + recordCount);
        OUT.println("Total CCID count: " + ccidToSubIdsMap.size());
        OUT.println("Total subscription count: " + subscriptionsSet.size());
    }

    private static List<String> buildQuery(String lastDayOfMonth) throws IOException, ParseException {
//        FileWriter fileWriter = new FileWriter("./AzureApp/query/query-"+timeStamp);
        // prepare query file
        FileWriter fileWriter = new FileWriter(generatedSummaryPath + "query\\" + timeStamp);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("Total CCID count: " + ccidToSubIdsMap.size());
        printWriter.println("Total subscription count: " + subscriptionsSet.size());

        List<String> queries = new ArrayList<>();
        List<String> subscriptionsList = new ArrayList<>(subscriptionsSet);
        for (int i = 0; i < subscriptionsList.size(); i = i + SUBSCRIPTION_PER_QUERY) {
            List<String> subscriptions = subscriptionsList.subList(i, Math.min(
                    i + SUBSCRIPTION_PER_QUERY, subscriptionsList.size()));
            List<String> timestamps = getTimeSlices(lastDayOfMonth);
            for (int j = 0; j < timestamps.size() - 1; ++j) {
                String query = buildOneQuery(subscriptions, timestamps.get(j), timestamps.get(j + 1));
                queries.add(query);
                printWriter.println(query);
            }
        }
        printWriter.close();
        return queries;
    }

    private static String buildOneQuery(List<String> subscriptions, String startDateTime, String endDateTime) {
        String subscriptionInQuery = buildSubscriptionQuery(subscriptions);
        StringBuilder query = new StringBuilder();
        query.append("HttpIncomingRequests\n");
        query.append("| where subscriptionId in (");
        query.append(subscriptionInQuery);
        query.append("\n)\n");
        query.append("| where userAgent like \"Azure-SDK-For-Java\"\n");
        query.append(
                "| where TIMESTAMP >= datetime(" + startDateTime + ")" + " and TIMESTAMP < datetime(" + endDateTime +
                        ")" + "\n");
        query.append("| summarize requestCount=count() by operationName, targetResourceProvider, userAgent, subscriptionId\n");
        return query.toString();
    }

    private static String buildSubscriptionQuery(List<String> subscriptions) {
        StringBuilder subscriptionInQuery = new StringBuilder();
        for (String subId : subscriptions) {
            subscriptionInQuery.append("\"");
            subscriptionInQuery.append(subId);
            subscriptionInQuery.append("\"");
            subscriptionInQuery.append(",");
        }
        if (subscriptionInQuery.length() > 0) {
            subscriptionInQuery.deleteCharAt(subscriptionInQuery.length() - 1);
        }
        return subscriptionInQuery.toString();
    }

    private static List<String> getTimeSlices(String lastDayOfMonth) throws ParseException {
        String[] s = lastDayOfMonth.split("-");
        List<String> res = new ArrayList<>();
        res.add(s[0] + "-" + s[1] + "-01");
        res.add(s[0] + "-" + s[1] + "-" + dayOfMonthDelimiter[0]);
        res.add(s[0] + "-" + s[1] + "-" + dayOfMonthDelimiter[1]);
        res.add(lastDayOfMonth);
        return res;
    }

    public static List<File> executeKustoQueries(List<String> queries) throws IOException {
        System.out.println("Total query count: " + queries.size());
        KustoExecutor kustoExecutor = new KustoExecutor();
        List<File> downloadedKustoResultFiles = new ArrayList<>();
        List<File> kustoResultFiles = new ArrayList<>();

        for(int i = 0; i < queries.size(); i = i + 1) {
            List<String> executedQueriesInOnetime = queries.subList(i, Math.min(queries.size(), i + 1));
            List<File> downloadedKustoFiles = kustoExecutor.executeKustoQueriesInBrowser(executedQueriesInOnetime, kustoDownloadPath);
            for (int j = 0; j < downloadedKustoFiles.size(); ++j) {
                File source = downloadedKustoFiles.get(j);
                File folder = new File(generatedSummaryPath + "kusto\\" + timeStamp + "\\");
                if(!folder.exists()) {
                    folder.mkdir();
                }
                File destination = new File(folder.getPath(), timeStamp + " - " + i + ".csv");
                FileUtils.moveFile(source, destination);
                kustoResultFiles.add(destination);
            }
        }

//        List<File> kustoResultFiles = new ArrayList<>();
//        for (int i = 0; i < queries.size(); ++i) {
//            kustoResultFiles.add(executeKustoQuery(queries.get(i), i));
//        }
        return kustoResultFiles;
    }

    private static File executeKustoQuery(String query, int i) throws IOException {
        KustoExecutor kustoExecutor = new KustoExecutor();
        File downloadedKustoResultFile = kustoExecutor.executeKustoQueryInBrowser(query, kustoDownloadPath);

        // move file to the correct place
        File destination = new File(
                generatedSummaryPath + "kusto\\" + timeStamp + " - " + i + ".csv");
        FileUtils.moveFile(downloadedKustoResultFile, destination);
        return destination;
    }

    private static void parseKustoResultFiles(List<File> kustoResultFiles) throws IOException {
        for (File file : kustoResultFiles) {
            parseKustoResultFile(file);
            System.out.println("Done with kusto file: " + file.getName());
        }
    }

    private static void parseKustoResultFiles(String directoryPath) throws IOException {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles(File::isFile);
        for (File file : files) {
            parseKustoResultFile(file);
            System.out.println("Done with kusto file: " + file.getName());
        }
    }

    private static void parseKustoResultFile(File kustoResultFile) throws IOException {
        Reader csvFile = new FileReader(kustoResultFile);
        CSVParser csvParser = CSVFormat.DEFAULT.withHeader(KustoHeadersEnum.class).parse(csvFile);

        int c = 0;
        try {
            for (CSVRecord record : csvParser) {
                if (record.size() < csvParser.getHeaderMap().size()) {
                    continue;
                }
                String operationName = record.get(KustoHeadersEnum.operationName);
                String targetResourceProvider = record.get(KustoHeadersEnum.targetResourceProvider);
                String userAgent = record.get(KustoHeadersEnum.userAgent);
                String subscriptionId = record.get(KustoHeadersEnum.subscriptionId);
                String requestCountString = record.get(KustoHeadersEnum.requestCount);
                int requestCount = 0;
                try {
                    requestCount = Integer.parseInt(requestCountString);
                } catch (Exception e) {
                    System.err.println(e);
                }

                if (subscriptionId.isEmpty() || userAgent.isEmpty()) {
                    continue;
                }
                // get the userAgent version part: "Azure-SDK-For-Java/1.41.2-newrelic1 OS:Linux/5.4.92-flatcar MacAddressHash:5e7e345acc018d713fe2fa94494dd2293a2400e0f39decb56b3598fead46a9f9 Java:11.0.11 (MonitorManagementClient)"
//                String userAgentVersion = "";
//                if (!userAgent.isEmpty() && userAgent.split("/").length > 1) {
//                    userAgentVersion = userAgent.split("/")[1];
//                    if(userAgentVersion.split(" ").length > 0) {
//                        userAgentVersion = userAgentVersion.split(" ")[0];
//                    }
//                } else {
//                    System.out.println(userAgent);
//                }
                // get the userAgent version part: "Azure-SDK-For-Java/1.41.2-newrelic1 OS:Linux/5.4.92-flatcar MacAddressHash:5e7e345acc018d713fe2fa94494dd2293a2400e0f39decb56b3598fead46a9f9 Java:11.0.11 (MonitorManagementClient)"
                // Turbonomic/8.3.3 Azure-SDK-For-Java/1.41.0 OS:Linux/3.10.0-1160.21.1.el7.x86_64 MacAddressHash:9a5e8ee346b62b460c0d8396c91f98ce70756aef24be4d528af4505af02283b1 Java:11.0.8 (ResourceManagementClient, 2020-06-01)
                String userAgentVersion = "";
                if(!userAgent.isEmpty() && userAgent.contains("Azure-SDK-For-Java/")) {
                    if(userAgent.split("Azure-SDK-For-Java/").length > 1) {
                        if(userAgent.split("Azure-SDK-For-Java/")[1].split("/").length > 0) {
                            userAgentVersion = userAgent.split("Azure-SDK-For-Java/")[1].split("/")[0];
                            if(userAgentVersion.split(" ").length > 0) {
                                userAgentVersion = userAgentVersion.split(" ")[0];
                            }
                        }
                    }
                }

                int rpCount = RPMap.getOrDefault(targetResourceProvider, 0);
                RPMap.put(targetResourceProvider, rpCount + 1);
                int userAgentCount = userAgentMap.getOrDefault(userAgent, 0);
                userAgentMap.put(userAgent, userAgentCount + 1);

                Set<String> rps = subscriptionIdToRPMap.getOrDefault(subscriptionId, new HashSet<>());
                rps.add(targetResourceProvider);
                subscriptionIdToRPMap.put(subscriptionId, rps);

                Set<String> subscriptions = userAgentVersionToSubscriptionListMap.getOrDefault(userAgentVersion, new HashSet<>());
                subscriptions.add(subscriptionId);
                userAgentVersionToSubscriptionListMap.put(userAgentVersion, subscriptions);

                Set<String> userAgentVersions = subIdToUserAgentVersionMap.getOrDefault(subscriptionId, new HashSet<>());
                userAgentVersions.add(userAgentVersion);
                subIdToUserAgentVersionMap.put(subscriptionId, userAgentVersions);

                String subIdAndVersion = subscriptionId + userAgentVersion;
                int count = subIdAndVersionCallCountMap.getOrDefault(subIdAndVersion, 0);
                subIdAndVersionCallCountMap.put(subIdAndVersion, count + requestCount);

                c++;
            }
        } catch (Exception e) {
            System.err.println(e);
        }

//        System.out.println(c);
    }

    private static void outputResult() {
        outputUserAgentVersionSummary();
//        outputVersionToCCID();
//        outputCustomerNameToCCIDCountInVersion("1.27.2");
//        outputCustomerNameToCCIDCountInVersion("1.34.0");
    }

    private static void outputUserAgentVersionSummary() {
        List<List<String>> output = new ArrayList<>();
        for (String subId : subscriptionsSet) {
            if (subIdToCCIDMap.containsKey(subId)) {
                String ccid = subIdToCCIDMap.get(subId);
                String customer = CCIDToCustomerMap.containsKey(ccid) ? CCIDToCustomerMap.get(ccid) : "Not Found";
                Set<String> userAgentVersions = subIdToUserAgentVersionMap.containsKey(subId) ? subIdToUserAgentVersionMap.get(subId) : new HashSet<>();
                for (String userAgentVersion : userAgentVersions) {
                    int count = subIdAndVersionCallCountMap.containsKey(
                            subId + userAgentVersion) ? subIdAndVersionCallCountMap.get(
                            subId + userAgentVersion) : 0;

                    List<String> item = new ArrayList<>();
                    item.add(subId);
                    item.add(ccid);
                    item.add(customer);
                    item.add(userAgentVersion);
                    item.add(count + "");
                    item.add(subscriptionIdToRPMap.get(subId).toString());

                    output.add(item);

                }


            }
        }

//        String fileName = "userAgentVersionSummary-" + (new Date()) + ".csv";

        String fileName = timeStamp + "-userAgentVersion.csv";
        String outputFilePath =
                generatedSummaryPath + "summary\\" + fileName;
        try (CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader("SubscriptionGuid", "CloudCustomerGuid", "Customer", "userAgentVersion", "RequestCount", "ResourceProvider")
                .print(new File(outputFilePath), Charset.forName("GBK"))) {
            for (List<String> items : output) {
                csvPrinter.printRecord(items);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void outputVersionToCCID() {
        List<List<String>> output = new ArrayList<>();
        Map<String, Set<String>> versionToCCIDsMap = new HashMap<>();
        if(userAgentVersionToSubscriptionListMap != null) {
            userAgentVersionToSubscriptionListMap.forEach((version, subs) -> {
                subs.forEach(sub -> {
                    if(subIdToCCIDMap != null && subIdToCCIDMap.containsKey(sub)) {
                        String ccid = subIdToCCIDMap.get(sub);
                        Set<String> set = versionToCCIDsMap.getOrDefault(version, new HashSet<>());
                        set.add(ccid);
                        versionToCCIDsMap.put(version, set);
                    }
                });
            });
        }
        versionToCCIDsMap.forEach((version, ccids) -> {
            List<String> line = new ArrayList<>();
            line.add(version);
            line.add(ccids.size() + "");
            output.add(line);
        });
        String fileName = timeStamp + "-versionToCCIDDistribution.csv";
        String outputFilePath =
                generatedSummaryPath + "summary\\" + fileName;
        try (CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader("Version", "CloudCustomerGuid Count")
                .print(new File(outputFilePath), Charset.forName("GBK"))) {
            for (List<String> items : output) {
                csvPrinter.printRecord(items);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void outputCustomerNameToCCIDCountInVersion(String version) {
        List<List<String>> output = new ArrayList<>();
        Map<String, Set<String>> customerNameToCCIDsMap = new HashMap<>();
        if(userAgentVersionToSubscriptionListMap != null) {
            if(userAgentVersionToSubscriptionListMap.containsKey(version)) {
                Set<String> subs = userAgentVersionToSubscriptionListMap.get(version);
                subs.forEach(sub -> {
                    if(subIdToCCIDMap.containsKey(sub)) {
                        String ccid = subIdToCCIDMap.get(sub);
                        if(CCIDToCustomerMap.containsKey(ccid)) {
                            String customerName = CCIDToCustomerMap.get(ccid);
                            Set<String> ccids = customerNameToCCIDsMap.getOrDefault(customerName, new HashSet<>());
                            ccids.add(ccid);
                            customerNameToCCIDsMap.put(customerName, ccids);
                        }
                    }
                });
            }
        }
        customerNameToCCIDsMap.forEach((customerName, ccids) -> {
            List<String> line = new ArrayList<>();
            line.add(customerName);
            line.add(ccids.size() + "");
            output.add(line);
        });
        String fileName = timeStamp + "-" + version + "-customerToCCIDCountDistribution.csv";
        String outputFilePath =
                generatedSummaryPath + "summary\\" + fileName;
        try (CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader("Customer Name", "CloudCustomerGuid Count")
                .print(new File(outputFilePath), Charset.forName("GBK"))) {
            for (List<String> items : output) {
                csvPrinter.printRecord(items);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void analyze() throws IOException {


//        System.out.println("input subscription list, each line is a subscription id:");
        Scanner scanner = new Scanner(IN);

        OUT.println("Please input file path of CCID and subscription relation file path");
        String CCIDSubFilePath = scanner.nextLine();
        CCIDSubFilePath = parseFileName(CCIDSubFilePath);


//        Reader CCIDCsvFile = new FileReader(CCIDSubFilePath);

        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(CCIDSubFilePath)), "GBK");
        CSVParser csvParserForCCIDFile = CSVFormat.DEFAULT.withHeader(CCIDHeadersEnum.class).parse(isr);


//        Map<String, String> subIdToCCIDMap = new HashMap<>();
//        Map<String, Set<String>> ccidToSubIdsMap = new HashMap<>();
//        Map<String, String> CCIDToCustomerMap = new HashMap<>();
//        Set<String> subscriptionsSet = new HashSet<>();

        int recordCount = 0;
        int ccidCount = 0;
        for (CSVRecord record : csvParserForCCIDFile) {
            recordCount++;
            String ccid = record.get(CCIDHeadersEnum.CCID);
            String subscriptionId = record.get(CCIDHeadersEnum.SubscriptionId);
            String customer = record.get(CCIDHeadersEnum.CustomerName);
            subIdToCCIDMap.put(subscriptionId, ccid);
            CCIDToCustomerMap.put(ccid, customer);
            Set<String> subs = ccidToSubIdsMap.getOrDefault(ccid, new HashSet<>());
            subs.add(subscriptionId);
            ccidToSubIdsMap.put(ccid, subs);
            subscriptionsSet.add(subscriptionId);
            ccidCount = ccidToSubIdsMap.size();
//            if (ccidCount == 100) {
//                break;
//            }
        }

        OUT.println("Total CCID count: " + ccidToSubIdsMap.size());
        OUT.println("Total subscription count: " + subscriptionsSet.size());
        // =======================build query==================
        StringBuilder subscriptionInQuery = new StringBuilder();
        for (String subId : subscriptionsSet) {
            subscriptionInQuery.append("\"");
            subscriptionInQuery.append(subId);
            subscriptionInQuery.append("\"");
            subscriptionInQuery.append(",");
        }
        if (subscriptionInQuery.length() > 0) {
            subscriptionInQuery.deleteCharAt(subscriptionInQuery.length() - 1);
        }
//        String subscriptionID = scanner.nextLine();
//
//        int subscriptionCount = 0;
//        StringBuilder subscriptionInQuery = new StringBuilder();
//        while(!subscriptionID.isEmpty()) {
//            subscriptionCount++;
//            subscriptionInQuery.append("\"");
//            subscriptionInQuery.append(subscriptionID);
//            subscriptionInQuery.append("\"");
//            subscriptionInQuery.append(",");
//            subscriptionID = scanner.nextLine();
//        }
//        if(subscriptionInQuery.length()  > 0) {
//            subscriptionInQuery.deleteCharAt(subscriptionInQuery.length() - 1);
//        }
//        OUT.println("Total subscription count: " + subscriptionCount);

        StringBuilder query = new StringBuilder();
        query.append("HttpIncomingRequests\n");
        query.append("| where subscriptionId in (");
        query.append(subscriptionInQuery);
        query.append("\n)\n");
        query.append("| where userAgent like \"Azure-SDK-For-Java\"\n");
        query.append("| where TIMESTAMP >= datetime(2021-09-01) and TIMESTAMP <= datetime(2021-09-30)\n");
        query.append("| summarize requestCount=count() by operationName, targetResourceProvider, userAgent, subscriptionId\n");

        FileWriter fileWriter = new FileWriter("./AzureApp/query/query-" + timeStamp);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("Total CCID count: " + ccidToSubIdsMap.size());
        printWriter.println("Total subscription count: " + subscriptionsSet.size());
        printWriter.println(query);
        printWriter.close();

        OUT.println("please paste query-20211021 in Kusto Data Explorer, execute, and export the result to a csv file:");
        OUT.println("=============================================================================");
//        OUT.println(query-20211021);


//        Scanner scannerFile = new Scanner(new File(path));
//        String header = scannerFile.nextLine(); // escape header
        OUT.println("Please input file path of Kusto result file:");
        String kustoPath = scanner.nextLine();
        kustoPath = parseFileName(kustoPath);


        Reader csvFile = new FileReader(kustoPath);
        CSVParser csvParser = CSVFormat.DEFAULT.parse(csvFile);

        int c = 0;
        for (CSVRecord record : csvParser) {

            String operationName = record.get(0);
            String targetResourceProvider = record.get(1);
            String userAgent = record.get(2);
            String subscriptionId = record.get(3);
            String requestCountString = record.get(4);
            int requestCount = 0;
            try {
                requestCount = Integer.parseInt(requestCountString);
            } catch (Exception e) {
                System.err.println(e);
            }
            // get the userAgent version part: "Azure-SDK-For-Java/1.41.2-newrelic1 OS:Linux/5.4.92-flatcar MacAddressHash:5e7e345acc018d713fe2fa94494dd2293a2400e0f39decb56b3598fead46a9f9 Java:11.0.11 (MonitorManagementClient)"
            // Turbonomic/8.3.3 Azure-SDK-For-Java/1.41.0 OS:Linux/3.10.0-1160.21.1.el7.x86_64 MacAddressHash:9a5e8ee346b62b460c0d8396c91f98ce70756aef24be4d528af4505af02283b1 Java:11.0.8 (ResourceManagementClient, 2020-06-01)
            String userAgentVersion = "";
            if(userAgent.contains("Azure-SDK-For-Java/")) {
                if(userAgent.split("Azure-SDK-For-Java/").length > 1) {
                    if(userAgent.split("Azure-SDK-For-Java/")[1].split("/").length > 0) {
                        userAgentVersion = userAgent.split("Azure-SDK-For-Java/")[1].split("/")[0];
                        if(userAgentVersion.split(" ").length > 0) {
                            userAgentVersion = userAgentVersion.split(" ")[0];
                        }
                    }
                }
            }
//            if (userAgent.split("/").length > 1) {
//                userAgentVersion = userAgent.split("/")[1];
//            } else {
//                System.out.println(userAgent);
//            }

            int rpCount = RPMap.getOrDefault(targetResourceProvider, 0);
            RPMap.put(targetResourceProvider, rpCount + 1);
            int userAgentCount = userAgentMap.getOrDefault(userAgent, 0);
            userAgentMap.put(userAgent, userAgentCount + 1);

            Set<String> rps = subscriptionIdToRPMap.getOrDefault(subscriptionId, new HashSet<>());
            rps.add(targetResourceProvider);
            subscriptionIdToRPMap.put(subscriptionId, rps);

            Set<String> subscriptions = userAgentVersionToSubscriptionListMap.getOrDefault(userAgentVersion, new HashSet<>());
            subscriptions.add(subscriptionId);
            userAgentVersionToSubscriptionListMap.put(userAgentVersion, subscriptions);

            Set<String> userAgentVersions = subIdToUserAgentVersionMap.getOrDefault(subscriptionId, new HashSet<>());
            userAgentVersions.add(userAgentVersion);
            subIdToUserAgentVersionMap.put(subscriptionId, userAgentVersions);

            String subIdAndVersion = subscriptionId + userAgentVersion;
            int count = subIdAndVersionCallCountMap.getOrDefault(subIdAndVersion, 0);
            subIdAndVersionCallCountMap.put(subIdAndVersion, count + requestCount);

            c++;
        }
        System.out.println(c);

        List<List<String>> output = new ArrayList<>();
        for (String subId : subscriptionsSet) {
            if (subIdToCCIDMap.containsKey(subId)) {

                String ccid = subIdToCCIDMap.containsKey(subId) ? subIdToCCIDMap.get(subId) : "Not Found";
                String customer = CCIDToCustomerMap.containsKey(ccid) ? CCIDToCustomerMap.get(ccid) : "Not Found";
                Set<String> userAgentVersions = subIdToUserAgentVersionMap.containsKey(subId) ? subIdToUserAgentVersionMap.get(subId) : new HashSet<>();
                for (String userAgentVersion : userAgentVersions) {
                    int count = subIdAndVersionCallCountMap.containsKey(
                            subId + userAgentVersion) ? subIdAndVersionCallCountMap.get(
                            subId + userAgentVersion) : 0;

                    List<String> item = new ArrayList<>();
                    item.add(subId);
                    item.add(ccid);
                    item.add(customer);
                    item.add(userAgentVersion);
                    item.add(count + "");
                    item.add(subscriptionIdToRPMap.get(subId).toString());

                    output.add(item);

                }


            }
        }

//        String fileName = "userAgentVersionSummary-" + (new Date()) + ".csv";

        String fileName = timeStamp + "-userAgentVersionSummary.csv";
        String outputFilePath =
                "C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\" + fileName;
        try (CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader("SubscriptionGuid", "CloudCustomerGuid", "Customer", "userAgentVersion", "RequestCount", "ResourceProvider")
                .print(new File(outputFilePath), Charset.forName("GBK"))) {
            for (List<String> items : output) {
                csvPrinter.printRecord(items);
            }
        }


//        while(scannerFile.hasNext()) {
//            String result = scannerFile.nextLine();
//            String[] items = result.split(",");
//            String operationName = items[0];
//            String targetResourceProvider = items[1];
//            String userAgent = items[2];
//            String subscriptionId = items[3];
//            String requestCount = items[4];
//            // use regex to get the userAgent version part: "Azure-SDK-For-Java/1.41.2-newrelic1 OS:Linux/5.4.92-flatcar MacAddressHash:5e7e345acc018d713fe2fa94494dd2293a2400e0f39decb56b3598fead46a9f9 Java:11.0.11 (MonitorManagementClient)"
//            String userAgentVersion = "";
//            if(userAgent.split("/").length > 1) {
//                userAgentVersion = userAgent.split("/")[1].split(" ")[0];
//            } else {
//                System.out.println(userAgent);
//            }
//
//            int rpCount = RPMap.getOrDefault(targetResourceProvider, 0);
//            RPMap.put(targetResourceProvider, rpCount + 1);
//            int userAgentCount = userAgentMap.getOrDefault(userAgent, 0);
//            userAgentMap.put(userAgent, userAgentCount + 1);
//
//            Set<String> subscriptions = userAgentVersionToSubscriptionListMap.getOrDefault(userAgentVersion, new HashSet<>());
//            subscriptions.add(subscriptionId);
//            userAgentVersionToSubscriptionListMap.put(userAgentVersion, subscriptions);
//
//
//        }
//        System.out.println(RPMap);
//        System.out.println(userAgentMap);
//        System.out.println(userAgentVersionToSubscriptionListMap);

        System.out.println("finish");
    }

    private static void executeKustoQuery(String dbName, String query) {
        try {
            ConnectionStringBuilder csb = ConnectionStringBuilder.createWithAadApplicationCredentials(
                    "https://armprod.kusto.windows.net/",
                    "cbd21561-2da6-499b-b745-786faaff18b2",
                    "SsDoCsrxSdn2Gfx0~IWoNuXhe3lIA8Z.N5",
                    "72f988bf-86f1-41af-91ab-2d7cd011db47");

            query = "HttpIncomingRequests\n" +
                    "| where subscriptionId in (\"ac499d90-cdae-4451-b8bf-50a3cff80a32\"\n" +
                    ")\n" +
                    "| where TIMESTAMP > ago(1d)";
            ClientImpl client = new ClientImpl(csb);

            KustoOperationResult results = client.execute(dbName, query);
            KustoResultSetTable mainTableResult = results.getPrimaryResults();
            System.out.println(String.format("Kusto sent back %s rows.", mainTableResult.count()));

            // iterate values
            while (mainTableResult.next()) {
                List<Object> nextValue = mainTableResult.getCurrentRow();
            }

            // in case we want to pass client request properties
            ClientRequestProperties clientRequestProperties = new ClientRequestProperties();
            clientRequestProperties.setTimeoutInMilliSec(TimeUnit.MINUTES.toMillis(1));

            results = client.execute(System.getProperty("dbName"), System.getProperty("query-20211021"), clientRequestProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String parseFileName(String inputFileName) {
        String res = "";
        if (inputFileName.startsWith("\"") && inputFileName.endsWith("\"")) {
            res = inputFileName.substring(1, inputFileName.length() - 1);
        }
        return res;
    }


}
