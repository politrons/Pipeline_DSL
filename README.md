# Pipeline DSL

![My image](img/butler.png)![My image](img/logo-groovy.png)


A Jenkins DSL project to create CI/CD pipeline for your project.

For this project we use the [Jenkins DSL](http://localhost:8080/plugin/job-dsl/api-viewer/index.html) which using [Groovy](http://groovy-lang.org/) language.

The pipeline is compose by the next jobs

```
Unit test -> Integration test -> Sonar -> Performance test -> Volume test
```
