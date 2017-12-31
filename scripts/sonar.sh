#!/usr/bin/env bash

sonarURI="$1"
projectName="$2"
echo "Checking quality gate for sonar $sonarURI and project key $projectName"
mvn sonar:sonar
sleep 3m
curl "$sonarURI/api/qualitygates/project_status?projectKey=$projectName"