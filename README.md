# cloud foundry tutorial

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
    
UI can be found under https://uaa.local.pcfdev.io/login    

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

Installing Eureka, let's build it and start it in CF

    $ cd eureka
    $ ./gradlew build
    cf push
    
see http://eureka.local.pcfdev.io (:8761)
      
Now let's expose it as service

    cf apps    
    $ cf create-user-provided-service eureka-service -p '{"uri":"http://eureka:changeme@eureka.local.pcfdev.io"}'
  
Eureka is up and running (UI on port 8761) as user-provided service instance (http://docs.pivotal.io/pivotalcf/1-9/devguide/services/user-provided.html)
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
    $ cf restart service-discovery-example

open the index urls of the services and you should get something like:

     Greetings from service a
     Greetings from service b
     Greetings from Spring Boot to <Information about the registered instances at eureka>

a more advanced example can be found here https://github.com/joshlong/service-registration-and-discovery   
    
## more hints    

more spring cloud examples
http://projects.spring.io/spring-cloud/

How PCF Works 
https://docs.pivotal.io/pivotalcf/concepts

PCF Documentation 
https://docs.pivotal.io/pivotalcf/installing/pcf-docs.html

