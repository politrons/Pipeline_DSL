// This script runs when Jenkins starts up (as it ends up in the $JENKINS_HOME/init.groovy.d/ dir).
def insideAws = true
def artifactoryIp

try {
    // This is the special AWS URL that provides each EC2 instance with access to it's own metadata.
    // See http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
    def metadata = new URL("http://instance-data/latest/dynamic/instance-identity/document").text
    artifactoryIp = new groovy.json.JsonSlurper().parseText(metadata).privateIp
    println "Running on AWS, IP: $artifactoryIp"
} catch(e) {
    insideAws = false

    def system = System.properties['os.name']
    println "System: $system"

    if (System.env["ARTIFACTORY_IP"]) {
        artifactoryIp = System.env["ARTIFACTORY_IP"]
    } else {
        // This is the default IP of docker machine on Mac / Windows. On Linux you have to
        // provide ARTIFACTORY_IP.
        artifactoryIp = "192.168.99.100"
    }
    println "Tried to read AWS data but got [$e], using $artifactoryIp instead"
}

System.properties["INSIDE_AWS"] = insideAws
System.properties["JENKINS_MASTER_IP"] = artifactoryIp
System.properties["MAVEN_REPO"] = "http://bintray.com/bintray/jcenter"

def artifactoryRepoUrl = "http://$artifactoryIp:8081/artifactory/jcenter/"

// TODO It might be better to add artifactory to docker-compose.yml and mount its volume so we
// would make it persistent (and faster).
if (!canConnect(artifactoryRepoUrl)) {
    println "Artifactory not responding, trying to start it."

    def checkContainer = "docker inspect artifactory".execute()
    checkContainer.waitFor()
    def artifactoryContainerExists = checkContainer.exitValue() == 0

    if (artifactoryContainerExists) {
        println "docker start artifactory".execute().err.text
    } else {
        println "docker run -m 1G -d -p 8081:8081 --name artifactory docker.bintray.io/jfrog/artifactory-oss:4.12.0.1".execute().err.text
    }
}

int remainingChecks = 50
println "Giving artifactory ${remainingChecks}s to start."
while (!canConnect(artifactoryRepoUrl) && remainingChecks > 0) {
    println "still waiting"
    Thread.sleep(1000)
    remainingChecks -= 1
}

if (canConnect(artifactoryRepoUrl)) {
    println "Artifactory is running - setting it as MAVEN_REPO."
    System.properties["MAVEN_REPO"] = artifactoryRepoUrl
} else {
    println "Artifactory is not running, so it will not be used."
}

def canConnect(url) {
    try {
        new URL(url).text
        return true
    } catch(e) {
        return false
    }
}
