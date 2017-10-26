#!/bin/bash

# A BASH script which serves as a wrapper for starting Virtuoso
# and handling a SIGTERM signal as a SIGINT signal. This ensures
# that Virtuoso will execute a normal shutdown, instead of a
# quick shutdown. This solves an issue specific to the HOBBIT
# Platform.

vos_shutdown() {
   echo "Caught a SIGTERM signal. Will send a SIGINT signal to Virtuoso."
   kill -2 "$child" 2>/dev/null
}

trap vos_shutdown SIGTERM

echo "Starting Virtuoso ..."
/opt/virtuoso-opensource/bin/virtuoso-t -f &

child="$!"
wait "$child"
