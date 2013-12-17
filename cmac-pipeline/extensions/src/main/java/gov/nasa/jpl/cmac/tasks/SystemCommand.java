package gov.nasa.jpl.cmac.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to run an external system command.
 * 
 * @author Luca Cinquini
 *
 */
public class SystemCommand {
    
    /**
     * The full command to run.
     */
    private String command;
    
    public SystemCommand(final String command) {
        this.command = command;
    }
    
    /**
     * Method that runs the system command, and returns the output line by line.
     * 
     * @param command
     * @return
     */
    public List<String> run() throws Exception {
        
        List<String> output = new ArrayList<String>();

        try {

            // run the command
            System.out.println("Running command: "+command);
            Process p = Runtime.getRuntime().exec(command);
            
            // read standard input
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            // read standard error
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // read the output from the command
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                output.add(s);
            }
            
            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                // error from the command
                System.out.println("Error detected: "+s);
                throw new Exception(s);
            }
                        
        } catch (IOException e) {
            // error running the command
            System.out.println("Error running command: "+command);
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        
        return output;

    } 
    
    /**
     * Debug method.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        SystemCommand self = new SystemCommand("ls -l");
        List<String> output = self.run();
        for (String s : output) {
            System.out.println(s);
        }
        
    }

}
