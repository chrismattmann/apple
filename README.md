APPLE - Automatic Preconditioning and PubLishing of remote sensing data to the Earth System Grid Federation (ESGF)
=====

# Getting Started 

1. Choose the directories where you will be unpacking and installing the distribution. The following paths are only examples of possible choices.

```
export CMAC_HOME=/usr/local/cmac
export PGE_INSTALL=/usr/local/cmac/pge
```

2. `git clone https://github.com/chrismattmann/apple.git`
3. `cd apple`
4. `mvn install`
5. cp -R distribution/target/*.tar.gz $CMAC_HOME
6. cd $CMAC_HOME && tar xvzf *.tar.gz

# Install the DAAC specific PGEs (Program Generation Executables).

7. `mkdir $PGE_INSTALL && cd $PGE_INSTALL`

The $PGE_INSTALL directory will contain a sub-directory for each DAAC: GSFC, JPL and LaRC. In turn, each DAAC sub-direcory will contain the following sub-directories: config, exec, and output.

# Configuration

Edit the file $CMAC_HOME/bin/daac.sh and insert values appropriate to your system. Specifically:

1. CMAC_HOME: same value as defined above 
2. PGE_ROOT: the full path to the DAAC specific sub-directory of PGE_INSTALL. For example, PGE_ROOT=/usr/local/pge/GSFC
3. DATA_DIR: the location of the data archive, where the final products will be moved to upon ingestion. Note that this value must match what is manually entered in the TDS catalog (see later)
4. TDS_HTTPS_URL: the HTTPS-based URL of the TDS serving the data. For example, "https://test-datanode.jpl.nasa.gov/thredds"
5. TDS_USERNAME: the username used to reinitialize the TDS. For now, use "cmac". Later instructions will be provided on how to change this parameter.
6. TDS_PASSWORD: the password used to reinitialize the TDS. For now, use "cmac123". Later instructions will be provided on how to change this parameter.
7. DATA_NODE: server name and port of local ESGF Node serving the data. For example, "cmac.jpl.nasa.gov:8080"
8. INDEX_NODE: server name and port of ESGF Node where the metadata is published. For example, "test-datanode.jpl.nasa.gov"

It is recommended to move the file daac.sh to a location outside of the CMAC distribution, so it can survive updates, and symlink it in place instead:

# Configure the TDS catalogs hierarchy to reflect the current DAAC:

1. Edit the top-level TDS catalog $CMAC_HOME/tomcat/content/thredds/catalog.xml and remove references to the non-current DAAC (for example, keep the reference to gsfc.xml and remove the references to jpl.xml and larc.xml).
2. Edit the DAAC-specific catalog (for example, the file $CMAC_HOME/tomcat/content/thredds/gsfc.xml) and change the "datasetScan" directive to the DAAC-specific values:
3. Choose a proper value for the top-level collection name: for example name="AIRS data"
4. Choose a proper value for the top-level collection identifier: for example ID="gsfc.airs"
5. Change value of "location" to match the full path of the DATA_DIR environment: for example location="file:/usr/local/cmac/data/archive"

# Start CMAC pipeline
To start all the services, simply:

1. cd $CMAC_HOME/bin
2. ./oodt start
Verify that the services are running:

```
Tomcat/Solr: try the following URL: http://localhost:8080/solr
Tomcat/TDS: try the following URL: http://localhost:8080/thredds/catalog.html
Tomcat/OPSUI: try the following URL: http://localhost:8080/opsui/
File Manager: check out the standard port: netstat -na | grep -i 9000
Workflow Manager: check out the standard port: port: netstat -na | grep -i 9001
```

The following log files may be useful to debug any problems:
```
OODT: $CMAC_HOME/logs/oodt.out
Tomcat: $CMAC_HOME/tomcat/logs/catalina.out
TDS: $CMAC_HOME/tomcat/content/thredds/logs/*.log
```

# To stop all the services:
1. cd $CMAC_HOME/bin
2. ./oodt stop

# Publishing data
To fully publish data to an ESGF Index Node, you need to obtain a short-lifetime certificate for a user that is authorized to publish data into a specific collection. For now, contact Luca (luca.cinquini@jpl.nasa.gov) to obtain a temporary certificate with his identity, that is authorized to publish Obs4MIPs data to the Index node "test-datanode.jpl.nasa.gov". Save the certificate in the location ~/.esg/credentials.pem

Empty the local Solr index, which is used as back-end of the File Manager. This step is NOT necessary when running the first time, but it is needed if running a second, third, etc. time to start with a clean state:

1. `curl -s http://localhost:8080/solr/update?commit=true -H "Content-Type:text/xml" --data-binary '<delete><query>*:*</query></delete>'`
 (Note that the above command is effective only if Tomcat/Solr is running.)

Also, you must configure the DAAC-specific processing algorithm to process one or more files - this is because typically, each algorithm will (correctly) NOT re-process files that have already bee processed.

# For the GSFC DAAC:
1. `cd $PGE_ROOT/exec`
2. edit AIRX3STM.005.history
3. remove the one line containing "2013-03" (one file only)

# For the JPL DAAC:
1. `cd $PGE_ROOT/exec`
2. edit products_ccmp.txt
3. remove the line "2011" if it is there (one file only)

# Request execution of the CMAC processing workflow:
1. `cd $CMAC_HOME/workflow/bin`
2. `./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName esgf --metaData --key data_node localhost`
3. `tail -f $CMAC_HOME/logs/oodt.out`

The above instruction will request the Workflow Manager to start executing the "esgf" workflow. While the workflow executes, it is useful to monitor the oodt.out log file. For both the GSFC and JPL cases, the workflow takes approximately 5 minutes to download, process and publish one single file.
The ESGF workflow is composed of three steps, which can be verified independently:

1. Data Processing: if this step completed successfully, the following conditions should hold true:
There will be products in the local Solr index with a ProductStatus="processed". You can inspect the content of the catalog at the URL: http://localhost:8080/solr/collection1/select?q=*%3A*&wt=xml&indent=true. 
The newly generated product files should have been copied to the $DATA_DIR directory: ls -l $DATA_DIR
2. Data Validation: this step is currently only implemented as a stub, but at the end of it the status of the products in the local Solr index should change to: ProductStatus="validated"
3. Data Publishing: at the end of this step, the following should be true:
The product status in the local Solr catalog is changed to ProductStatus="published". 

Additionally, ESGF metadata should have been pushed to the ESGF Index Node, which can be inspected at the following URL: http://test-datanode.jpl.nasa.gov/esg-search/search?distrib=false&project=Obs4MIPs (note that the metadata on this server is populated within one minute of the data being published). The "timestamp" field of the newly Obs4MIPs record should be relatively current, reflecting the time when the record was last published.
 

## About This Site

This site provides software developed in response to the CMAC12: Next Generation CyberInfrastructure call. More information about the NASA solicitation that this effort responds to can be found at the
[NASA NSPIRES](http://nspires.nasaprs.com/external/solicitations/summary.do?method=init&solId=%7B074C12AB-FE57-8247-AC16-D620E429359F%7D&path=open) website.

## Contact

For additional information about this project, or for questions about the software available from this site, please contact Chris Mattmann.
