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

import java.util.List;

import org.apache.oodt.cas.filemgr.catalog.solr.DefaultProductSerializer;
import org.apache.oodt.cas.filemgr.catalog.solr.ProductSerializer;
import org.apache.oodt.cas.filemgr.catalog.solr.SolrClient;
import org.apache.oodt.cas.filemgr.structs.exceptions.CatalogException;
import org.apache.oodt.cas.metadata.Metadata;

/**
 * Utility class to interact with the Solr back-end of a File Manager catalog.
 * 
 * @author Luca Cinquini
 *
 */
public class SolrTool {
    
    /**
     * Method to update a product metadata field in the Solr catalog.
     * 
     * @param solrUrl
     * @param productId
     * @param metadataKey
     * @param metadataValue
     * @throws CatalogException
     */
    public final static void update(String solrUrl, String productId, 
                                    String metadataKey, String metadataValue) throws CatalogException {
        
        ProductSerializer ps = new DefaultProductSerializer();
        SolrClient solrClient = new SolrClient(solrUrl);
        
        Metadata metadata = new Metadata();
        metadata.addMetadata(metadataKey, metadataValue);
        
        // serialize updated metadata to Solr document(s)
        // replace=true to override the previous metadata value
        List<String> docs = ps.serialize(productId, metadata, true); 
                  
        // send documents to Solr server
        solrClient.index(docs, true, ps.getMimeType());
       
    }
    
    private SolrTool() {}

}
