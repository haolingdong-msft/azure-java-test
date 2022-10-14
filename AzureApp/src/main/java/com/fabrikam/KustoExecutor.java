// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.fabrikam;

import com.microsoft.azure.kusto.data.ClientImpl;
import com.microsoft.azure.kusto.data.ClientRequestProperties;
import com.microsoft.azure.kusto.data.KustoOperationResult;
import com.microsoft.azure.kusto.data.KustoResultSetTable;
import com.microsoft.azure.kusto.data.auth.ConnectionStringBuilder;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KustoExecutor {
    public void executeKustoQuery(String dbName, String query) {
        try {
            ConnectionStringBuilder csb = ConnectionStringBuilder.createWithAadApplicationCredentials(
                    System.getProperty("clusterPath"),
                    System.getProperty("appId"),
                    System.getProperty("appKey"),
                    System.getProperty("appTenant"));
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

    public List<File> executeKustoQueriesInBrowser(List<String> queries, String outputFileDirPath) throws IOException {
        System.setProperty("webdriver.edge.driver", "C:\\workspace\\edgedriver_win64\\102\\msedgedriver.exe");
        System.setProperty("webdriver.chrome.driver", "C:\\workspace\\chromedriver_win32\\chromedriver.exe");

        WebDriver driver = new EdgeDriver();
        List<File> files = new ArrayList<>();
        try {
            driver.get("https://dataexplorer.azure.com/clusters/armprod/databases/ARMProd");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            for (int j = 0; j < queries.size(); ++j) {
                String query = queries.get(j);
                if (j > 0) {
                    // click add button
                    WebElement addButton = new WebDriverWait(driver, Duration.ofSeconds(60))
                            .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@aria-label='Add new tab']")));
                    addButton.click();
                }
                // execute query
                WebElement textareaElement = new WebDriverWait(driver, Duration.ofSeconds(60))
                        .until(ExpectedConditions.elementToBeClickable(By.xpath("//textarea")));
                for (int i = 0; i < query.length(); i = i + 1000) {
                    textareaElement.sendKeys(query.substring(i, Math.min(i + 1000, query.length())));
                }
//            for(int i = 0; i < query.length(); ++i) {
//                textareaElement.sendKeys(query.charAt(i) + "");
//            }
//            textareaElement.sendKeys(query);
                Thread.sleep(10000);
                WebElement runElement = driver.findElement(By.xpath("//button[@name='Run']"));
                runElement.click();

                Thread.sleep(10000);

                // make sure the query is in executing status, otherwise click run again
                if(driver.findElement(By.xpath("//button[@name='Cancel']")) == null) {
                    runElement.click();
                }

                // wait until the query is done
                try {
                    new WebDriverWait(driver, Duration.ofMinutes(30)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='status' and (starts-with(@aria-label, 'Query Done') or starts-with(@aria-label, 'Query Error'))]")));

                    // if run is successful, than download
                    if (driver.findElement(By.xpath("//div[@role='status' and starts-with(@aria-label, 'Query Done')]")) !=
                            null) {
                        WebElement fileElement = new WebDriverWait(driver, Duration.ofMinutes(1))
                                .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@name='File']")));
                        fileElement.click();
                        WebElement exportElement = driver.findElement(By.xpath("//button[@name='Export to CSV']"));
                        exportElement.click();

                        Thread.sleep(10000);
                        // get exported file
                        files.add(getLatestFileInPath(outputFileDirPath));
                        System.out.println("Successfully execute query #" + j);
                    } else { // if run is failure, then take screen shot
                        System.out.println("Status error execute query #" + j);
                        // still get exported file

                        try {
                            WebElement fileElement = new WebDriverWait(driver, Duration.ofMinutes(1))
                                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@name='File']")));
                            fileElement.click();
                            WebElement exportElement = driver.findElement(By.xpath("//button[@name='Export to CSV']"));
                            exportElement.click();

                            Thread.sleep(10000);
                            // get exported file
                            files.add(getLatestFileInPath(outputFileDirPath));
                        } catch (Exception e) {
                            System.out.println("Failure exporting file when ecountering error");
                        }

                        takeScreenShot(driver);
                    }
                } catch (Exception e) {
                    System.out.println("Failure execute query #" + j);
                    takeScreenShot(driver);
                    // try to get exported file as well
                    try {
                        WebElement fileElement = new WebDriverWait(driver, Duration.ofMinutes(1))
                                .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@name='File']")));
                        fileElement.click();
                        WebElement exportElement = driver.findElement(By.xpath("//button[@name='Export to CSV']"));
                        exportElement.click();

                        Thread.sleep(10000);
                        // get exported file
                        files.add(getLatestFileInPath(outputFileDirPath));
                    } catch (Exception ex) {
                        System.out.println("Failure exporting file when Failure execute query");
                    }
                }
            }

//            List<WebElement> tabs = driver.findElements(By.xpath("//div[@role='tab']"));
//            for (WebElement tab : tabs) {
//                tab.click();
//                // wait until the query is done
//                new WebDriverWait(driver, Duration.ofMinutes(30)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='status' and (starts-with(@aria-label, 'Query Done') or starts-with(@aria-label, 'Query Error'))]")));
//
//                WebElement fileElement = new WebDriverWait(driver, Duration.ofMinutes(1))
//                        .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@name='File']")));
//                fileElement.click();
//                WebElement exportElement = driver.findElement(By.xpath("//button[@name='Export to CSV']"));
//                exportElement.click();
//
//                // get exported file
//                files.add(getLatestFileInPath(outputFileDirPath));
//            }
        } catch (Exception e) {
            System.out.println("Failure execute query");
            takeScreenShot(driver);
        } finally {
            driver.quit();
        }
        return files;
    }

    private void takeScreenShot(WebDriver driver) throws IOException {
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileUtils.moveFile(scrFile, new File(
                "C:\\Users\\haolingdong\\OneDrive - Microsoft\\Documents\\Track2-adoption\\generated_summary\\screenshot\\" +
                        new SimpleDateFormat("yyyy.MM.dd.hh.mm.ss").format(new Date())));
    }

    public File executeKustoQueryInBrowser(String query, String outputFileDirPath) {
        System.setProperty("webdriver.edge.driver", "C:\\workspace\\edgedriver_win64\\msedgedriver.exe");
        System.setProperty("webdriver.chrome.driver", "C:\\workspace\\chromedriver_win32\\chromedriver.exe");

        WebDriver driver = new EdgeDriver();
        driver.get("https://dataexplorer.azure.com/clusters/armprod/databases/ARMProd");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
//            WebElement e = driver.findElement(By.xpath("//div[@class='view-lines monaco-mouse-cursor-text']"));
            WebElement textareaElement = new WebDriverWait(driver, Duration.ofSeconds(60))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//textarea")));
            for (int i = 0; i < query.length(); i = i + 1000) {
                textareaElement.sendKeys(query.substring(i, Math.min(i + 1000, query.length())));
            }
//            for(int i = 0; i < query.length(); ++i) {
//                textareaElement.sendKeys(query.charAt(i) + "");
//            }
//            textareaElement.sendKeys(query);
            WebElement runElement = driver.findElement(By.xpath("//button[@name='Run']"));
            runElement.click();
            // wait until the query is done
            new WebDriverWait(driver, Duration.ofMinutes(30)).until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='status' and (starts-with(@aria-label, 'Query Done') or starts-with(@aria-label, 'Query Error'))]")));

            WebElement fileElement = new WebDriverWait(driver, Duration.ofMinutes(1))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@name='File']")));
            fileElement.click();
            WebElement exportElement = driver.findElement(By.xpath("//button[@name='Export to CSV']"));
            exportElement.click();

            // get exported file
            return getLatestFileInPath(outputFileDirPath);

//            driver.findElement(By.name("q")).sendKeys("cheese" + Keys.ENTER);
//            WebElement firstResult = wait.until(presenceOfElementLocated(By.cssSelector("h3")));
//            System.out.println(firstResult.getAttribute("textContent"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return null;
    }

    public static void main(String[] args) {
    }

    private File getLatestFileInPath(String directoryFilePath) {
        File directory = new File(directoryFilePath);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("export") && file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        return chosenFile;

    }
}
