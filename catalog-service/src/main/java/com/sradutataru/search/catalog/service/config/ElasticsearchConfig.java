package com.sradutataru.search.catalog.service.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.host:localhost}")
    private String esHost;

    @Value("${spring.elasticsearch.port:9200}")
    private int esPort;

    @Value("${spring.elasticsearch.scheme:http}")
    private String esScheme;

    @Value("${spring.elasticsearch.username:elastic}")
    private String esUsername;

    @Value("${spring.elasticsearch.password:elastic}")
    private String esPassword;

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(esUsername, esPassword)
        );

        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(esHost, esPort, esScheme))
                        .setHttpClientConfigCallback(
                                httpClientBuilder -> httpClientBuilder
                                        .setDefaultCredentialsProvider(credentialsProvider)
                                        .setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(4).build())
                                )
                        );
    }
}
