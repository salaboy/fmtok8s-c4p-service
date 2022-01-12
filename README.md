# Fmtok8s-c4p-rest
From Monolith to K8s: Call for Proposals (REST based)

## Build and Release

```
mvn package
```

```
docker build -t salaboy/fmtok8s-c4p-rest:0.1.0
docker push salaboy/fmtok8s-c4p-rest:0.1.0
```

```
cd charts/fmtok8s-c4p-rest
helm package .
```

Copy tar to http://github.com/salaboy/helm and push