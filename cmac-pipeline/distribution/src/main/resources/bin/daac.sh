#!/bin/bash
#
# CMAC environment configuration

# installation directory of CMAC package
# should contain tomcat, filemgr, worklow etc.
#export CMAC_HOME=/usr/local/cmac

# installation directory for DAAC specific data processing algorithm and configuration
#export PGE_ROOT=/usr/local/pge

# root directory where data products will be archived
# should match what is configured in THREDDS <datascan> directive
#export DATA_DIR=/usr/local/cmac/data/archive

# Choose an identifier for the DAAC (e.g. GSFC)
#export DAAC_ID=GSFC

# The https-based URL of the TDS serving the data
#export TDS_HTTPS_URL=https://localhost:8443/thredds

# The digest username to re-initialize the TDS, matching the value specified in tomcat-users.xml
#export TDS_USERNAME=****

# The digest password to reinitializet the TDS, matching the value specified in tomcat-users.xml
#export TDS_PASSWORD=****


# server name and port of local ESGF data node serving the data
#export DATA_NODE=localhost:8080

# server name and port of target ESGF Index Node indexing the data
#export INDEX_NODE=test-datanode.jpl.nasa.gov
