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

# 1. Build the CMAC-Extensions component
cd $SRC/extensions
mvn package install
# 2. Build the full CMAC distribution
cd $SRC
mvn package install

# 3. Package PGE bin scripts into final distribution.
# TODO: For a future release, this step could be folded into the
#       maven build step above.
BUILD_DIR=$SRC/distribution/target/cmac-pipeline-$DAAC_ID-$VERSION
mkdir $BUILD_DIR && cd $BUILD_DIR
cp -r $SRC/distribution/target/cmac-pipeline-*.tar.gz .
cp -r $SRC/workflow/src/main/resources/etc/pge/$DAAC_ID .
cd ../
tar czvf cmac-pipeline-$DAAC_ID-$VERSION.tar.gz cmac-pipeline-$DAAC_ID-$VERSION
