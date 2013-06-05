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
package gov.nasa.jpl.cmac.utils;

import gov.nasa.jpl.cmac.tasks.DataValidationTask;

import java.net.URL;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.structs.Product;
import org.apache.oodt.cas.filemgr.system.XmlRpcFileManagerClient;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * Utility class to interact with the CAS File Manager.
 * 
 * @author Luca Cinquini
 *
 */
public class FileManagerTool {
    
    private static final Logger LOG = Logger.getLogger(FileManagerTool.class.getName());
    
    /**
     * Retrieves the product filepath from the product metadata.
     * 
     * @param fileManagerUrl
     * @param productId
     * @return
     */
    public final static String getFilePath(String fileManagerUrl, String productId) throws Exception {
        
        LOG.info("Retrieving product id="+productId);
        XmlRpcFileManagerClient fmclient = new XmlRpcFileManagerClient(new URL(fileManagerUrl));
        Product prod = fmclient.getProductById(productId);
        Metadata met = fmclient.getMetadata(prod);
        String fileLocation = met.getMetadata("FileLocation");
        String fileName = met.getMetadata("Filename");
        String filePath = fileLocation+"/"+fileName;
        LOG.info("File path="+filePath);
        return filePath;
        
    }
    
    private FileManagerTool() {}

}
