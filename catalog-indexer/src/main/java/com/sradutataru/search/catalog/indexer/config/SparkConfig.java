package com.sradutataru.search.catalog.indexer.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("Catalog Indexer")
                .master("local[*]")
                .config("es.index.auto.create", "true")
                .config("es.nodes", "localhost")
                .config("es.port", "9200")
                .config("es.nodes.wan.only", "true")
                .config("es.net.http.auth.user", "elastic")
                .config("es.net.http.auth.pass", "ElasticRocks!")
                .getOrCreate();
    }
}
