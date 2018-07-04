#!/bin/bash
# Runs the initialization script for the Storage of the HOBBIT Platform.
VOS_CONTAINER_ID=$(docker ps -a | grep vos | cut -d' ' -f 1)
VOS_CONTAINER_NUMBER=$(docker ps -a | grep vos | cut -d' ' -f 1 | wc -l)
if [ "$VOS_CONTAINER_NUMBER" = "1" ]; then
  docker exec -it $VOS_CONTAINER_ID bash ./storage-init.sh
else
  echo "Can not determine vos container name..."
  echo "Use docker ps to determine vos container name and execute the following command manually:"
  echo "docker exec -it yourvoscontainername bash ./storage-init.sh"
fi
