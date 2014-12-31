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
package gov.nasa.jpl.cmac.conditions;

import gov.nasa.jpl.cmac.Constants;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.oodt.cas.filemgr.structs.Query;
import org.apache.oodt.cas.filemgr.tools.QueryTool;
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionInstance;

/**
 * Workflow condition that evaluates to true 
 * if the configured File Manager query returns one or more matching results. 
 * 
 * @author Luca Cinquini
 *
 */
public class FileManagerQueryCondition implements WorkflowConditionInstance {
    
    private static final Logger LOG = Logger.getLogger(FileManagerQueryCondition.class.getName());

    @Override
    public boolean evaluate(Metadata metadata, WorkflowConditionConfiguration config) {
        
        boolean ok = false;

        try {

            final String url = (String)config.getProperties().get(Constants.FILE_MANAGER_URL);
            final String query = (String)config.getProperties().get(Constants.QUERY);
            LOG.info("Querying "+url+" for: "+query);

            final QueryTool queryTool = new QueryTool(new URL(url));
            final Query casQuery = new Query();
            queryTool.generateCASQuery(casQuery, QueryTool.parseQuery(query));

            final List<String> prodIds = queryTool.query(casQuery);
            if (prodIds!=null) {
                LOG.info("Number of query results found: "+prodIds.size());
                if (prodIds.size()>0) {
                    ok = true; // condition is succesfull
                    for (Iterator i = prodIds.iterator(); i.hasNext();) {
                        String prodId = (String)i.next();
                        LOG.info("Result product id="+ prodIds);
                    }
                    metadata.addMetadata(Constants.PRODUCT_IDS, prodIds);
                }

            }

        } catch(Exception e) {
            e.printStackTrace();
        }

        return ok;

    }

}
