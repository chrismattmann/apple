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
package gov.nasa.jpl.cmac.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to run a system command.
 * 
 * @author Luca Cinquini
 *
 */
public class Exec {
    
    private static final Logger LOG = Logger.getLogger(Exec.class.getName());

    /**
     * Runs a system command synchronously
     * (i.e. the call does not return until the command has terminated).
     * 
     * @param command : the system command to run
     * @return : the command exit status (0 if successful, not 0 if errors)
     */
    public static int runSync(String command) throws IOException, InterruptedException {
        
        LOG.log(Level.INFO,"Executing command: "+command);
        
        String[] args = command.split("\\s+");
        ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(args));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader ibr = new BufferedReader(isr);
        String iline;
        while ((iline = ibr.readLine()) != null) {
           LOG.log(Level.INFO, iline);
        }
        
        // wait for the command to complete
        process.waitFor();
        LOG.log(Level.INFO,"Command terminated: exit value="+process.exitValue());
        
        return process.exitValue();

    }
    
    private Exec() {}
    
}
