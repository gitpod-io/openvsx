package org.eclipse.openvsx.util;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.configcat.*;
import com.ctc.wstx.shaded.msv_core.util.Uri;
import okhttp3.OkHttpClient;

@Component
public class ConfigCat {

    public static final String UpstreamURL = "self_hosted_openvsx_upstream_url";

    ConfigCatClient client;

    public ConfigCat(@Value("${configcat.host:https://gitpod-staging.com}") String host) {
        var httpClient = new OkHttpClient.Builder()
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build();
        client = ConfigCatClient.newBuilder()
            .baseUrl(Uri.resolve(host, "/configcat").toString())
            .mode(PollingModes.autoPoll(60))
            .httpClient(httpClient)
            .build("gitpod");
    }

    
    public String getUpstreamURL(String defaultValue) {
        return this.getValue(String.class, ConfigCat.UpstreamURL, defaultValue);
    }

    public <T> T getValue(Class<T> clazz, String key, String userId, T defaultValue) {
        return client.getValue(
            clazz, 
            key, 
            User.newBuilder().build(userId),
            defaultValue
        );
    }

    public <T> T getValue(Class<T> clazz, String key, T defaultValue) {
        return client.getValue(
            clazz, 
            key,
            defaultValue
        );
    }

}
