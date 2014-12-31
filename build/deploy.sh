#!/bin/bash
#
# CMAC Deployment script
#
# This script uses the environment variables set by sourcing one
# of the configure-*.sh scripts to deploy a CMAC distribution.

CWD=$(pwd)
ROOT=`dirname $CWD`

# Create the CMAC_HOME directory, if it does not exist
mkdir -p $CMAC_HOME && cd $CMAC_HOME

# Copy and extract the distribution tarball
cp $ROOT/distribution/target/cmac-pipeline-*.tar.gz . && tar xzvf cmac-pipeline-*.tar.gz

# Create the CMAC PGE_ROOT directory, if it does not exist
mkdir -p $PGE_ROOT && cd $PGE_ROOT

# Copy the DAAC-specific PGE configurations to PGE_ROOT
cp -r $ROOT/workflow/src/main/resources/etc/pge/$DAAC_ID/* .




