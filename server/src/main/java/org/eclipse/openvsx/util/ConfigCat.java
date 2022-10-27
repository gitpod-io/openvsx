package org.eclipse.openvsx.util;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.configcat.*;
import com.ctc.wstx.shaded.msv_core.util.Uri;
import okhttp3.OkHttpClient;

@Component
public class ConfigCat {

    public static final String UpstreamURL = "openvsx_mirror_upstream_url";

    ConfigCatClient client;

    public ConfigCat(@Value("${configcat.base-url:https://gitpod-staging.com/configcat}") String baseUrl, @Value("${configcat.sdkKye:gitpod}") String sdkKey) {
        var httpClient = new OkHttpClient.Builder()
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build();
        var builder = ConfigCatClient.newBuilder()
            .mode(PollingModes.autoPoll(60))
            .httpClient(httpClient);
        if (Uri.isValid(baseUrl)) {
            builder.baseUrl(baseUrl);
        }
        client = builder.build(sdkKey);
    }

    
    public String getUpstreamURL(String defaultValue) {
        return client.getValue(
            String.class, 
            ConfigCat.UpstreamURL, 
            User.newBuilder().build(UUID.randomUUID().toString()), // We use random UUID here to control traffic
            defaultValue
        );
    }

}
