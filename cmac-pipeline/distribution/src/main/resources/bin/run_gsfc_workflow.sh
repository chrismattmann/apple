#!/bin/bash
# example script to run a full GSFC workflow instance

# empty Solr database
curl -s http://localhost:8080/solr/update?commit=true -H "Content-Type:text/xml" --data-binary '<delete><query>*:*</query></delete>'

# remove last 2013-03 file from log so it can be downloaded again
cp /usr/local/pge/exec/AIRX3STM.005.history-no-2013-03 /usr/local/pge/exec/AIRX3STM.005.history

# run example workflow
cd /usr/local/cmac/workflow/bin
./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName esgf --metaData --key data_node test-datanode.jpl.nasa.gov