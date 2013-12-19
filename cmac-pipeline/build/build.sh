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
VERSION=0.2.0-beta

# 1. Install the ESG-Search Jar
mvn install:install-file \
  -DgroupId=esgf.org \
  -DartifactId=esg-search \
  -Dversion=3.7.7 \
  -Dpackaging=jar \
  -Dfile=$SRC/distribution/src/main/resources/esgf/esg-search-3.7.7.jar

# 2. Build the CMAC-Extensions component
cd $SRC/extensions
mvn package install

# 3. Build the full CMAC distribution
cd $SRC
mvn package install

# 4. Package PGE bin scripts into final distribution.
# TODO: For a future release, this step could be folded into the
#       maven build step above.
BUILD_DIR=$SRC/distribution/target/cmac-pipeline-$VERSION
mkdir $BUILD_DIR && cd $BUILD_DIR
cp -r $SRC/distribution/target/cmac-pipeline-*.tar.gz .
cp -r $SRC/workflow/src/main/resources/etc/pge/* .
cd ../
tar czvf cmac-pipeline-$VERSION.tar.gz cmac-pipeline-$VERSION
