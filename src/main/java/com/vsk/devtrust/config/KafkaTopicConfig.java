package com.vsk.devtrust.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${devtrust.kafka.topics.deployments}")
    private String deploymentsTopic;

    @Value("${devtrust.kafka.topics.anomalies}")
    private String anomaliesTopic;

    @Value("${devtrust.kafka.topics.incidents}")
    private String incidentsTopic;

    @Bean
    public NewTopic deploymentsTopic() {
        return TopicBuilder.name(deploymentsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic anomaliesTopic() {
        return TopicBuilder.name(anomaliesTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic incidentsTopic() {
        return TopicBuilder.name(incidentsTopic).partitions(3).replicas(1).build();
    }
}