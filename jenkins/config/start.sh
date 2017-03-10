#!/usr/bin/env bash

# this file allows docker image to take precedence over mounted volume
# which allows overriding some settings when deploying new version


# always override init scripts at startup
rm -rf /var/jenkins_home/init.groovy.d/*
cp -rp /usr/share/jenkins/ref/init.groovy.d/* /var/jenkins_home/init.groovy.d/

cp /usr/share/jenkins/ref/* /var/jenkins_home/

cp /usr/share/jenkins/ref/jobs/dsl-seed-job/config.xml /var/jenkins_home/jobs/dsl-seed-job/

# always reinstall plugins
rm -rf /var/jenkins_home/plugins/*

# remove all node data - we might have orphan nodes (slaves that will never be killed by jenkins)
# but there should be no problems with startup because of node no longer existing.
rm -rf /var/jenkins_home/nodes/*
export JAVA_OPTS="-Dhudson.DNSMultiCast.disabled=true -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/ec2-user -Xmx1524m"
export JENKINS_OPTS="-Djenkins.install.runSetupWizard=false"
export JAVA_ARGS="-Djenkins.install.runSetupWizard=false"

./bin/tini -- /usr/local/bin/jenkins.sh $@
