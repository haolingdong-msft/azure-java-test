package com.fabrikam;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.AzureAuthorityHosts;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.EnvironmentCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.OperatingSystem;
import com.azure.resourcemanager.appservice.models.PricingTier;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.compute.models.AvailabilitySetSkuTypes;
import com.azure.resourcemanager.compute.models.CachingTypes;
import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.DiskInstanceView;
import com.azure.resourcemanager.compute.models.DiskSkuTypes;
import com.azure.resourcemanager.compute.models.InstanceViewStatus;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.OperatingSystemTypes;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineSizeTypes;
import com.azure.resourcemanager.costmanagement.CostManagementManager;
import com.azure.resourcemanager.costmanagement.models.ExportType;
import com.azure.resourcemanager.costmanagement.models.FunctionType;
import com.azure.resourcemanager.costmanagement.models.QueryAggregation;
import com.azure.resourcemanager.costmanagement.models.QueryColumnType;
import com.azure.resourcemanager.costmanagement.models.QueryDataset;
import com.azure.resourcemanager.costmanagement.models.QueryDefinition;
import com.azure.resourcemanager.costmanagement.models.QueryGrouping;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import com.azure.resourcemanager.costmanagement.models.TimeframeType;
import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.PublicIPSkuType;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.sql.models.SqlDatabase;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.azure.resourcemanager.storage.models.PublicEndpoints;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.common.StorageSharedKeyCredential;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AzureApp {
    public static void main(String[] args) {
        computeDoc();
    }

    public static void listVM() {
        TokenCredential credential = new EnvironmentCredentialBuilder()
                .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                .build();

        // If you don't set the tenant ID and subscription ID via environment variables,
        // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                .withLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
                .authenticate(credential, profile)
                .withDefaultSubscription();

        System.out.println(azureResourceManager.resourceGroups().list().stream().count());
    }

    private static void computeDoc() {

        TokenCredential credential = new EnvironmentCredentialBuilder()
                .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                .build();

        // If you don't set the tenant ID and subscription ID via environment variables,
        // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        AzureResourceManager azure = AzureResourceManager.configure()
                .withLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
                .authenticate(credential, profile)
                .withDefaultSubscription();

        System.out.println("Creating resource group...");
        ResourceGroup resourceGroup = azure.resourceGroups()
                .define("myResourceGroup")
                .withRegion(Region.US_EAST)
                .create();

        System.out.println("Creating availability set...");
        AvailabilitySet availabilitySet = azure.availabilitySets()
                .define("myAvailabilitySet")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .create();

        System.out.println("Creating public IP address...");
        PublicIpAddress publicIPAddress = azure.publicIpAddresses()
                .define("myPublicIP")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .withDynamicIP()
                .create();

        System.out.println("Creating virtual network...");
        Network network = azure.networks()
                .define("myVN")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .withAddressSpace("10.0.0.0/16")
                .withSubnet("mySubnet", "10.0.0.0/24")
                .create();

        System.out.println("Creating network interface...");
        NetworkInterface networkInterface = azure.networkInterfaces()
                .define("myNIC")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .withExistingPrimaryNetwork(network)
                .withSubnet("mySubnet")
                .withPrimaryPrivateIPAddressDynamic()
                .withExistingPrimaryPublicIPAddress(publicIPAddress)
                .create();

        System.out.println("Creating virtual machine...");
        VirtualMachine virtualMachine = azure.virtualMachines()
                .define("myVM")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .withExistingPrimaryNetworkInterface(networkInterface)
                .withLatestWindowsImage("MicrosoftWindowsServer", "WindowsServer", "2012-R2-Datacenter")
                .withAdminUsername("azureuser")
                .withAdminPassword("Azure12345678")
                .withComputerName("myVM")
                .withExistingAvailabilitySet(availabilitySet)
                .withSize("Standard_DS1")
                .create();
        Scanner input = new Scanner(System.in);
        System.out.println("Press enter to get information about the VM...");
        input.nextLine();

        Disk managedDisk = azure.disks().define("myosdisk")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .withWindowsFromVhd("https://mystorage.blob.core.windows.net/vhds/myosdisk.vhd")
                .withStorageAccountName("mystorage")
                .withSizeInGB(128)
                .withSku(DiskSkuTypes.PREMIUM_LRS)
                .create();

        azure.virtualMachines().define("myVM2")
                .withRegion(Region.US_EAST)
                .withExistingResourceGroup("myResourceGroup")
                .withExistingPrimaryNetworkInterface(networkInterface)
                .withSpecializedOSDisk(managedDisk, OperatingSystemTypes.WINDOWS)
                .withExistingAvailabilitySet(availabilitySet)
                .withSize(VirtualMachineSizeTypes.STANDARD_DS1)
                .create();

        VirtualMachine vm = azure.virtualMachines().getByResourceGroup("myResourceGroup", "myVM");

        System.out.println("hardwareProfile");
        System.out.println("    vmSize: " + vm.size());
        System.out.println("storageProfile");
        System.out.println("  imageReference");
        System.out.println("    publisher: " + vm.storageProfile().imageReference().publisher());
        System.out.println("    offer: " + vm.storageProfile().imageReference().offer());
        System.out.println("    sku: " + vm.storageProfile().imageReference().sku());
        System.out.println("    version: " + vm.storageProfile().imageReference().version());
        System.out.println("  osDisk");
        System.out.println("    osType: " + vm.storageProfile().osDisk().osType());
        System.out.println("    name: " + vm.storageProfile().osDisk().name());
        System.out.println("    createOption: " + vm.storageProfile().osDisk().createOption());
        System.out.println("    caching: " + vm.storageProfile().osDisk().caching());
        System.out.println("osProfile");
        System.out.println("    computerName: " + vm.osProfile().computerName());
        System.out.println("    adminUserName: " + vm.osProfile().adminUsername());
        System.out.println("    provisionVMAgent: " + vm.osProfile().windowsConfiguration().provisionVMAgent());
        System.out.println(
                "    enableAutomaticUpdates: " + vm.osProfile().windowsConfiguration().enableAutomaticUpdates());
        System.out.println("networkProfile");
        System.out.println("    networkInterface: " + vm.primaryNetworkInterfaceId());
        System.out.println("vmAgent");
        System.out.println("  vmAgentVersion: " + vm.instanceView().vmAgent().vmAgentVersion());
        System.out.println("    statuses");
        for (InstanceViewStatus status : vm.instanceView().vmAgent().statuses()) {
            System.out.println("    code: " + status.code());
            System.out.println("    displayStatus: " + status.displayStatus());
            System.out.println("    message: " + status.message());
            System.out.println("    time: " + status.time());
        }
        System.out.println("disks");
        for (DiskInstanceView disk : vm.instanceView().disks()) {
            System.out.println("  name: " + disk.name());
            System.out.println("  statuses");
            for (InstanceViewStatus status : disk.statuses()) {
                System.out.println("    code: " + status.code());
                System.out.println("    displayStatus: " + status.displayStatus());
                System.out.println("    time: " + status.time());
            }
        }
        System.out.println("VM general status");
        System.out.println("  provisioningStatus: " + vm.provisioningState());
        System.out.println("  id: " + vm.id());
        System.out.println("  name: " + vm.name());
        System.out.println("  type: " + vm.type());
        System.out.println("VM instance status");
        for (InstanceViewStatus status : vm.instanceView().statuses()) {
            System.out.println("  code: " + status.code());
            System.out.println("  displayStatus: " + status.displayStatus());
        }
        System.out.println("Press enter to continue...");
        input.nextLine();


        System.out.println("Stopping vm...");
        vm.powerOff();
        System.out.println("Press enter to continue...");
        input.nextLine();

        vm.deallocate();

        System.out.println("Starting vm...");
        vm.start();
        System.out.println("Press enter to continue...");
        input.nextLine();


        System.out.println("Resizing vm...");
        vm.update()
                .withSize(VirtualMachineSizeTypes.STANDARD_DS2)
                .apply();
        System.out.println("Press enter to continue...");
        input.nextLine();


        System.out.println("Adding data disk...");
        vm.update()
                .withNewDataDisk(2, 0, CachingTypes.READ_WRITE)
                .apply();
        System.out.println("Press enter to delete resources...");
        input.nextLine();


        System.out.println("Deleting resources...");
        azure.resourceGroups().deleteByName("myResourceGroup");

        azure.resourceGroups().beginDeleteByName("myResourceGroup");
    }

    private static void webappDoc() {
        TokenCredential credential = new EnvironmentCredentialBuilder()
                .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                .build();

        // If you don't set the tenant ID and subscription ID via environment variables,
        // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        AzureResourceManager azure = AzureResourceManager.configure()
                .withLogLevel(HttpLogDetailLevel.BASIC)
                .authenticate(credential, profile)
                .withDefaultSubscription();

        AppServiceManager appServiceManager = AppServiceManager
                .authenticate(credential, profile);


        AppServicePlan myLinuxAppServicePlan =
                appServiceManager
                        .appServicePlans()
                        .define("myServicePlan")
                        .withRegion(Region.US_WEST)
                        .withNewResourceGroup("myResourceGroup")
                        .withPricingTier(PricingTier.PREMIUM_P1)
                        .withOperatingSystem(OperatingSystem.WINDOWS)
                        .withPerSiteScaling(false)
                        .withCapacity(2)
                        .create();

        WebApp app = azure.webApps().define("newLinuxWebApp")
                .withExistingLinuxPlan(myLinuxAppServicePlan)
                .withExistingResourceGroup("myResourceGroup")
                .withPrivateDockerHubImage("username/my-java-app")
                .withCredentials("dockerHubUser", "dockerHubPassword")
                .withAppSetting("PORT", "8080")
                .create();

    }

    private static void createLinuxVM() { // pass
        final String userName = "haolingdong";
        final String sshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDtyI4Q33AVWRZHiqElG3znQXJ2lcPBL/IEjo+ULx3xkcdiZCSymkok614kg8OMkUuc/VriydB7IHCPdJ1Zz5lgZWyd2PYVScww89TFrHVwfMBq+Y3bl7TElehu8w8JiMQpf1taQOhyqEeGyKShtjqZNCpkj69RtS1RadpJxcgr4eYBMdK61G0Y5wRWouoBOY0R5WvpN36yI2o+oW3FqQqY77cg97RoUmj6n5HEv6ZdH8C2KbbutRtWfczT1pgrgdA5Uiyu3Ba1BTjrM8lPrFGGKDwlJcdj/wXuZ8Ax3We4sQSwPaf9jNBpf0zOtt8R9TXfoM7mq+93LaTAPo5rZacFY9upXfEiv1Ff+ZNy36GZkTmGE7C2LRsMff8PGOkXkn/OVYM6AzcbDIljOabXp65dFRrSsPQvP5tcxjvr1d1LXSOK8mlGNE0+K9oNTayJ/JsWlcrFXXSyRquMtLnbzO3jq2nk2nBIX9AOL33lecRoF6XWu3HKHyrs4AC0EdalajM= haolingdong@haolindong-712";

        try {
            TokenCredential credential = new EnvironmentCredentialBuilder()
                    .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                    .build();

            // If you don't set the tenant ID and subscription ID via environment variables,
            // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            // Create an Ubuntu virtual machine in a new resource group.
            VirtualMachine linuxVM = azureResourceManager.virtualMachines().define("testLinuxVM")
                    .withRegion(Region.US_EAST)
                    .withNewResourceGroup("sampleVmResourceGroup")
                    .withNewPrimaryNetwork("10.0.0.0/24")
                    .withPrimaryPrivateIPAddressDynamic()
                    .withoutPrimaryPublicIPAddress()
                    .withPopularLinuxImage(KnownLinuxVirtualMachineImage.UBUNTU_SERVER_16_04_LTS)
                    .withRootUsername(userName)
                    .withSsh(sshKey)
                    .withUnmanagedDisks()
                    .withSize(VirtualMachineSizeTypes.STANDARD_D3_V2)
                    .create();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void connectDB() {
        // Create the db using the management libraries.
        try {
            TokenCredential credential = new EnvironmentCredentialBuilder()
                    .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                    .build();

            // If you don't set the tenant ID and subscription ID via environment variables,
            // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
                    .authenticate(credential, profile)
                    .withDefaultSubscription();


            // create SQL Server
            final String adminUser = "haolingdong";
            final String sqlServerName = "haolingsqlserver";
            final String sqlDbName = "HaolingSQLServerDB";
            final String dbPassword = "yepwkerhsWER123$$@";
            final String firewallRuleName = "haolingFireRule";

            SqlServer sampleSQLServer = azureResourceManager.sqlServers().define(sqlServerName)
                    .withRegion(Region.US_EAST)
                    .withNewResourceGroup("sampleSqlResourceGroup")
                    .withAdministratorLogin(adminUser)
                    .withAdministratorPassword(dbPassword)
                    .defineFirewallRule(firewallRuleName)
                    .withIpAddressRange("0.0.0.0","255.255.255.255")
                    .attach()
                    .create();

            // create Database with tags
            Map<String, String> tags = new HashMap<>();
            tags.put("tag-test-key", "tag-test-value");
            sampleSQLServer.databases().define(sqlDbName).withTags(tags).create();

            // get tags
            SqlDatabase sqlDatabase = sampleSQLServer.databases().get(sqlDbName);
            System.out.println(sqlDatabase.innerModel().tags());
            // Assemble the connection string to the database.
            final String domain = sampleSQLServer.fullyQualifiedDomainName();
            String url = "jdbc:sqlserver://"+ domain + ":1433;" +
                    "database=" + sqlDbName +";" +
                    "user=" + adminUser+ "@" + sqlServerName + ";" +
                    "password=" + dbPassword + ";" +
                    "encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

            // Connect to the database, create a table, and insert an entry into it.
            Connection conn = DriverManager.getConnection(url);

            String createTable = "CREATE TABLE CLOUD ( name varchar(255), code int);";
            String insertValues = "INSERT INTO CLOUD (name, code ) VALUES ('Azure', 1);";
            String selectValues = "SELECT * FROM CLOUD";
            Statement createStatement = conn.createStatement();
            createStatement.execute(createTable);
            Statement insertStatement = conn.createStatement();
            insertStatement.execute(insertValues);
            Statement selectStatement = conn.createStatement();
            ResultSet rst = selectStatement.executeQuery(selectValues);

            while (rst.next()) {
                System.out.println(rst.getString(1) + " "
                        + rst.getString(2));
            }


        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace().toString());
        }
    }

    private static void deployWebAppFromGithub() { // pass
        try {

            final String appName = "AzureAppHaoling";

            TokenCredential credential = new EnvironmentCredentialBuilder()
                    .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                    .build();

            // If you don't set the tenant ID and subscription ID via environment variables,
            // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            WebApp app = azureResourceManager.webApps().define(appName)
                    .withRegion(Region.US_WEST2)
                    .withNewResourceGroup("sampleWebResourceGroup")
                    .withNewWindowsPlan(PricingTier.FREE_F1)
                    .defineSourceControl()
                    .withPublicGitRepository(
                            "https://github.com/Azure-Samples/app-service-web-java-get-started")
                    .withBranch("master")
                    .attach()
                    .create();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void writeBlobToNewStorageAccount() { // pass
        try {
            TokenCredential tokenCredential = new EnvironmentCredentialBuilder()
                    .authorityHost(AzureAuthorityHosts.AZURE_PUBLIC_CLOUD)
                    .build();

            // If you don't set the tenant ID and subscription ID via environment variables,
            // change to create the Azure profile with tenantId, subscriptionId, and Azure environment.
            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(tokenCredential, profile)
                    .withDefaultSubscription();

            // Create a new storage account.
            String storageAccountName = "haolingstorage";
            StorageAccount storage = azureResourceManager.storageAccounts().define(storageAccountName)
                    .withRegion(Region.US_WEST2)
                    .withNewResourceGroup("sampleStorageResourceGroup")
                    .create();

            // Create a storage container to hold the file.
            List<StorageAccountKey> keys = storage.getKeys();
            PublicEndpoints endpoints = storage.endPoints();
            String accountName = storage.name();
            String accountKey = keys.get(0).value();
            String endpoint = endpoints.primary().blob();

            StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

            BlobServiceClient storageClient =new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .credential(credential)
                    .buildClient();

            // Container name must be lowercase.
            BlobContainerClient blobContainerClient = storageClient.getBlobContainerClient("helloazure");
            blobContainerClient.create();

            // Make the container public.
            blobContainerClient.setAccessPolicy(PublicAccessType.CONTAINER, null);

            // Write a blob to the container.
            String fileName = "helloazure.txt";
            String textNew = "Hello Azure";

            BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
            InputStream is = new ByteArrayInputStream(textNew.getBytes());
            blobClient.upload(is, textNew.length());

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    private static void testCostManagement() {
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        TokenCredential credential = new DefaultAzureCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                .build();

        HttpLogOptions httpLogOptions = new HttpLogOptions();
        httpLogOptions.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);

        CostManagementManager manager = CostManagementManager.configure().withLogOptions(httpLogOptions)
                .authenticate(credential, profile);

        Map<String, QueryAggregation> aggregateByTotalCost = Map.of("totalCost", new QueryAggregation()
                .withName("Cost")
                .withFunction(FunctionType.SUM));

        List<QueryGrouping> groupByTag = Collections.singletonList(
                new QueryGrouping()
                        .withType(QueryColumnType.fromString("TagKey"))
                        .withName("myTag"));

        QueryDataset queryDataset = new QueryDataset()
                .withAggregation(
                        aggregateByTotalCost)
                .withGrouping(
                        groupByTag);

        QueryDefinition queryDefinition = new QueryDefinition()
                .withType(ExportType.ACTUAL_COST)
                .withTimeframe(TimeframeType.MONTH_TO_DATE)
                .withDataset(queryDataset);

        String scope = "subscriptions/ec0aa5f7-9e78-40c9-85cd-535c6305b380";

        /*
        {
    "type": "ActualCost",
    "timeframe": "MonthToDate",
    "dataset": {
        "aggregation": {
            "totalCost": {
                "name": "Cost",
                "function": "Sum"
            }
        },
        "grouping": [
            {
                "type": "Tag",
                "name": "myTag"
            }
        ]
    }
}
         */
        QueryResult usage = manager
                .queries()
                .usage(scope, queryDefinition);

        System.out.println(usage);
    }

    private static void testNetwork() {
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        TokenCredential credential = new DefaultAzureCredentialBuilder()
                .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                .build();
        NetworkManager networkManager = NetworkManager
                .authenticate(credential, profile);

        PublicIpAddress pip =
                networkManager
                        .publicIpAddresses()
                        .define("my-network-ip")
                        .withRegion(Region.US_EAST)
                        .withNewResourceGroup("rg-haoling")
                        .withSku(PublicIPSkuType.STANDARD)
                        .withStaticIP()
                        .create();
    }
}
