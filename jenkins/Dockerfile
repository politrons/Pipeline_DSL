
FROM jenkins:2.32.1

ENV JENKINS_UC_DOWNLOAD "http://mirrors.jenkins-ci.org"

#Jenkins should only work on SSL
EXPOSE 443
#This port is for artifactory
EXPOSE 50000

# Prefer to install plugins during container build rather than at container run, because it fails
# distressingly often. We prefer that failure to happen here.
COPY config/plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt

COPY config/xml/* /usr/share/jenkins/ref/

## These credentials and config are not in the project, but in S3 repository.
#COPY config/credentials.xml /usr/share/jenkins/ref/credentials.xml

#COPY config/config.tar /tmp/config.tar

#RUN tar -xf /tmp/config.tar  -C /tmp/
#RUN mv /tmp/config/* /usr/share/jenkins/ref/

COPY config/users.tar /tmp/users.tar
RUN tar -xf /tmp/users.tar  -C /usr/share/jenkins/ref

COPY dsl-seed-job /usr/share/jenkins/ref/jobs/dsl-seed-job

RUN rm /usr/share/jenkins/ref/init.groovy.d/*
COPY init/* /usr/share/jenkins/ref/init.groovy.d/

USER root

RUN rm /tmp/users.tar

RUN addgroup -gid 497 docker &&\
  adduser -system --uid 500 --gid 497 ec2 &&\
  usermod -a -G docker jenkins &&\
  usermod -a -G docker root &&\
  wget -nv https://get.docker.io/builds/Linux/x86_64/docker-1.9.1 -O /bin/docker &&\
  chmod +x /bin/docker

COPY config/start.sh /usr/local/bin/start.sh
ENTRYPOINT ["/usr/local/bin/start.sh"]
