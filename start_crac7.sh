#!/bin/bash

echo "docker run -it --privileged --rm --name crac7 hansolo/crac7:checkpoint java -jar /opt/app/crac7-17.0.0.jar"

docker run -it --privileged --rm --name crac7 hansolo/crac7:checkpoint java -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/crac7-17.0.0.jar