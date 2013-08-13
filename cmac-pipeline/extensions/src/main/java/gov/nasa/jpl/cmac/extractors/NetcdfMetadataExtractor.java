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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.pge.writers.PcsMetFileWriter;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 * PCS metadata extractor that parses the global attributes of a NetCDF file.
 * The extraction business logic is delegated to the undelrying UCAR NetCDF 4 Java library.
 * 
 * @author Luca Cinquini
 *
 */
public class NetcdfMetadataExtractor extends PcsMetFileWriter {
    
    private static final Logger LOG = Logger.getLogger(NetcdfMetadataExtractor.class.getName());
    
    @Override
    protected Metadata getSciPgeSpecificMetadata(File sciPgeCreatedDataFile,
                    Metadata inputMetadata, Object... customArgs) throws Exception {

        // empty metadata container
        Metadata met = new Metadata();
        
        NetcdfFile ncfile = null;
        String filename = sciPgeCreatedDataFile.getAbsolutePath();
        try {
          ncfile = NetcdfFile.open(filename);
          
          // parse global attributes, add to metadata
          for (Attribute att : ncfile.getGlobalAttributes()) {
              met.addMetadata(att.getName(), att.getStringValue());
          }
          
        } catch (IOException ioe) {
          throw new Exception("Error while trying to open " + filename, ioe);
        } finally { 
          if (null != ncfile) try {
            ncfile.close();
          } catch (IOException ioe) {
            throw new Exception("Error while trying to close " + filename, ioe);
          }
       } 

        return met;
        
    }
    
}
