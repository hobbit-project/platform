# Runs the initialization script for the Storage of the HOBBIT Platform.
docker exec -it vos bash
cd /opt/virtuoso-opensource/var/lib/virtuoso/db
./storage-init.sh
exit