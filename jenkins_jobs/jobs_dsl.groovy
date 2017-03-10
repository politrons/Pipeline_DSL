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
                component('Api sdk compile and build', "branches/$branch_name/build")
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
     * sonar/job
     */
    job("$branchFolder/sonar") {

        setupGithub(branch_name, repository)

        steps {
            gradleRun("clean build")
        }
    }

    /**
     * github_state/job
     */
    job("$branchFolder/github_state") {

        steps {
            gradleRun("clean build")
        }
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
                trigger(["$branchFolder/performance", "$branchFolder/integration","$branchFolder/sonar"]) {

                }
            }


            /*def nextJobName="$branchFolder/github_state"
            if (is_master) {
                  nextJobName = "deploy/price/price_dev"
            } else {
                  nextJobName = "$branchFolder/github_state"
            }

            joinTrigger {
                  publishers {
                      downstreamParameterized {
                          trigger(["$branchFolder/github_state"]) {
                          }
                      }
                  }
            }*/


        }
    }

    createPipelineView("$branch_name")
}