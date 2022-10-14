// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.fabrikam;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NewCustomerDataAnalyzerTest {

    @Test
    public void test() throws IOException {
        NewCustomerDataAnalyzer.executeKustoQueries(Arrays.asList("HttpIncomingRequests\n" +
                "| where subscriptionId in (\n" +
                "'ec0aa5f7-9e78-40c9-85cd-535c6305b380'\n" +
                ")\n" +
                "| where TIMESTAMP > ago(1d)\n" +
                "| where userAgent like \"azsdk-java\"",
                "HttpIncomingRequests\n" +
                        "| where subscriptionId in (\n" +
                        "'ec0aa5f7-9e78-40c9-85cd-535c6305b380'\n" +
                        ")\n" +
                        "| where TIMESTAMP > ago(1d)\n" +
                        "| where userAgent like \"azsdk-java\"",
                "HttpIncomingRequests\n" +
                        "| where subscriptionId in (\n" +
                        "'ec0aa5f7-9e78-40c9-85cd-535c6305b380'\n" +
                        ")\n" +
                        "| where TIMESTAMP > ago(1d)\n" +
                        "| where userAgent like \"azsdk-java\"",
                "HttpIncomingRequests\n" +
                        "| where subscriptionId in (\n" +
                        "'ec0aa5f7-9e78-40c9-85cd-535c6305b380'\n" +
                        ")\n" +
                        "| where TIMESTAMP > ago(1d)\n" +
                        "| where userAgent like \"azsdk-java\""
                ));
    }
}