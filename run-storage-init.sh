#!/bin/bash
# Runs the initialization script for the Storage of the HOBBIT Platform.
VOS_CONTAINER_ID=$(docker ps -a | grep vos | cut -d' ' -f 1)
echo $VOS_CONTAINER_ID
docker exec -it $VOS_CONTAINER_ID bash ./storage-init.sh
