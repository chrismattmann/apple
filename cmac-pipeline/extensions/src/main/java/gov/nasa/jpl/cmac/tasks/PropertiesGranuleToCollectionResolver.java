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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Implementation of {@link GranuleToCollectionResolver} that is configured from a property file
 * containing granule matching regular expressions.
 * 
 * @author Luca Cinquini
 *
 */
public class PropertiesGranuleToCollectionResolver implements GranuleToCollectionResolver {
    
    // for each pattern, stores all the collection URIs to be published (and their type)
    Map<Pattern, String[]> patterns = new HashMap<Pattern, String[]>();
    
    private static final Logger LOG = Logger.getLogger(PropertiesGranuleToCollectionResolver.class.getName());
    
    public PropertiesGranuleToCollectionResolver() {}
    
    /**
     * Initializes the object from a configuration properties file.
     */
    public void init(String propertiesFilePath) throws Exception {
        
        LOG.info("Initializing from file: "+propertiesFilePath);
        
        // load properties
        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFilePath));
        
        // compile map of regular expressions
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            String[] values = value.replaceAll("\\s+", "").split(",");
            patterns.put(Pattern.compile(key), values);
          }
        
    }
    

    /**
     * This implementation matches the product filepath 
     * to one or more regex contained in the configuration properties file. 
     * All matches are returned.
     */
    public List<String> resolve(String productId, String filePath) {
        
        List<String> collections = new ArrayList<String>();
        
        // loop over configured regex patterns
        for (Pattern pattern : patterns.keySet()) {
            if (pattern.matcher(filePath).matches()) {
                for (String uri : patterns.get(pattern)) {
                    collections.add(uri);
                }
            }
        }
        
        return collections;
    }

}
