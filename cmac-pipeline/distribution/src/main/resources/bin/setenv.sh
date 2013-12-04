#!/bin/bash
#
# CMAC environment configuration for the Goddard DAAC
#

# OODT workflow manager endpoint
export WORKFLOW_URL=http://localhost:9200

# Root of CMAC software installation
export CMAC_HOME=${OODT_HOME}

#must reference the location of the local Solr home directory containing the CAS Solr index
export CATALINA_OPTS='-Dsolr.solr.home=${CMAC_HOME}/solr-home'

if [ -r "$CMAC_HOME"/bin/daac.sh ]; then
  . "$CMAC_HOME"/bin/daac.sh

