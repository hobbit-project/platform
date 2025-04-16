# Kubernetes YAML Files

This directory contains the Kubernetes YAML configuration files used by the Hobbit project to deploy and manage application components on Kubernetes clusters. These configurations define the deployments, services, ingress rules, and other necessary Kubernetes objects.

## Overview

The Kubernetes YAML files in this directory are essential for automating the provisioning and scaling of services. The configurations allow seamless deployments as part of the project's continuous integration and delivery pipelines. They are meant to be used as a starting point and can be customized to better suit different environments or specific deployment requirements.

## Directory Structure

The directory is organized to facilitate clarity and ease of use:
- **Deployment Files:** Define how pods are deployed and managed. This usually includes specifications such as replicas, container images, resource limits, and update strategies.
- **Service Files:** Expose the pods to internal or external networks. These YAMLs typically cover ClusterIP, NodePort, or LoadBalancer services.
- **Ingress Files:** Manage external access to the services in the cluster. These files are configured to work with Ingress controllers.
- **ConfigMap/Secret Files:** Provide a way to decouple configuration settings and sensitive data from container images. Adjust these according to your environment’s needs.

## Prerequisites

To successfully apply these configurations:
- You must have a working Kubernetes cluster.
- Ensure that `kubectl` is installed and configured to communicate with your cluster.
- Familiarity with Kubernetes concepts (pods, deployments, services, ingress, etc.) is helpful.

## Usage

### Applying the Configurations

1. **Navigate to the Directory:**
   Open your terminal and change the current directory to the location where the YAML files are stored.

   ```bash
   cd path/to/kubernetes-yaml-files
   ```

2. **Deploy the Resources:**
   Use `kubectl` to apply all the configurations at once:

   ```bash
   kubectl apply -f .
   ```

   This command iterates through all YAML files and creates or updates the Kubernetes objects accordingly.

3. **Verify the Deployment:**
   To check if the deployments are running correctly, use:

   ```bash
   kubectl get deployments
   kubectl get pods
   ```

   Review logs if any of the pods are not running as expected:

   ```bash
   kubectl logs [pod-name]
   ```
it is suggester keep the order of files while deploying.(00 until 21 the next ones are for testing and development purpose)  

### Customizing Configurations

- **Environment Variables:** Some files might reference environment variables. Edit these sections as needed to match your environment.
- **Resource Limits:** Customize CPU and memory resources based on your cluster’s capacity.
- **External Dependencies:** Review and adjust configurations for services like Ingress or persistent storage if your project setup requires it.
- **Define secrets of Git based on your data:** like bellow
```bash
kubectl create secret generic gitlab-secret --from-literal=GITLAB_USER=XXX --from-literal=GITLAB_TOKEN=XXX
````
and if need to reach docker for images like this 

```bash
kubectl create secret docker-registry gitlab-registry-secret \ --docker-server=[for example:git.project-hobbit.eu:4567] \ --docker-username=XXX \ --docker-password=XXX \ --docker-email=XXX \ -n default

```

```
