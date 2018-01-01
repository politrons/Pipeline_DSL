import hudson.model.*
import javaposse.jobdsl.dsl.Job

import static StepExtensions.setBuildJob
import static StepExtensions.setIntegrationJob
import static StepExtensions.setSonarJob
import static StepExtensions.setPerformanceJob
import static StepExtensions.setVolumeJob

def projectName = ""
def repository = ""
def sonarUri = ""
def emailNotification = ""

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

    /**
     * Setup build job
     */
    def static setBuildJob(Job delegate, String projectName, String repository, String emailNotification) {
        setupGitRepository(delegate, projectName, repository)
        delegate.triggers {
            cron("H */3 * * *")
        }
        delegate.wrappers {
            timeout {
                absolute(120)
            }
        }
        delegate.steps {
            shell("mvn clean install")
        }
        delegate.publishers {
            gitLabCommitStatusPublisher {
                name('build')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
            downstreamParameterized {
                trigger("$projectName/integration") {
                    parameters {
                        predefinedProp('VERSION', '1.0.0')
                    }
                }
            }
        }
    }

    /**
     * Setup integration job
     */
    def static setIntegrationJob(Job delegate, String projectName, String emailNotification) {
        delegate.wrappers {
            timeout {
                absolute(120)
            }
        }
        delegate.steps {
            shell("""
                    cd ..
                    cd build
                    "mvn install")
                """)
        }
        delegate.publishers {
            gitLabCommitStatusPublisher {
                name('integration')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
            downstreamParameterized {
                trigger(["$projectName/sonar"]) {
                    parameters {
                        predefinedProp('VERSION', '1.0.0')
                    }
                }
            }
        }
    }

    /**
     * Setup sonar job
     */
    def static setSonarJob(Job delegate, String projectName, String sonarUri, String emailNotification) {
        delegate.wrappers {
            timeout {
                absolute(120)
            }
        }
        delegate.steps {
            shell("""
                cd ..
                cd build
                echo "Checking quality gate for sonar $sonarUri and project key $projectName"
                mvn sonar:sonar
                #sleep 3m
                #curl "$sonarUri/api/qualitygates/project_status?projectKey=$projectName"
               """)
        }
        delegate.publishers {
            gitLabCommitStatusPublisher {
                name('sonar')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
            downstreamParameterized {
                trigger(["$projectName/performance"]) {
                    parameters {
                        predefinedProp('VERSION', '1.0.0')
                    }
                }
            }
        }
    }

    /**
     * Setup performance job
     */
    def static setPerformanceJob(Job delegate, String projectName, String emailNotification) {
        delegate.wrappers {
            timeout {
                absolute(120)
            }
        }
        delegate.steps {
            shell("""
                    cd ..
                    cd build
                    "mvn install")
                """)
        }
        delegate.publishers {
            gitLabCommitStatusPublisher {
                name('performance')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
            downstreamParameterized {
                trigger(["$projectName/volume"]) {
                    parameters {
                        predefinedProp('VERSION', '1.0.0')
                    }
                }
            }
        }
    }

    /**
     * Setup volume job
     */
    def static setVolumeJob(Job delegate, String emailNotification) {
        delegate.wrappers {
            timeout {
                absolute(120)
            }
        }
        delegate.steps {
            shell("""
                    cd ..
                    cd build
                    "mvn install")
                """)
        }
        delegate.publishers {
            gitLabCommitStatusPublisher {
                name('volume')
                markUnstableAsSuccess(false)
            }
            mailer("$emailNotification", true, true)
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
 * The Pipeline jobs
 */
use(StepExtensions) {

    folder(projectName) {
        description("Project $projectName")
    }
    job("$projectName/build") {
        setBuildJob(projectName, repository, emailNotification)
    }
    job("$projectName/integration") {
        setIntegrationJob(projectName, emailNotification)
    }
    job("$projectName/sonar") {
        setSonarJob(projectName, sonarUri, emailNotification)
    }
    job("$projectName/performance") {
        setPerformanceJob(projectName, emailNotification)
    }
    job("$projectName/volume") {
        setVolumeJob(emailNotification)
    }

    createPipelineView("$projectName")
}