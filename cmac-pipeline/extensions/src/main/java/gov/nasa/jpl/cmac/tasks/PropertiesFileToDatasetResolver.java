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

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implementation of {@link FileToDatasetResolver} that is configured from a properties file
 * containing file-matching regular expressions.
 * 
 * @author Luca Cinquini
 *
 */
public class PropertiesFileToDatasetResolver implements FileToDatasetResolver {
    
    // for each pattern, stores the matching Dataset
    Map<Pattern, Dataset> patterns = new HashMap<Pattern, Dataset>();
    
    private static final Logger LOG = Logger.getLogger(PropertiesFileToDatasetResolver.class.getName());
    
    public PropertiesFileToDatasetResolver() {}
    
    /**
     * Initializes the object from a configuration properties file.
     */
    public void init(String propertiesFilePath) throws Exception {
        
        LOG.info("Initializing FileToDatasetResolver from file: "+propertiesFilePath);
        
        // load properties
        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFilePath));
        
        // compile map of regular expressions
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            String[] parts = value.split("\\|\\|");
            if (parts.length!=3) throw new Exception("Invalid property key="+key+" value="+value);
            patterns.put(Pattern.compile(key), new Dataset(parts[0], parts[1], parts[2]));
          }
        
    }
    

    /**
     * This implementation matches the file identifier 
     * to the first matching regex contained in the configuration properties file. 
     */
    public Dataset resolve(String fileId) {
                
        // loop over configured regex patterns
        for (Pattern pattern : patterns.keySet()) {
            if (pattern.matcher(fileId).matches()) {
                return patterns.get(pattern);
            }
        }
        
        // match not found
        return null;
    }

}
