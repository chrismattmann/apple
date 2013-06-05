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

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

public class DataPublishingTask implements WorkflowTaskInstance {
    
    private static final Logger LOG = Logger.getLogger(DataPublishingTask.class.getName());

    @Override
    public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
        
        try {
            
            String filemgrUrl = config.getProperty(Constants.FILE_MANAGER_URL);
            XmlRpcFileManagerClient fmclient = new XmlRpcFileManagerClient(new URL(filemgrUrl));
        
            // loop over products identified by pre-condition
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
                
                // 2) resolve files to enclosing datasets (aka granules to collections)
                String resolverClass = config.getProperty(Constants.RESOLVER_CLASS);
                GranuleToCollectionResolver resolver = (GranuleToCollectionResolver)Class.forName(resolverClass).newInstance();
                String recolverConfig = config.getProperty(Constants.RESOLVER_CONFIG);
                resolver.init(recolverConfig);
                List<String> uris = resolver.resolve(prodId, filePath);
                
                // 3) publish all URIs that resolved from product
                for (String tuple : uris) {
                    String[] parts = tuple.split("\\|");
                    String url = parts[0];
                    String type = parts[1];
                    
                    String command = config.getProperty(Constants.PUBLISHING_COMMAND);
                    command = command.replaceAll("CERTIFICATE", config.getProperty("certificate"));
                    command = command.replaceAll("INDEX_NODE", config.getProperty("indexNode"));
                    command = command.replaceAll("COLLECTION_URL", url);
                    command = command.replaceAll("URL_TYPE", type);
                    int exitStatus = Exec.runSync(command);
                    
                    if (exitStatus==0) { // success
                    
                        // 4) update product status in FM catalog
                        SolrTool.update(config.getProperty(Constants.SOLR_URL), prodId, Constants.PRODUCT_STATUS, Constants.STATUS_PUBLISHED);
                     
                    }
                    
                }
                
            }
        
        } catch(Exception e) {
            throw new WorkflowTaskInstanceException(e);
            
        }
        
    }

}
