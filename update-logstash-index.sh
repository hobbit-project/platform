#!/bin/bash

curl -X POST "http://localhost:9200/logstash-*/_close"
curl -X PUT "http://localhost:9200/logstash-*/_settings" -H 'Content-Type: application/json' -d' { "index" : { "sort.field" : "date", "sort.order": "desc" } } '
curl -X POST "http://localhost:9200/logstash-*/_open"

