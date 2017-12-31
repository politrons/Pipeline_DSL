import hudson.model.*
import javaposse.jobdsl.dsl.Job

import static StepExtensions.setupGitRepository

def projectName = ""
def repository = ""
def sonarUri = ""
def emailNotification=""

println "###### Loading job parameters ######"
binding.variables.each {
    if ("${it.key}" == "Project_name") {
        projectName = "${it.value}"
    }

    if ("${it.key}" == "Git_repository") {
        repository = "${it.value}"
    }

    if ("${it.key}" == "Sonar_URI") {
        sonarUri = "${it.value}"
    }

    if ("${it.key}" == "Email_notification") {
        emailNotification = "${it.value}"
    }
}

class StepExtensions {

    def static setupGitRepository(Job delegate, String projectName, String repository) {
        delegate.scm {
            git {
                remote {
                    url("git@$repository")
                    credentials("git_user_$projectName")
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
                component("Pipeline for project $project_name", "$project_name/build")
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

        setupGitRepository(projectName, repository)

        triggers {
            cron("H */3 * * *")
        }

        wrappers {
            timeout {
                absolute(120)
            }
        }

        steps {
            shell("mvn clean install")
        }

        publishers {
            gitLabCommitStatusPublisher {
                name('build')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
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

        wrappers {
            timeout {
                absolute(120)
            }
        }

        steps {
            shell("mvn install -P integration")
        }

        publishers {
            gitLabCommitStatusPublisher {
                name('integration')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
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

        wrappers {
            timeout {
                absolute(120)
            }
        }

        steps {
            shell("""
                echo "Checking quality gate for sonar $sonarUri and project key $projectName"
                mvn sonar:sonar
                sleep 3m
                curl "$sonarUri/api/qualitygates/project_status?projectKey=$projectName"
               """)
        }

        publishers {
            gitLabCommitStatusPublisher {
                name('sonar')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
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

        wrappers {
            timeout {
                absolute(120)
            }
        }

        steps {
            shell("mvn install -P performance")
        }

        publishers {
            gitLabCommitStatusPublisher {
                name('performance')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
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

        wrappers {
            timeout {
                absolute(120)
            }
        }

        steps {
            shell("mvn install -P volume")
        }

        publishers {
            gitLabCommitStatusPublisher {
                name('volume')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
        }
    }

    createPipelineView("$projectName")
}