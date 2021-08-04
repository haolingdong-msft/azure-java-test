package com.fabrikam;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.AzureAuthorityHosts;
import com.azure.identity.EnvironmentCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;

public class CleanupResourceGroups {
    public static void main(String[] args) {
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

//        System.out.println(azureResourceManager.resourceGroups().list().stream().filter(rg -> rg.name().startsWith("java") || rg.name().startsWith("rg")).count());
//        azureResourceManager.resourceGroups().list().stream().filter(rg -> rg.name().startsWith("java") || rg.name().startsWith("rg")).map(rg -> azureResourceManager.resourceGroups().deleteByNameAsync(rg.name())).map(result -> result.block());
        int count = 0;
        for(ResourceGroup resourceGroup : azureResourceManager.resourceGroups().list()) {
            String name = resourceGroup.name();

//            if(name.startsWith("java") || name.startsWith("rg")) {
                try {
                    azureResourceManager.resourceGroups().deleteByName(name);
                    count++;
                    System.out.println("delete" + resourceGroup.name());
//                    if(count == 1) {
//                        break;
//                    }

                } catch (Exception e) {
                    System.out.println(e);
                }
//            }
        }

        System.out.println(count + " resource groups deleted");
    }
}
