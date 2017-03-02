# Storage Service for the HOBBIT Platform

This project introduces the Storage Service of the HOBBIT Platform.

The Storage Service functions as a RabbitMQ server which runs in the background waiting for messages from the incoming queue. The messages are expected to be valid SPARQL queries, which the Storage Service sends to the Storage SPARQL Endpoint for execution. The results from the executed SPARQL query are serialized as JSON / JSON-LD and returned to the message issuer.

The purpose of the Storage Service is to serve as a layer between the Plaform Components and the Platform Storage.

## Notes

This is a very early version of the Storage Service, intended for the alpha version of the HOBBIT Platform.

Current assumptions:

* The message from the issuer, sent to the incoming queue, is expected to be a valid SPARQL query.
* The Storage Service currently supports all SPARQL query types: SELECT, CONSTRUCT, DESCRIBE and ASK.
* The results of the executed SPARQL query against the Platform Storage are serialized as JSON / JSON-LD (with the exception of ASK queries - their result is either `true` or `false`, as a `String`).
* If the Storage Service crashes, the queued message will not be answered and the issuer may hang waiting for a response. The issuer should implement a time-out to deal with this.

## Next steps:

* Define a Storage API - a set of rules for incoming messages and replies, to limit the possibilities for error in communication between the Storage Service and the Platform Components.
* Fix encoding issues.
* Introduce a time-out in the Storage Service when querying the SPARQL Endpoint.
* ...