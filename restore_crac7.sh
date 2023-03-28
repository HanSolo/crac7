#!/bin/bash

echo "docker run -it --privileged --rm --name $1 hansolo/crac7:checkpoint java
-XX:CRaCRestoreFrom=/opt/crac-files"

docker run -it --privileged --rm --name $1 hansolo/crac7:checkpoint java -XX:CRaCRestoreFrom=/opt/crac-files