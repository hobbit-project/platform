# Initialization script for the Storage of the HOBBIT Platform.
# Controls access rights for Platform-specific RDF graphs.

# Limit access rights of anonymous users to RDF graphs in the Storage
/opt/virtuoso/bin/isql 1111 dba dba exec="DB.DBA.RDF_DEFAULT_USER_PERMS_SET ('nobody', 0);"

# Create empty graphs for the Platform
/opt/virtuoso/bin/isql 1111 dba dba exec="SPARQL CREATE GRAPH <http://hobbit.org/graphs/PublicResults>;"
/opt/virtuoso/bin/isql 1111 dba dba exec="SPARQL CREATE GRAPH <http://hobbit.org/graphs/PrivateResults>;"
/opt/virtuoso/bin/isql 1111 dba dba exec="SPARQL CREATE GRAPH <http://hobbit.org/graphs/ChallengeDefinitions>;"

# Allow anonymous users to read the public graph(s)
/opt/virtuoso/bin/isql 1111 dba dba exec="DB.DBA.RDF_GRAPH_USER_PERMS_SET ('http://hobbit.org/graphs/PublicResults', 'nobody', 1);"

# Setup the HOBBIT Platform user
/opt/virtuoso/bin/isql 1111 dba dba exec="DB.DBA.USER_CREATE ('HobbitPlatform', 'Password'); GRANT SPARQL_UPDATE TO "HobbitPlatform";"

# Set the user rights for the HOBBIT Platform user
/opt/virtuoso/bin/isql 1111 dba dba exec="DB.DBA.RDF_DEFAULT_USER_PERMS_SET ('HobbitPlatform', 15);"
/opt/virtuoso/bin/isql 1111 dba dba exec="DB.DBA.RDF_GRAPH_USER_PERMS_SET ('http://hobbit.org/graphs/PublicResults', 'HobbitPlatform', 15);"

# Finally, change the 'dba' password
/opt/virtuoso/bin/isql 1111 dba dba exec="user_set_password ('dba', 'Password');"
