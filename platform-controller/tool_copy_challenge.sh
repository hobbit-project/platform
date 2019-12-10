#!/bin/sh
# Arguments:
# - URI of the challenge to copy
# - new challenge label
# - new execution date
# - new publication date
docker run --network hobbit-core --volume ${PWD}:/data:ro -e "HOBBIT_RABBIT_HOST=rabbit" -e CHALLENGE_URI="$1" -e NEW_LABEL="$2" -e NEW_EXEC_DATE="$3" -e NEW_PUB_DATE="$4" --rm java:alpine java -cp /data/target/platform-controller.jar org.hobbit.core.run.ComponentStarter org.hobbit.controller.tools.ChallengeCopier
