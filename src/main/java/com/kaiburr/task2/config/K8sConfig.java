package com.kaiburr.task2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Configuration
public class K8sConfig {
    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }
}
