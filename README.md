# From Monolith to K8s: Call for Proposals Service

To install this service on a Kubernetes cluster you can use helm: 

```

```

If you installed the service in the `default` namespace you can access to it from outside the cluster with: 
```
kubectl port-forward svc/fmtok8s-c4p -n default 8080:80

```

To uninstall the service you also need to delete the `posgresql` PVC. 
