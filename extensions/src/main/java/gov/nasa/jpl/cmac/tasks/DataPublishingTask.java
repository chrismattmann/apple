/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.nasa.jpl.cmac.tasks;

import gov.nasa.jpl.cmac.Constants;
import gov.nasa.jpl.cmac.utils.Exec;
import gov.nasa.jpl.cmac.utils.SolrTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oodt.cas.filemgr.catalog.solr.CompleteProduct;
import org.apache.oodt.cas.filemgr.structs.exceptions.CatalogException;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * CMAC workflow task that generates file and dataset level records,
 * and pushes them to the configured ESGF Index Node.
 * 
 * @author Luca Cinquini
 *
 */
public class DataPublishingTask implements WorkflowTaskInstance {
    
    private static final Logger LOG = Logger.getLogger(DataPublishingTask.class.getName());
    
    // XML parser
    final static SAXBuilder builder;
    
    // XML writer
    final static XMLOutputter outputter;
    
    // pattern for environmental variable substitution
    final static Pattern PATTERN_ENVIRONMENT = Pattern.compile("(\\[[^\\]]*\\])");
    
    // pattern for dynamic metadata substitution
    final static Pattern PATTERN_METADATA = Pattern.compile("(\\$[^\\$]*\\$)");
    
    static {
        
        builder = new SAXBuilder();
        builder.setFeature("http://xml.org/sax/features/namespaces",true);
        
        Format format = Format.getPrettyFormat();
        format.setLineSeparator(System.getProperty("line.separator"));
        // must omit the header line <xml....>  which causes problems when inserting records into Solr
        format.setOmitDeclaration(true); 
        outputter = new org.jdom.output.XMLOutputter(format);

    }
    
    /**
     * Main program provided to execute invocation outside of workflow manager for debugging purposes.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        DataPublishingTask task = new DataPublishingTask();
        Metadata metadata = new Metadata();
        List<String> prodIds = new ArrayList<String>();
        //prodIds.add("ta_AIRS.2013.01.01.L3.RetStd031.v5.2.2.0.G13036162933.hdf.nc");
        prodIds.add("ta_AIRS.2013.02.01.L3.RetStd028.v5.2.2.0.G13064150210.hdf.nc");
        metadata.addMetadata(Constants.PRODUCT_IDS, prodIds);
        WorkflowTaskConfiguration config = new WorkflowTaskConfiguration();
        config.addConfigProperty(Constants.SOLR_URL, "http://localhost:8080/solr");
        config.addConfigProperty(Constants.RESOLVER_CLASS, "gov.nasa.jpl.cmac.tasks.PropertiesFileToDatasetResolver");
        config.addConfigProperty(Constants.RESOLVER_CONFIG, "/usr/local/pge/GSFC/config/fileToDatasetResolver.properties");          
        String publishingCommand =  "curl --insecure --key ~/.esg/credentials.pem --cert ~/.esg/credentials.pem"
                       + " --verbose -X POST -d @XMLFILE  --header 'Content-Type:application/xml'"
                       + " https://test-datanode.jpl.nasa.gov/esg-search/ws/publish";
        publishingCommand = publishingCommand.replace("~",System.getProperty("user.home"));
        config.addConfigProperty(Constants.PUBLISHING_COMMAND, publishingCommand);
        config.addConfigProperty(Constants.TEMPLATE_DIR,"/usr/local/pge/GSFC/config/");
        config.addConfigProperty(Constants.RECORDS_DIR,"/usr/local/pge/GSFC/output/records/");
        config.addConfigProperty(Constants.TDS_PATH,"cmac");
        config.addConfigProperty(Constants.TDS_LOCATION,"/usr/local/cmac/data/archive");
        task.run(metadata, config);
        
    }
    
    @Override
    public void run(Metadata workflowMetadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
        
        // read configuration parameters
        String solrUrl = config.getProperty(Constants.SOLR_URL);
        String templateDir = config.getProperty(Constants.TEMPLATE_DIR);
        String recordsDir = config.getProperty(Constants.RECORDS_DIR);

        try {
                
            // resolve files to enclosing dataset (aka granules to collection)
            String resolverClass = config.getProperty(Constants.RESOLVER_CLASS);
            FileToDatasetResolver resolver = (FileToDatasetResolver)Class.forName(resolverClass).newInstance();
            String recolverConfig = config.getProperty(Constants.RESOLVER_CONFIG);
            resolver.init(recolverConfig);
            
            // read record templates from configuration directory
            File datasetTemplate = new File(templateDir, Constants.TEMPLATE_DATASET);
            final Document datasetDocumentTemplate = readXML(datasetTemplate);
            File fileTemplate = new File(templateDir, Constants.TEMPLATE_FILE);
            final Document fileDocumentTemplate = readXML(fileTemplate);
            
            // loop over products identified by pre-condition
            List<String> prodIds = workflowMetadata.getAllMetadata(Constants.PRODUCT_IDS);
            if (prodIds!=null) {
                for (String prodId : prodIds) {
                    
                    // retrieve CAS product
                    CompleteProduct cp = SolrTool.query(solrUrl, prodId);
                                       
                    // file/granule identifier
                    String fileId = cp.getProduct().getProductId();
                    
                    // dataset/collection identifier
                    Dataset dataset = resolver.resolve(fileId);

                    // file-level metadata
                    Metadata fileMetadata = this.makeFileMetadata(fileId, dataset.getId(), cp);
                                        
                    // generate file record from template, publish
                    this.publishRecord(fileDocumentTemplate, fileMetadata, config, new File(recordsDir, fileId+".xml"));
                    
                    // dataset-level metadata
                    Metadata datasetMetadata = this.makeDatasetMetadata(dataset, fileMetadata);

                    // generate dataset record from template, publish
                    this.publishRecord(datasetDocumentTemplate, datasetMetadata, config, new File(recordsDir, dataset.getId()+".xml"));                    
                    
                    // update file product status in FM catalog
                    SolrTool.update(solrUrl, prodId, Constants.PRODUCT_STATUS, Constants.STATUS_PUBLISHED);
                    
                } // loop over products
                
            } // productIds != null
            
        } catch(Exception e) {
            throw new WorkflowTaskInstanceException(e);
        }
        
    }
    
    /**
     * Method to generate the ESGF File-level metadata from the CAS Product metadata.
     * @param fileId
     * @param datasetId
     * @param cp
     * @return
     */
    final private Metadata makeFileMetadata(String fileId, String datasetId, CompleteProduct cp) {
        
        Metadata fileMetadata = cp.getMetadata();
        
        fileMetadata.addMetadata("id", fileId);
        fileMetadata.addMetadata("dataset_id", datasetId);
        fileMetadata.addMetadata("master_id", fileId);
        fileMetadata.addMetadata("instance_id", fileId);
        if (!fileMetadata.containsKey("title"))
            fileMetadata.addMetadata("title", cp.getProduct().getProductName());
        fileMetadata.addMetadata("description", cp.getProduct().getProductName());
                
        return fileMetadata;

    }
    
    /**
     * Method to generate the ESGF Dataset-level metadata from File-level metadata.
     * 
     * @param datasetInfo
     * @param 
     * @return
     */
    final private Metadata makeDatasetMetadata(Dataset dataset, Metadata fileMetadata) {
        
        String datasetId = dataset.getId();
        
        // FIXME: copy file-level metadata
        Metadata datasetMetadata = new Metadata();
        datasetMetadata.addMetadata(fileMetadata);
        
        // replace file values with dataset values
        datasetMetadata.replaceMetadata("id", datasetId);
        datasetMetadata.replaceMetadata("master_id", datasetId);
        datasetMetadata.replaceMetadata("instance_id", datasetId);
        datasetMetadata.replaceMetadata("title", dataset.getTitle());
        datasetMetadata.replaceMetadata("description", dataset.getDescription());
        // add timestamp (since CAS.ProductReceivedTime is not exposed)
        // use current date for dataset last update
        datasetMetadata.replaceMetadata("timestamp", Constants.SOLR_DATE_TIME_FORMATTER.format(new Date()));
        
        // FIXME: add number of files, number of aggregations to dataset metadata
        
        return datasetMetadata;

    }
    
    /**
     * Method to generate a Dataset/File record from an XML template
     * and push it to the ESGF Index Node.
     * 
     * @param template
     * @param metadata
     * @param config
     * @param recordFile
     * @throws IOException
     * @throws InterruptedException
     * @throws CatalogException
     */
    final private void publishRecord(Document template, Metadata metadata, 
                                     WorkflowTaskConfiguration config, File recordFile) 
                       throws IOException, InterruptedException, CatalogException {
        
        String command = config.getProperty(Constants.PUBLISHING_COMMAND);
        String tdsPath = config.getProperty(Constants.TDS_PATH);
        String tdsLocation = config.getProperty(Constants.TDS_LOCATION);
        
        // root element
        Element rootElement = template.getRootElement();
        Element _rootElement = new Element(rootElement.getName(), rootElement.getNamespace());
        for (Object obj : rootElement.getAttributes()) {
            Attribute att = (Attribute)obj;
            _rootElement.setAttribute(new Attribute(att.getName(), att.getValue()));
            
        }
        
        // child elements
        for (Object obj : rootElement.getChildren()) {
            
            Element childElement = (Element)obj;
            String value = childElement.getTextNormalize();
            
            // process element text: replace place-holders with values
            // from environment or CAS metadata
            String _value = replaceWithEnvironment(value);
            List<String> _values = replaceWithMetadata(_value, metadata);
            
            // write out processed values
            for (String __value : _values) {
                
                Element _childElement = (Element)childElement.clone();
                
                // special processing for URLs: must replace "tdsLocation" with "tdsPath"
                 if (_childElement.getAttributeValue("name").equals("url")) {
                     __value = __value.replace(tdsLocation, tdsPath);
                }
                
                _childElement.setText(__value);
                _rootElement.addContent(_childElement);
            }
                                   
        }
        
        // write XML document to file
        if (recordFile.exists()) recordFile.delete();
        this.writeXML(new Document(_rootElement),recordFile);
        
        // push XML record to ESGF Index Node
        String _command = command.replace("XMLFILE", recordFile.getAbsolutePath());
        int status = Exec.runSync(_command);
        if (status!=0) throw new CatalogException("Error executing command: "+_command);
        
    }
    
    /**
     * Reads XML content from the specified file.
     * @param xmlFile
     * @return
     */
    final private Document readXML(File xmlFile) throws IOException, JDOMException {
        
        LOG.log(Level.INFO,"Reading file="+xmlFile.getAbsolutePath());
        
        FileInputStream in = new FileInputStream(xmlFile);     
        Document jdoc = builder.build(in);
        in.close();
        return jdoc;
    }
    
    /**
     * Writes XML content to the specified file.
     * @param jdoc
     * @param xmlFile
     * @throws IOException
     */
    final private void writeXML(Document jdoc, File xmlFile) throws IOException {
        
        LOG.log(Level.INFO,"Writing file="+xmlFile.getAbsolutePath());
                
        java.io.FileWriter writer = new java.io.FileWriter(xmlFile);
        outputter.output(jdoc, writer);
        writer.close();
        
    }
    
    /**
     * Method to replace all occurrences of "[...]" in a string with corresponding environmental variables.
     * Works with multiple "[...]" in the same string.
     * @param value
     * @return
     */
    final private String replaceWithEnvironment(final String value) {
        String _value = value;
        Matcher matcher = PATTERN_ENVIRONMENT.matcher(value);
        while (matcher.find()) {
            String match = matcher.group(1);
            String var = match.substring(1,match.length()-1);
            String env = System.getenv(var);
            if (env!=null && env.trim().length()>0) _value = _value.replace(match, env);
        }
        return _value;
    }
    
    /**
     * Method to replace all occurrences of "$...$" in a string
     * with ALL possible values of that pattern found in the metadata.
     * Works with multiple "$...$" in the same string. 
     * May return more then one string if the metadata replacement has multiple values.
     */
    final private List<String> replaceWithMetadata(String value, Metadata metadata) {
        
        // initial input list containing only one value
        List<String> input = new ArrayList<String>();
        input.add(value);
        
        // final list of replaced values, to be populated recursively
        List<String> output = new ArrayList<String>();
        
        // start processing
        _replaceWithMetadata(input, metadata, output);
        
        return output;
        
    }
    
    /**
     * Iterative method to replace values with metadata strings.
     * @param input
     * @param metadata
     * @param output
     */
    final private void _replaceWithMetadata(List<String> input, Metadata metadata, List<String> output) {
        
        // create list of values that have been replaced this iteration
        List<String> _input = new ArrayList<String>();
        
        // loop over values
        for (String value : input) {
           boolean replaced = false;
           Matcher matcher = PATTERN_METADATA.matcher(value);
           // find all occurrences of the $...$ in the string
           while (matcher.find()) {
               String match = matcher.group(1);
               // remove the enclosing '$...$'
               String var = match.substring(1,match.length()-1);
               if (metadata.containsKey(var)) {
                   for (String val : metadata.getAllMetadata(var)) {
                       String newvalue = value.replace(match, val);
                       // add any processed value to next iteration processing list
                       _input.add( newvalue ); 
                       replaced = true;
                   }
               }
               // only replace first occurrence - replace others in following iterations
               break;
           } 
           if (!replaced) {
               // don't process this value any more
               output.add(value);
           }
        }
        
        // recurrence
        if (_input.size()>0) {
            _replaceWithMetadata(_input, metadata, output);
        }
        
    }

}
