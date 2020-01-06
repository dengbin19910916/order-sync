package com.purcotton.omni.ordersync.config;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory requestFactory) {
        return new RestTemplate(requestFactory);
    }

    @Bean
    public ClientHttpRequestFactory httpsFactory() throws Exception {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    private static CloseableHttpClient httpClient() throws Exception {
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();

        ConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", socketFactory)
                .build();

        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(registry);
        manager.setMaxTotal(100);
        manager.setDefaultMaxPerRoute(20);

        return HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .setConnectionManager(manager)
                .build();
    }
}
