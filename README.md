# Project DSL

A Jenkins dsl to create a CI/CD pipeline for your projects.

For this project we use the [Jenkins DSL](http://localhost:8080/plugin/job-dsl/api-viewer/index.html) which using groovy language we can create pipelines.

Those pipeline compose the jobs

```
Unit test -> Integration test -> Sonar -> Performance test -> Volume test
```
