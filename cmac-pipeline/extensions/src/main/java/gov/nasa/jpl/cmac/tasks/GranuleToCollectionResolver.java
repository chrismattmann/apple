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

import java.util.List;

/**
 * Interface that resolve single granule products
 * into containing collections that need to be published.
 * 
 * @author Luca Cinquini
 *
 */
public interface GranuleToCollectionResolver {
    
    /**
     * Resolves a granule into one or more collection URIs to be published.
     * 
     * @param productId : the granule product identifier
     * @param filePath : the granule location path
     * @return : list of collections to be published encoded as "URI|type"
     *           example: "http://airsl2.ecs.nasa.gov/thredds/dodsC/TA_AIRX3STM_ESG_timeSeries.nc|OPENDAP"
     */
    List<String> resolve(String productId, String filePath);
    
    /**
     * Method to initialize the resolver from a configuration file.
     * @param config
     */
    void init(String config) throws Exception;

}
