# From Monolith to K8s: Call for Proposals Service

To install this service on a Kubernetes cluster you can use helm: 

```
helm install c4p fmtok8s/fmtok8s-c4p-service 
```

If you installed the service in the `default` namespace you can access to it from outside the cluster with: 
```
kubectl port-forward svc/fmtok8s-c4p -n default 8080:80

```

To uninstall the service you also need to delete the `postgresql` PVC that is created by the postgresql chart that is installed as a dependnecy for this service. 

## Interacting with the service

You can use `curl` or `httpie` to send requests to the exposed REST Endpoints: 

Submit a new proposal with: 

```
http post :8080 @proposal.json
```

To get all proposals: 

```
http :8080
```

Approve the proposal: 

```
http post :8080/1/decision @approve.json
```

Where `1` is the proposal id.


## Service Pipelines

This repository contains three service pipeline definitions:
- Tekton: you can find the Tekton Pipeline definition under the [`tekton` directory](tekton/README.md). 
- Dagger: you can find the Dagger pipeline defintion inside the []`pipeline.go` file](pipeline.go). 
- GitHub Actions: you can find the GitHub action definition under the [`.github/workflows/` directory](.github/workflows/ci_workflow.yml).