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
import gov.nasa.jpl.cmac.utils.FileManagerTool;
import gov.nasa.jpl.cmac.utils.SolrTool;

import java.util.List;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

public class DataValidationTask implements WorkflowTaskInstance {
    
    private static final Logger LOG = Logger.getLogger(DataValidationTask.class.getName());

    @Override
    public void run(Metadata metadata, WorkflowTaskConfiguration config) throws WorkflowTaskInstanceException {
        
        try {
            
            // configuration parameters
            String filemgrUrl = config.getProperty(Constants.FILE_MANAGER_URL);
            String validationCommand = config.getProperty(Constants.VALIDATION_COMMAND);
            String solrUrl = config.getProperty(Constants.SOLR_URL);
        
            // loop over products identified by pre-condition
            List<String> prodIds = metadata.getAllMetadata(Constants.PRODUCT_IDS);
            for (String prodId : prodIds) {
            
                // 1) retrieve product full path
                String filePath = FileManagerTool.getFilePath(filemgrUrl, prodId);
                 
                // 2) execute validation
                String command = validationCommand + " " + filePath;
                LOG.info("Executing command: "+command);
                SystemCommand syscom = new SystemCommand(command);
                // remove comments to run the command and inspect the output
                //List<String> output = syscom.run();
                //for (String s : output) {
                //    LOG.info(s);
                //}
                                
                // 3) update product status in FM catalog
                SolrTool.update(solrUrl, prodId, Constants.PRODUCT_STATUS, Constants.STATUS_VALIDATED);
                
            }
        
        } catch(Exception e) {
            throw new WorkflowTaskInstanceException(e);
            
        }
        
    }

}
