# Project DSL

![My image](img/butler.png)![My image](img/logo-groovy.png)


A Jenkins dsl to create a CI/CD pipeline for your projects.

For this project we use the [Jenkins DSL](http://localhost:8080/plugin/job-dsl/api-viewer/index.html) which using groovy language.

The pipeline compose to the next jobs

```
Unit test -> Integration test -> Sonar -> Performance test -> Volume test
```
