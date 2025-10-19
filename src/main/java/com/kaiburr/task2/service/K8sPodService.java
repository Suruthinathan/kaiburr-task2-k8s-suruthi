package com.kaiburr.task2.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.springframework.stereotype.Service;

@Service
public class K8sPodService {

    public void createPod(String podName, String image) throws ApiException {
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        V1Pod pod = new V1Pod()
                .metadata(new V1ObjectMeta().name(podName))
                .spec(new V1PodSpec()
                        .containers(java.util.Collections.singletonList(
                                new V1Container()
                                        .name(podName)
                                        .image(image)
                        ))
                        .restartPolicy("Never")
                );

        api.createNamespacedPod("default", pod, null, null, null);
    }
}
