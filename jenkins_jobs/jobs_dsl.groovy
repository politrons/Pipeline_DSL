import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.step.StepContext
import static StepExtensions.*

//import commons.*

class StepExtensions {
    def static gradleRun(StepContext delegate, String task) {
        delegate.gradle {
            tasks(task)
            def cacheDir = '$WORKSPACE/.gradle/'
            switches "-Dgradle.user.home=$cacheDir --project-cache-dir $cacheDir"
            switches '--console=plain'
            switches '-Dfile.encoding="UTF-8"'
            useWrapper false
        }
    }

    def static setupGithub(Job delegate, String branch, String repository) {
        delegate.scm {
            git {
                remote {
                    github(repository, "ssh", "github.com")
                    credentials('github_user')
                    branches(branch)
                }
            }
        }
    }
}

def createPipelineView = {
    String branch_name ->
        deliveryPipelineView("branches/$branch_name/Pipeline") {
            allowPipelineStart()
            allowRebuild()
            showAggregatedPipeline()
            updateInterval(30)
            enableManualTriggers()
            showAvatars()
            showChangeLog()
            showPromotions()
            showTotalBuildTime()
            pipelines {
                component('Api sdk compile and build', "branches/$branch_name/build")
            }
        }
}



/**
 * The Build jobs
 */

use(StepExtensions) {

//    getBrachesNames()

    [[branchName: "feature1"],
     [branchName: "feature2" ],
     [branchName: "feature3"]].each { env ->7

        def branch_name= env.branchName
        def repository = "politrons/API_JENKINS"
        def branchFolder = "branches/$branch_name"

        folder("branches") {
            description("Branches with build pipeline defined")
        }

        folder(branchFolder) {
            description("Branch $branch_name")
        }

        /**
         * build/job
         */
        job("$branchFolder/build") {

            setupGithub(branch_name, repository)

            steps {
                gradleRun("clean build")
            }

            publishers {
                downstreamParameterized {
                    trigger(["$branchFolder/integration"]) {

                    }
                }
            }
        }

        /**
         * integration/job
         */
        job("$branchFolder/integration") {

            setupGithub(branch_name, repository)

            steps {
                gradleRun("clean verify")
            }

            publishers {
                downstreamParameterized {
                    trigger(["$branchFolder/sonar"]) {

                    }
                }
            }
        }

        /**
         * sonar/job
         */
        job("$branchFolder/sonar") {

            setupGithub(branch_name, repository)

            steps {
                gradleRun("clean build")
            }

            publishers {
                downstreamParameterized {
                    trigger(["$branchFolder/performance"]) {

                    }
                }
            }
        }

        /**
         * performance/job
         */
        job("$branchFolder/performance") {

            setupGithub(branch_name, repository)

            steps {
                gradleRun("clean build")
            }

            publishers {
                downstreamParameterized {
                    trigger(["$branchFolder/volume"]) {

                    }
                }
            }
        }


        /**
         * volume/job
         */
        job("$branchFolder/volume") {

            setupGithub(branch_name, repository)

            steps {
                gradleRun("clean build")
            }
        }

        createPipelineView("$branch_name")
    }
}