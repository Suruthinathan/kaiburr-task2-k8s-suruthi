package com.kaiburr.task2.util;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class K8sCommandRunner {

    private final KubernetesClient client;

    public K8sCommandRunner() {
        Config config = Config.autoConfigure(null);
        client = new DefaultKubernetesClient(config);
    }

    public String runCommandInPod(String namespace, String namePrefix, String[] command, long timeoutSeconds) throws Exception {
        String podName = namePrefix + "-" + System.currentTimeMillis();

        Pod pod = new PodBuilder()
                .withNewMetadata().withName(podName).endMetadata()
                .withNewSpec()
                    .withRestartPolicy("Never")
                    .addNewContainer()
                        .withName("runner")
                        .withImage("busybox:1.36")
                        .withCommand(command)
                        .withTty(false)
                    .endContainer()
                .endSpec()
                .build();

        pod = client.pods().inNamespace(namespace).create(pod);

        long start = System.currentTimeMillis();
        while (true) {
            Pod current = client.pods().inNamespace(namespace).withName(podName).get();
            String phase = current.getStatus().getPhase();
            if ("Succeeded".equals(phase) || "Failed".equals(phase)) {
                break;
            }
            if ((System.currentTimeMillis() - start) / 1000 > timeoutSeconds) {
                client.pods().inNamespace(namespace).withName(podName).delete();
                throw new RuntimeException("Timeout waiting for pod");
            }
            Thread.sleep(1000);
        }

        String logs = client.pods().inNamespace(namespace).withName(podName).getLog();

        // Delete pod after capturing logs
        client.pods().inNamespace(namespace).withName(podName).delete();

        return logs;
    }
}
