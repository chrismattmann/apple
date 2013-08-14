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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.pge.writers.PcsMetFileWriter;

/**
 * PCS metadata extractor that computes the file checksum.
 * 
 * @author Luca Cinquini
 *
 */
public class ChecksumMetadataExtractor extends PcsMetFileWriter {
    
    //private static final Logger LOG = Logger.getLogger(ChecksumMetadataExtractor.class.getName());
    
    @Override
    protected Metadata getSciPgeSpecificMetadata(File sciPgeCreatedDataFile,
                    Metadata inputMetadata, Object... customArgs) throws Exception {

        // empty metadata container
        Metadata met = new Metadata();
        
        // compute MD5 checksum
        String checksum = DigestUtils.md5Hex(FileUtils.readFileToByteArray(sciPgeCreatedDataFile));
        met.addMetadata("checksum", checksum);
        
        return met;
        
    }
    
}
