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

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.catalog.solr.DefaultProductSerializer;
import org.apache.oodt.cas.filemgr.catalog.solr.ProductSerializer;
import org.apache.oodt.cas.filemgr.catalog.solr.SolrClient;
import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

public class DataValidationTask implements WorkflowTaskInstance {
    
    private static final Logger LOG = Logger.getLogger(DataValidationTask.class.getName());

    @Override
    public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
        
        try {
            
            String filemgrUrl = config.getProperty(Constants.FILE_MANAGER_URL);
            LOG.info("FILEMGR URL="+filemgrUrl);
            XmlRpcFileManagerClient fmclient = new XmlRpcFileManagerClient(new URL(filemgrUrl));
            String validationCommand = config.getProperty(Constants.VALIDATION_COMMAND);
        
            List<String> prodIds = metadata.getAllMetadata(Constants.PRODUCT_IDS);
            for (String prodId : prodIds) {
            
                // 1) retrieve product full path
                LOG.info("Retrieving product id="+prodId);
                Product prod = fmclient.getProductById(prodId);
                Metadata met = fmclient.getMetadata(prod);
                String fileLocation = met.getMetadata("FileLocation");
                String fileName = met.getMetadata("Filename");
                String filePath = fileLocation+"/"+fileName;
                LOG.info("File path="+filePath);
                
                // 2) execute validation
                String command = validationCommand + " " + filePath;
                LOG.info("Executing command: "+command);
                                
                // 3) update catalog
                ProductSerializer ps = new DefaultProductSerializer();
                String solrUrl = config.getProperty(Constants.SOLR_URL);
                SolrClient solrClient = new SolrClient(solrUrl);
                
                Metadata newMet = new Metadata();
                newMet.addMetadata("ProductStatus","validated");
                
                // serialize updated metadata to Solr document(s)
                // replace=true to override the previous metadata value
                List<String> docs = ps.serialize(prodId, newMet, true); 
                          
                // send documents to Solr server
                solrClient.index(docs, true, ps.getMimeType());
                
            }
        
        } catch(Exception e) {
            throw new WorkflowTaskInstanceException(e);
            
        }
        
    }

}
