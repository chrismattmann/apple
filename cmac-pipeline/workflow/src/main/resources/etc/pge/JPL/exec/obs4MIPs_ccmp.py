'''
Python script to generate obs4MIPs products from PO.DAAC CCMP data.
'''
import os, sys
from string import replace
import subprocess
import shlex

INPUT_DIRECTORY = "/usr/local/pge/JPL/input"
OUTPUT_DIRECTORY = "/usr/local/pge/JPL/output"
SUBDIRECTORY = "ccmp/L3.5a/monthly/flk"
NCOPATH = "/opt/local/bin"

#VARIABLES = ["uwnd","vwnd","wspd","upstr","vpstr","nobs"]
VARIABLES = ["uwnd"]

# dictionary of global attributes to add to each file
GLOBAL_ATTRIBUTES = {
                     "project":"Obs4MIPs",
                     "time_frequency":"monthly" 
                     }

def execute(command):
    '''
    Function to execute a system command.
    If the command fails, the calling program is terminated. 
    '''
    
    print "Executing command=%s" % command

    # execute command synchronously, echo stdout and stderr, check exit status
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for line in p.stdout.readlines():
        print line,
    retval = p.wait()
    if retval!=0:
        print 'COMMAND RETURNED WITH ERROR STATUS=%s, EXITING' % retval                
        sys.exit(-1)

if __name__ == '__main__':
    
    # '/usr/local/pge/JPL/input/ccmp/L3.5a/monthly/flk'
    inDir = os.path.join(INPUT_DIRECTORY, SUBDIRECTORY)
    
    # '/usr/local/pge/JPL/output/ccmp/L3.5a/monthly/flk'
    outDir = os.path.join(OUTPUT_DIRECTORY, SUBDIRECTORY)
    
    # dictionary of monthly files grouped by variable
    monthly_files = {}
    
    # '2010', '2011',...
    for year in os.listdir(inDir):
        
        inSubDir = os.path.join(inDir, year)
        outSubDir = os.path.join(outDir, year)
        
        # loop over sub-directories i.e. years    
        for file in os.listdir(inSubDir):
            inFile = os.path.join(inSubDir, file)
            
            # create output directory if not existing already
            if not os.path.exists(outSubDir):
                os.makedirs(outSubDir)
                print "Created output directory: %s" % outSubDir
           
            for var in VARIABLES:
                
                # initialize list for this variable
                try:
                    monthly_files[var]
                except KeyError:
                    monthly_files[var] = []
                    
                outFile = os.path.join(outSubDir, replace(file, ".nc", "_%s.tmp.nc" % var, 1)) # only replace first occurrence
                outFile2 = os.path.join(outSubDir, replace(file, ".nc", "_%s.tmp2.nc" % var, 1)) # only replace first occurrence
                                
                 # extract single variable
                command = os.path.join(NCOPATH,"ncks") + " -h --overwrite -v %s %s %s" % (var, inFile, outFile)
                execute(command)
                
                # make 'time' dimension the record dimension (i.e., 'UNLIMITED')
                command = os.path.join(NCOPATH,"ncks") + " -h --overwrite --mk_rec_dmn time %s %s" % (outFile, outFile2)
                execute(command)
                os.remove(outFile) # cleanup
                monthly_files[var].append(outFile2)

                                
        # concatenate all files for each variable
        for var in VARIABLES:
            command = os.path.join(NCOPATH,"ncrcat")
            command = command + " -h --overwrite"
            sumFile = os.path.join(outSubDir,"year_%s_v11l35flk_%s.nc" % (year, var))
            
            for file in monthly_files[var]:
                command = "%s %s" % (command, file)
            command = command + " " + sumFile
            
            execute(command)
            
            # cleanup
            for file in monthly_files[var]:
                os.remove(file)
                
            # add global attrbutes
            for key, value in GLOBAL_ATTRIBUTES.items():
                command = os.path.join(NCOPATH,"ncatted")
                command = command + " -a %s,global,c,c,%s %s" % (key, value, sumFile)
                execute(command)
    