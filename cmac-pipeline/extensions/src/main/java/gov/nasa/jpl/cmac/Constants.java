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

package gov.nasa.jpl.cmac;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Class holding CMAC package constants.
 * 
 * @author Luca Cinquini
 *
 */
public class Constants {

    public final static String FILE_MANAGER_URL = "fileManagerUrl";
    public final static String SOLR_URL = "solrUrl";
    public final static String QUERY = "query";
    public final static String PRODUCT_IDS = "productIds";
    public final static String VALIDATION_COMMAND = "validationCommand";
    public final static String PUBLISHING_COMMAND = "publishingCommand";
    public final static String RESOLVER_CLASS = "fileToDatasetResolverClass";
    public final static String RESOLVER_CONFIG = "fileToDatasetResolverConfig";
    public final static String USERNAME = "username";
    public final static String PASSWORD = "password";
    public final static String TEMPLATE_DIR = "templateDir";
    public final static String RECORDS_DIR = "recordsDir";    
    
    public final static String PRODUCT_STATUS = "ProductStatus";
    public final static String STATUS_PROCESSED = "processed";
    public final static String STATUS_VALIDATED = "validated";
    public final static String STATUS_PUBLISHED = "published";
    public final static String COMMAND = "command";
    
    public final static String TEMPLATE_DATASET = "dataset_template.xml";
    public final static String TEMPLATE_FILE = "file_template.xml";
    
    public static String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final DateFormat SOLR_DATE_TIME_FORMATTER = new SimpleDateFormat(SOLR_DATE_FORMAT);
    
    public Constants() {}

}
