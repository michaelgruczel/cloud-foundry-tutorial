# cloud foundry tutorial

![travis status](https://travis-ci.org/michaelgruczel/cloud-foundry-tutorial.svg?branch=master)

local development on cloud foundry for java developers

PCF Dev - Pivotal Cloud Foundry for Local Development - https://pivotal.io/pcf-dev

**requirements:**

* 4 GB virtual appliance that installs as a 20 GB virtual image. 
* at least 8 GB of system memory (16 GB recommended)
* VirtualBox 5.0 or higher
* Java 7 or higher
* git client

**install and start local PCF:**

install cli - https://github.com/cloudfoundry/cli#downloads
download appliance - https://network.pivotal.io/products/pcfdev and unzip it

    unzip pcfdev-VERSION-osx.zip

Install the PCF Dev plugin:

    ./pcfdev-VERSION-osx

Start PCF Dev:

    cf dev start -s all
    
after a very long running process you can enter the systems locally

* Apps Manager URL: https://local.pcfdev.io
* Admin user => Email: admin / Password: admin
* Regular user => Email: user / Password: pass    

and login by commandline by 

    cf login -a https://api.local.pcfdev.io --skip-ssl-validation
    
UI can be found under https://console.local.pcfdev.io or https://uaa.local.pcfdev.io/login    

**Start an app and connect to database**

PCF is a PAAS solution. In order to deploy your application you have to provide 
a build pack. Luckily for java spring boot applications and some other popular
setup pre-build build packages are available by default. In that case all you need is 
some meta info. We will do this with a sample app which uses a java build pack.
In the manifest.yml you will find the meta information needed to push an java
application to PCF.

deploy a demo app

    git clone https://github.com/cloudfoundry-samples/spring-music
    cd ./spring-music
    ./gradlew assemble
    cf push --hostname spring-music

open the app 
    
    spring-music.local.pcfdev.io    

check logs

    cf logs spring-music --recent
    cf logs spring-music    
    
add a mysql database from the PFC marketplace, make it available by exposing it as service
and connect demo app to it

    cf marketplace -s p-mysql
    cf create-service p-mysql 512mb my-spring-db
    cf services
    cf bind-service spring-music my-spring-db
    cf restart spring-music
    cf services

let's scale the app

    cf scale spring-music -i 2
    cf app spring-music
    cf scale spring-music -m 1G
    cf scale spring-music -k 512M        
    
let's clean up

    cf delete spring-music
    cf delete-service my-spring-db    
    
## create you custom services

a service needs a catalog description in config/settings.yml which describes
the service and a Dockerfile to package it.

an example can be found in https://github.com/cloudfoundry-samples/github-service-broker-ruby/tree/master/service_broker

## create you own buildpacks

A buildpack needs 3 scripts:

* bin/detect The detect script determines whether or not to apply the buildpack to an app. 
The script is called with one argument, the build directory containing the app files uploaded.
* bin/compile builds a droplet by packaging the app dependencies, assuring that the app has all the necessary components needed to run.
* bin/release provides feedback metadata to Cloud Foundry indicating how the app should be executed. The script is run with one argument, the build directory,
must generate a YAML file

    default_process_types:
       web: <start_command>

More details about buildpacks can be found under create your own buildpacks

And example can be found here https://github.com/cloudfoundry/java-buildpack 
Hints how to package a buildpack can be found here https://github.com/cloudfoundry/buildpack-packager
A buildpack can be uploaded or used by:

    $ cf push my-new-app -b git://github.com/johndoe/my-buildpack.git
    $ cf push my-new-app -b https://github.com/johndoe/my-buildpack#v1.4.2
    
more info can be found here https://docs.cloudfoundry.org/buildpacks/custom.html    
    
## Spring Cloud Netflix

**service discovery with eureka**

Installing Eureka, let's build it and start it in CF

    $ cd eureka
    $ ./gradlew build
    cf push
    
see http://eureka.local.pcfdev.io (:8761)
      
Now let's expose it as service

    cf apps    
    $ cf create-user-provided-service eureka-service -p '{"uri":"http://eureka:changeme@eureka.local.pcfdev.io"}'
  
Eureka is up and running (UI http://eureka.local.pcfdev.io 8761) as user-provided service instance (http://docs.pivotal.io/pivotalcf/1-9/devguide/services/user-provided.html)
User-provided service instances can be used to deliver service credentials to an application, and/or to trigger streaming of application logs to a syslog compatible consumer

Now let's deploy 2 services which register at eureka and 
a third one which retrieves the data from eureka

    $ cd spring-service-a
    $ ./gradlew build
    $ cf push
    $ cf bind-service spring-service-a eureka-service
    $ cf restage spring-service-a
    
    $ cd ./../spring-service-b
    $ ./gradlew build
    $ cf push
    $ cf bind-service spring-service-b eureka-service
    $ cf restage spring-service-b

    $ cd ./../spring-service-discovery
    $ ./gradlew build
    $ cf push
    $ cf bind-service service-discovery-example eureka-service
    $ cf restage service-discovery-example

open the index urls of the services and you should get something like:

     Greetings from service a
     Greetings from service b
     Greetings from Spring Boot to <Information about the registered instances at eureka>

a more advanced example can be found here https://github.com/joshlong/service-registration-and-discovery   
    
**Client-Side Load Balancing with Ribbon**

Let us now scale service-a

    cf scale spring-service-a -i 2
    
the call to http://spring-service-a-....local.pcfdev.io    
should now return different string depending on which instance
is requested, a call to will now be loadbalanced

    http://service-discovery-example-...local.pcfdev.io/lb-test

**circuit breaker**

remove the service a now and upload a faulty version of it

    $ cf delete spring-service-a
    $ cd spring-service-faulty-a
    $ ./gradlew build
    $ cf push       
    $ cf bind-service spring-service-a eureka-service
    $ cf restage spring-service-a
     

The service in spring-service-faulty-a has the name spring-service-a.
Spring-service-faulty-a gives correct responses at every even minute and crashes on not-even minutes.
Now call http://service-discovery-example-besprent-millipoise.local.pcfdev.io/circuit-breaker
which is equal to lb-test apart from the circuit breaker protection
If the services do not response hystrix will close the connection and returns a default value "They do not respond"

Let's make visible whether a circuit is closed:

    $ cf marketplace
    $ cf marketplace -s p-circuit-breaker-dashboard
    $ cf create-service p-circuit-breaker-dashboard standard circuit-breaker-dashboard
    $ cf service circuit-breaker-dashboard
    $ cf bind-service service-discovery-example circuit-breaker-dashboard

more details can be found at:

* https://docs.pivotal.io/spring-cloud-services/1-3/circuit-breaker/creating-an-instance.html    
* https://docs.pivotal.io/spring-cloud-services/1-3/circuit-breaker/writing-client-applications.html    

## zipkin

Lets push a zipkin server

    $ wget -O zipkin.jar 'https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec'
    $ cf push zipkin-server -p zipkin.jar -m 512M

you can see the UI of zipkin on http://zipkin-server.local.pcfdev.io

    $ cf bind-service zipkin-server eureka-service
    $ cf cups zipkin -p '{"uri":"http://zipkin-server.local.pcfdev.io"}'
    $ cf bind-service service-discovery-example zipkin
    $ cf bind-service spring-service-a zipkin

## application logging
     
in case cf logs APP or cf logs APP --recent does not make you happy,
you maybe want to direct the logs to an ELK stack.

setup an ELK stack in a virtual box

    $ cd ELK
    $ vagrant up

let's bind it

    $ cf cups logstash-drain -l syslog://192.168.33.10:5000
    $ cf bind-service service-discovery-example logstash-drain
    $ cf restage service-discovery-example   
    
now you should see the logs in Kibana http://localhost:5600/kibana/index.html#/dashboard/file/logstash.json   

in case of issues check:

* /var/log/elasticsearch (vagrant ssh)
* /etc/elasticsearch/elasticsearch.yml (vagrant ssh)
* sudo service elasticsearch status (vagrant ssh)
* echo -n "test message 3" | nc -4u -w1 localhost 5000 (vagrant ssh)
* /var/log/logstash/logstash.log(vagrant ssh)
* http://localhost:9200/_nodes
* http://192.168.33.10:9200

## blue/gree deployments

by default cloud foundry stops all instance before starting new ones,
but it is possible to do a blue/green deployment.
This is done by deploying the new version under a different route
and then moving the route by map-route

let's say you have pushed a service

    $ cf push my-service-v1 -n my-hostname

then you want to deploy a new version without downtime

    $ cf push my-service-v2 -n my-hostname-temp
    $ cf map-route my-service-v2 example.com -n my-hostname
    $ cf unmap-route my-service-v1 example.com -n my-hostname
    $ cf unmap-route my-hostname-temp.example.com
    $ cf delete my-service-v1
    
There are plugins available to do this for you

install cf-blue-green-deploy:

     $ cf add-plugin-repo CF-Community https://plugins.cloudfoundry.org
     $ cf install-plugin blue-green-deploy -r CF-Community

instead of push

    $ cd spring-service-a
    $ cf blue-green-deploy spring-service-a
    $ cf delete spring-service-a-old

the update will appear without any downtime

see https://github.com/bluemixgaragelondon/cf-blue-green-deploy for more details   
     
## service discovery with consul

WARNING: This did not work when i have done it, not researched yet why

you can install from https://www.consul.io/downloads.html and copy to /usr/local/bin/consul or brew install consul  

for this tutuorial there is a consul installation in a vagrant box
 
    $ cd consul
    $ vagrant up
    $ curl 192.168.33.1:8500/v1/catalog/nodes

take a look into the ui http://192.168.33.11:8500/ui    
    
    
it can be needed to set the hostname with -node   

now deploy the service spring-service-consul, it's comparable to spring-service-faulty-a,
means it will be unhealthy every not even minute
apart from giving it the information where consul can be found
in this example consul is not deployed in cloud foundry itself, so just do

    $ cd spring-service-consul
    $ ./gradlew clean build
    $ cf push 
     
## config

The config server retrieves his data from a git repository, the apps retrieve that data from the config server.
The config server can be deployed as normal application or from the marketplace (our approach in this tutorial)

You can use labels and profiles to select different versions of the properties for example for staging_

* profiles can be things like development or production 
* label can be a Git commit hash or branch name

we will start the configtest service
It will use configtest.properties by default
but in our case we will set the profile development so configtest-development.properties will be used

    $ cf marketplace -s p-config-server
    $ cf create-service p-config-server standard config-server 
    $ cf update-service config-server -c githubinfo.json

let's push the app (it will not start because of missing properties, that's ok)

    $ cd configtest
    $ ./gradlew clean build
    $ cf push
 
more complex stuff would be possible, e.g.
 
    $ cf create-service -c '{"git": { "uri": "https://github.com/michaelgruczel/cloud-foundry-tutorial", "cloneOnStart": "true", "repos": { "cook": { "pattern": "cook*", "uri": "https://github.com/michaelgruczel/config-test" } } }, "count": 3 }' p-config-server standard config-server-complex

but for this tutorial we don't need that    
    
    $ cf service config-server
    $ cf bind-service config-test config-server
    $ cf set-env configtest SPRING_PROFILES_ACTIVE development
    $ cf restage config-test
    $ cf env config-test
    $ curl http://config-test.local.pcfdev.io/hello


instead of defining the profile on command line it would be possible in application.yml or manifest as well, e.g.

    applications:
      - name: cook
        host: cookie
        services:
          - config-server
        env:
          SPRING_PROFILES_ACTIVE: production

a change in the properties (means git push) will change the runtime property by calling the refresh rest interface on the app

    $ git push
    $ curl -X POST http://configtest.local.pcfdev.io/refresh ["configtest.message"]


more details in
    
http://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_spring_cloud_config_server     
http://docs.pivotal.io/spring-cloud-services/1-3/config-server/
http://cloud.spring.io/spring-cloud-config/
               
## more hints    

more spring cloud examples
http://projects.spring.io/spring-cloud/
http://projects.spring.io/spring-cloud/#quick-start

How PCF Works 
https://docs.pivotal.io/pivotalcf/concepts

PCF Documentation 
https://docs.pivotal.io/pivotalcf/installing/pcf-docs.html

cf push appname [-b buildpack_name] [-c start_command] [-f manifest_path] [-i instance_number] [-k disk_limit] [-m memory_limit] [-n host_name] [-p app_path] [-s stack_name] [-t timeout_length] [--no-hostname] [--no-manifest] [--no-route] [--no-start] [--random-route]
