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


/**
 * Interface that resolves a single File into the enclosing Dataset.
 * 
 * @author Luca Cinquini
 *
 */
public interface FileToDatasetResolver {
    
    /**
     * Resolves a File into the enclosing Dataset.
     * 
     * @param fileId : the File product identifier
     * @return : a skeleton enclosing Dataset
     */
    Dataset resolve(String fileId);
    
    /**
     * Method to initialize the resolver from a configuration file.
     * @param config
     */
    void init(String config) throws Exception;

}
