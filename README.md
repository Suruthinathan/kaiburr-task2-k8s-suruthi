
# Kaiburr Task 2 – Kubernetes Deployment of Java REST API

**Name:** Suruthi  
**Date:** 19-Oct-2025  

---

## **Overview**

This repository contains the Kubernetes deployment for the Java REST API application developed in **Task 1**.  
The application provides endpoints to manage `Task` objects and execute shell commands.  

In Task 2, the application has been containerized using Docker and deployed to a Kubernetes cluster along with a MongoDB database.  
The PUT `/tasks/{id}/execution` endpoint has been updated to execute commands inside a **Kubernetes pod** instead of locally.  

---

## **Repository Structure**

```

kaiburr-task2-k8s-suruthi/
├── Dockerfile                  # Dockerfile for Java app
├── k8s/
│   ├── mongodb-pvc.yaml        # PersistentVolumeClaim for MongoDB
│   ├── mongodb-deployment.yaml # MongoDB Deployment
│   ├── mongodb-service.yaml    # MongoDB Service
│   ├── taskapp-deployment.yaml # Java App Deployment
│   └── taskapp-service.yaml    # Java App Service
├── src/                        # Java source code (Task 1)
├── screenshots/                # Place all screenshots here
└── README.md
└── pom.xml
````

---

## **Prerequisites**

- Java 17 installed  
- Maven 
- Docker installed  
- Kubernetes cluster (Docker Desktop)  
- kubectl configured and pointing to the cluster  

---

## **Step 1 – Build Java Application**

Navigate to the `src/` folder (or project root if Maven/Gradle is there):

**Maven:**
```bash
mvn clean package -DskipTests
````

The compiled JAR file will be available in `target/` (Maven).

---

## **Step 2 – Build Docker Image**

From the root directory (where `Dockerfile` is located):

```bash
docker build -t suruthi/kaiburr-task2:v1 .
```

* **For Minikube:**

```bash
minikube image load suruthi/kaiburr-task2:v1
```

* **Optional – Push to Docker Hub:**

```bash
docker push suruthi/kaiburr-task2:v1
```

---

## **Step 3 – Deploy MongoDB to Kubernetes**

Apply the manifests in order:

```bash
kubectl apply -f mongodb-pvc.yaml
kubectl apply -f mongodb-deployment.yaml
kubectl apply -f mongodb-service.yaml
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml

```

Verify MongoDB deployment:

```bash
kubectl get pods
kubectl get pvc
kubectl get svc
```

## **Step 4 – Deploy Java Application**

Apply the Java app manifests:

```bash
kubectl apply -f k8s/taskapp-deployment.yaml
kubectl apply -f k8s/taskapp-service.yaml
```

Verify deployment:

```bash
kubectl get pods
kubectl get svc
```

Access the service:

* NodePort:

```bash
kubectl get svc taskapp
```

Then visit: `http://<minikube-ip>:<nodePort>`

* Or Port Forward:

```bash
kubectl port-forward svc/taskapp 8080:8080
curl http://localhost:8080/tasks

## **Step 5 – PUT TaskExecution with Kubernetes Pod Execution**

The PUT `/tasks/{id}/execution` endpoint has been modified to:

1. Programmatically create a Kubernetes pod using Fabric8 client.
2. Execute the given shell command inside the pod.
3. Capture `startTime`, `endTime`, and `output` from the pod logs.
4. Store the result as a `TaskExecution` object in MongoDB.

**Sample Java snippet:**

```java
String[] command = {"sh","-c","echo Hello from Kubernetes pod"};
String output = k8sCommandRunner.runCommandInPod("default", "task-"+taskId, command, 60);
```

* **Security validation:** Only safe commands are allowed (disallow destructive or malicious commands).


## **Step 6 – Verify MongoDB Persistence**

1. Create some tasks via API.
2. Delete MongoDB pod:

```bash
kubectl delete pod -l app=mongodb
```

3. Wait for the pod to restart.
4. Query tasks again:

```bash
curl http://<app-service-url>/tasks
```


## **Step 7 – Screenshots Folder**

* Place all screenshots in `screenshots/` folder.
* Each screenshot must include:

  * System date/time
  * Your name visible somewhere (terminal, editor, etc.)

---

## **Step 8 – Notes**

* Ensure `MONGO_URI` or `SPRING_DATA_MONGODB_URI` env vars point to the MongoDB service in Kubernetes.
* Validate all commands executed in pods.
* Pods run with `restartPolicy: Never` and limited permissions.
* All PVCs are used for MongoDB persistence.

---

## **Step 9 – References**

* [Kubernetes Documentation](https://kubernetes.io/docs/)
* [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client)
* [Docker Documentation](https://docs.docker.com/)

---
