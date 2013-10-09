# - 
# CMAC Project Build Script
#
# Build a distribution from a fresh Subversion checkout
#
#
# Software Prerequisites:
#   Maven2 (`mvn`)
#
# Steps:
# 0. Environment
CWD=$(pwd)
SRC=`dirname $CWD`
# 1. Install the ESG-Search Jar
mvn install:install-file -DgroupId=esgf.org -DartifactId=esg-search -Dversion=3.7.7 -Dpackaging=jar -Dfile=$SRC/libs/esg-search-3.7.7.jar
# 2. Build the CMAC-Extensions component
cd $SRC/extensions
mvn package install
# 3. Build the full CMAC distribution
cd $SRC
mvn package install