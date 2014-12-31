#!/bin/bash
#
# CMAC environment configuration for the Goddard DAAC
#
export DAAC_ID=GSFC

# Root of CMAC software installation
export CMAC_HOME=/usr/local/cmac
# Reference to root directory containing PGE executables
export PGE_ROOT=${CMAC_HOME}/pge
# OODT resource manager installation directory
export RESMGR_HOME=${CMAC_HOME}/resmgr
# OODT workflow manager installation directory
export WORKFLOW_HOME=${CMAC_HOME}/workflow
# OODT file manager installation directory
export FILEMGR_HOME=${CMAC_HOME}/filemgr
# OODT crawler installation directory
export CRAWLER_HOME=${CMAC_HOME}/crawler



# OODT workflow manager endpoint
export WORKFLOW_URL=http://localhost:9200
# OODT file manager endpoint
export FILEMGR_URL=http://localhost:9000



# The https-based URL of the TDS serving the data
export TDS_HTTPS_URL=https://localhost:8443/thredds
# The digest username to re-initialize the TDS, matching the value specified in tomcat-users.xml
export TDS_USERNAME=****
# The digest password to reinitializet the TDS, matching the value specified in tomcat-users.xml
export TDS_PASSWORD=****


# server name and port of local ESGF data node serving the data
export DATA_NODE=localhost:8080
# server name and port of target ESGF Index Node indexing the data
export INDEX_NODE=test-datanode.jpl.nasa.gov
#must reference the location of the local Solr home directory containing the CAS Solr index
export CATALINA_OPTS='-Dsolr.solr.home=${CMAC_HOME}/solr-home'