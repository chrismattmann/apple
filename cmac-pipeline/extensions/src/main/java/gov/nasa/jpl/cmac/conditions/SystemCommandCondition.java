package gov.nasa.jpl.cmac.conditions;

import gov.nasa.jpl.cmac.Constants;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.apache.oodt.cas.metadata.Metadata;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionConfiguration;
import org.apache.oodt.cas.workflow.structs.WorkflowConditionInstance;

/***
 * Workflow condition that runs a system command, waits for it, 
 * and returns true if the command succedeed, false otherwise.
 * 
 * @author Luca Cinquini
 *
 */
public class SystemCommandCondition implements WorkflowConditionInstance {
    
    private static final Logger LOG = Logger.getLogger(SystemCommandCondition.class.getName());

    @Override
    public boolean evaluate(Metadata metadata, WorkflowConditionConfiguration config) {
        
        final String command = (String)config.getProperties().get(Constants.COMMAND);       
        LOG.info("Executing command="+command);

        // execute the command
        try {
            
            // start the command
            String[] args = command.split("\\s+");
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true); // redirect standard error to standard output
            Process process = builder.start();
            
            // read from stdin, stderr
            String line;
            InputStream in = process.getInputStream();
            BufferedInputStream buf = new BufferedInputStream(in);
            InputStreamReader inread = new InputStreamReader(buf);
            BufferedReader bufferedreader = new BufferedReader(inread);
            StringBuffer sb = new StringBuffer();
            while ((line = bufferedreader.readLine ()) != null) {
                sb.append(line);
            }
            
            // check for error
            try {
                if (process.waitFor() != 0) {
                    LOG.warning("Condition failed, error="+sb.toString());
                    return false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                LOG.warning("Condition failed, error="+sb.toString());
                return false;
                
            } finally {
                bufferedreader.close();
                inread.close();
                buf.close();
                in.close();
            }
            
            LOG.info("System command was run succesfully, condition succeeded");
            return true;
            
        } catch(Exception e) {
            LOG.warning(e.getMessage());
            return false;
        }

    }
}