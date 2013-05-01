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

//JDK imports
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

//OODT imports
import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowTaskInstance;
import org.apache.oodt.cas.workflow.structs.exceptions.WorkflowTaskInstanceException;

//ESGF imports
import esg.search.core.Record;
import esg.search.core.RecordSerializer;
import esg.search.publish.impl.solr.SolrRecordSerializer;
import esg.search.publish.opendap.Cmip5OpendapParserStrategyImpl;
import esg.search.publish.opendap.OpendapParserStrategy;
import esg.search.publish.plugins.AllPropertiesMetadataEnhancer;

/**
 * 
 * @author cinquini
 * 
 */
public class MetadataHarvesterTask implements WorkflowTaskInstance {

	@Override
	public void run(Metadata metadata, WorkflowTaskConfiguration config)
			throws WorkflowTaskInstanceException {

		System.out.println("Running task: "
				+ config.getProperties().get("name"));

		String url = config.getProperties().get("url").toString();
		System.out.println("Harvesting URL=" + url);

		// load fixed attributes from properties file
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("/esg/config/opendap.properties"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {

			// create ESGF record from OpenDAP URL
			OpendapParserStrategy parser = new Cmip5OpendapParserStrategyImpl(
					new AllPropertiesMetadataEnhancer(props));
			URI schema = new URI("cmip5");
			Record record = parser.parse(url, schema).get(0);

			// serialize record
			RecordSerializer serializer = new SolrRecordSerializer();
			String xml = serializer.serialize(record, true);
			System.out.println(xml);

		} catch (Exception e) {
			throw new WorkflowTaskInstanceException(e.getMessage());
		}

		System.out.println("Done.");

	}

}