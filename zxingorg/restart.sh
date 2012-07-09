#!/bin/bash
export CATALINA_OPTS="-server -da -dsa -Xmx80m"
while :
do
        pkill -KILL java
        #/usr/local/tomcat/bin/shutdown.sh
        sleep 5
        /usr/local/tomcat/bin/startup.sh
        sleep 7200
done