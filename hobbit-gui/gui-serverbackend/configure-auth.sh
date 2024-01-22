#!/bin/bash
# Runs the GUI serverbackend of the HOBBIT Platform.
USE_KEYCLOAK="${USE_UI_AUTH:-true}"

if [ "${USE_KEYCLOAK}" == "false" ]; then
    echo "Replacing web.xml with web-without-ui-auth.xml"
    cp /var/lib/jetty/webapps/ROOT/WEB-INF/web-without-ui-auth.xml /var/lib/jetty/webapps/ROOT/WEB-INF/web.xml
fi

