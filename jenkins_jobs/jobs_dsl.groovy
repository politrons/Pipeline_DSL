import hudson.model.*
import javaposse.jobdsl.dsl.Job

import static StepExtensions.setupGithub
def projectName = ""
def repository = ""

println "###### Loading job parameters ######"
binding.variables.each {
    if ("${it.key}" == "Project_name") {
        projectName = "${it.value}"
    }

    if ("${it.key}" == "repository") {
        repository = "${it.value}"
    }
}

class StepExtensions {

    def static setupGithub(Job delegate, String repository) {
        delegate.scm {
            git {
                remote {
                    github(repository, "ssh", "github.com")
                    credentials('github_user')
                }
            }
        }
    }
}

def createPipelineView = {
    String project_name ->
        deliveryPipelineView("$project_name/Pipeline") {
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
                component('Api sdk compile and build', "$project_name/build")
            }
        }
}

/**
 * The Build jobs
 */

use(StepExtensions) {

    folder(projectName) {
        description("Project $projectName")
    }

    /**
     * build/job
     */
    job("$projectName/build") {

        setupGithub(repository)

        steps {
            shell("mvn clean install")
        }

        publishers {
            downstreamParameterized {
                trigger(["$projectName/integration"]) {

                }
            }
        }
    }

    /**
     * integration/job
     */
    job("$projectName/integration") {

        steps {
            shell("mvn install -P integration")
        }

        publishers {
            downstreamParameterized {
                trigger(["$projectName/sonar"]) {

                }
            }
        }
    }

    /**
     * sonar/job
     */
    job("$projectName/sonar") {

        steps {
            shell("sonar command")
        }

        publishers {
            downstreamParameterized {
                trigger(["$projectName/performance"]) {

                }
            }
        }
    }

    /**
     * performance/job
     */
    job("$projectName/performance") {

        steps {
            shell("mvn install -P performance")
        }

        publishers {
            downstreamParameterized {
                trigger(["$projectName/volume"]) {

                }
            }
        }
    }

    /**
     * volume/job
     */
    job("$projectName/volume") {

        steps {
            shell("mvn install -P volume")
        }
    }

    createPipelineView("$projectName")
}