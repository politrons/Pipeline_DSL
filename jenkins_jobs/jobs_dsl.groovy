import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.step.StepContext

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
                component('Api sdk compile and build', "branches/$branch_name/api_sdk_build")
            }
        }
}

/**
 * The Build jobs
 */
use(StepExtensions) {
    def branch_name= "master"
    def repository = "politrons/API_JENKINS"
    def branchFolder = "branches/master"

    folder("branches") {
        description("Branches with build pipeline defined")
    }

    folder(branchFolder) {
        description("Branch $branch_name")
    }

    /**
     * performance/job
     */
    job("$branchFolder/performance") {

        setupGithub(branch_name, repository)

        steps {
            gradleRun("clean build")
        }
    }

    /**
     * integration/job
     */
    job("$branchFolder/integration") {

        setupGithub(branch_name, repository)

        steps {
            gradleRun("clean build")
        }
    }

    /**
     * build/job
     */
    job("$branchFolder/api_sdk_build") {

        setupGithub(branch_name, repository)

        steps {
            gradleRun("clean build")
        }

        publishers {

            downstreamParameterized {
                trigger(["$branchFolder/performance", "$branchFolder/integration"]) {

                }
            }

        }
    }

    createPipelineView("$branch_name")

}