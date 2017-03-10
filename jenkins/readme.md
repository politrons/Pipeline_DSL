# Jenkins docker images

This folders stores jenkins docker image based on official ubuntu based docker image -
https://hub.docker.com/_/jenkins/

It consist of few parts:
- Dockerfile and docker-compose definition for it
- DSL seed job
- Groovy jenkins init scripts
- Jenkins configuration
  - Users
  - Plugins
  - New jenkins start script

## Running the image

One could build and run this image with:

```
docker build -t jenkins_image . && docker run -d --name="jenkins_container" --net="host" --privileged \
-v /var/jenkins_home:/var/jenkins_home \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /tmp:/tmp \
jenkins_image
```

This builds local dockerfile, tags it as *jenkins_image* and then run *jenkins_image* image and names it as *jenkins_container*. The flags are:
- `--net="host"` - use host networking, makes life easier on localhost, otherwise you need to expose ports 8080 and 50000 with -p
- `-v /var/jenkins_home:/var/jenkins_home` - needed to store state of jenkins (builds, workspaces, logs, configuration, credentials) in persistent way. It could also be done by using data volume on proper docker cloud.
- `--privileged -v /var/run/docker.sock:/var/run/docker.sock` - Those are needed to invoke docker from inside jenkins image. It is not docker in docker, it spans siblings not children. This allow us to spand additional images during jenkins startup. At the moment this is how Artifactory is created.
- `-v /tmp:/tmp` - Together with docker and *jenkins_home*, this flag allow local machine  to run build in docker slaves or run build in docker on master. That requires some folders to be accessible from spanned docker siblings, thus one must mount *jenkins_home* to the same folder on host as on image (buildInDocker mount the *jenkins_home* to share workspaces). It additionally requires tmp, because in this folder jenkins create start scripts and it is also mounted by buildInDocker.

One can also run, which mostly do the same (see [compose file](docker-compose.yml)):
```
docker-compose build && docker-compose up
```


## Start script

The `start.sh` script exists so changes in docker image (at the moment only init scripts) can override things on existing volume.

## Dockerfile

```
COPY config/plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
```

Copy list of plugins to docker image and download all of them using script from official image.

```
COPY config/config.xml /usr/share/jenkins/ref/config.xml
COPY config/users.tar /tmp/users.tar
RUN tar -xf /tmp/users.tar  -C /usr/share/jenkins/ref
```

Copy jenkins config and users to jenkins "ref" folder. Content of this folder is copied to JENKINS_HOME on first run. **It is not secure.** We should copy users from secure storage (i.e. S3 bucket that only jenkins machine has access to).

```
USER root

RUN rm /tmp/users.tar

RUN addgroup -gid 497 docker &&\
  adduser -system --uid 500 --gid 497 ec2 &&\
  usermod -a -G docker jenkins &&\
  usermod -a -G docker root &&\
  wget -nv https://get.docker.io/builds/Linux/x86_64/docker-1.9.1 -O /bin/docker &&\
  chmod +x /bin/docker
```

Switching the user to ROOT and installing docker. Docker is useful if one want to run local slave in docker. Jenkins image use *jenkins* user, we switch to *root* because there are some problems running as *jenkins* (docker and some other troubles).

## Jenkins initialization

We execute 2 scripts during jenkins initialization:
- `one_executor.groovy` - set number of executors on master to 1 - because of no good reason
- `setup.groovy` - run Artifactory, check private ip and set some system properties.

### setup.groovy

This file first tries to read our private ip as visible in vpc by using special link:

```
def metadata = new URL("http://instance-data/latest/dynamic/instance-identity/document").text
ip = new groovy.json.JsonSlurper().parseText(metadata).privateIp
```

Next it tries to check if artifactory is accessible, if not it tries to check if we have container
named artifactory, if yes we start it with docker, if not we run artifactory image and name it accordingly.

Eventually script sets 3 system properties:

```
System.properties["INSIDE_AWS"]
System.properties["JENKINS_MASTER_IP"]
System.properties["MAVEN_REPO"]
```

Which can be later accessed in system groovy scripts, as long as they run on master (setting up build parameters or env for slave, running build on master, etc.).

# Some commands

A typical build for me:

```
cd /Users/krzysztof/git/price-aws/images/jenkins
docker ps -q | xargs docker kill
#docker ps -a -q | xargs docker rm
docker-machine ssh default  "sudo rm -rf /var/jenkins_home/*"
#rm -rf /var/jenkins_home/*
docker build -t kk-jenkins . && docker run --net="host" --privileged \
-e ARTIFACTORY_IP=192.168.99.100 \
-v /var/jenkins_home:/var/jenkins_home \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /tmp:/tmp \
kk-jenkins
```

## Where are builds run?

We are using the [EC2 plugin][ec2-plugin], this, in addition to
providing Jenkins AWS credentials for EC2 access, allows our Jenkins
container to spin up instances on which to perform builds. See the
`clouds` section in [config/xml/config.xml](config/xml/config.xml).

[ec2-plugin]: https://wiki.jenkins-ci.org/display/JENKINS/Amazon+EC2+Plugin