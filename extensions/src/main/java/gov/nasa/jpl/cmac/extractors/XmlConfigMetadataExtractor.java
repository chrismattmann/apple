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
package gov.nasa.jpl.cmac.extractors;

import java.io.File;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.metadata.extractors.FilenameTokenMetExtractor;
import org.apache.oodt.cas.pge.writers.PcsMetFileWriter;

/**
 * Generic PCS metadata writer that writes metadata (name, value) pairs
 * read from an XML comfiguration file.
 * 
 * @author Rishi Verma, Luca Cinquini
 *
 */
public class XmlConfigMetadataExtractor extends PcsMetFileWriter {
    
    private static final Logger LOG = Logger.getLogger(XmlConfigMetadataExtractor.class.getName());
    
    @Override
    protected Metadata getSciPgeSpecificMetadata(File sciPgeCreatedDataFile,
                    Metadata inputMetadata, Object... customArgs) throws Exception {

        // empty metadata container
        Metadata met = new Metadata();

        // add metadata from configuration file
        String metConfFilePath = String.valueOf(customArgs[0]);
        LOG.info("Using metadata from XML configuration file: "+metConfFilePath);
        FilenameTokenMetExtractor extractor = new FilenameTokenMetExtractor();
        extractor.setConfigFile(metConfFilePath);
        met.addMetadata(extractor.extractMetadata(sciPgeCreatedDataFile));

        return met;
        
    }
    
}
