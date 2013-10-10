'''
Python script to generate obs4MIPs products from PO.DAAC CCMP data.
'''
import os, sys
from string import replace
import subprocess
import ConfigParser
import datetime
import uuid
from collections import OrderedDict

# dictionary that maps the original variable name to the CMIP5 controlled vocabulary name
# uwnd -> uas, vwnd -> vas, wspd -> sfcWind
# NOTE: do NOT include variables "upstr", "vpstr"
VARIABLES = { "uwnd":"uas", "vwnd":"vas", "wspd":"sfcWind", "nobs":"nobs" }
variables = OrderedDict(sorted(VARIABLES.items(), key=lambda t: t[0]))

# global attributes to be removed
#NC_GLOBAL = ["NC_GLOBAL.Conventions", "NC_GLOBAL.title", "NC_GLOBAL.description", "NC_GLOBAL.history", "NC_GLOBAL.base_date",
#             "DODS_EXTRA.Unlimited_Dimension", "NCO"]

# file containing common (in [default] section) and variable-specific (in [variable] section) global attributes
CONFIG_FILE = "./obs4MIPs_ccmp.cfg"

# file listing years already processed, so they don't get processed again
PRODUCTS_FILE = "./products_ccmp.txt"

def main():
    '''
    Main data processing script body.
    '''
    
    # gather script arguments from environment, or use defaults
    INPUT_DIRECTORY = os.getenv('INPUT_DIRECTORY','/usr/local/pge/JPL/input')
    OUTPUT_DIRECTORY = os.getenv('OUTPUT_DIRECTORY','/usr/local/pge/JPL/output')
    SUBDIRECTORY = os.getenv('SUBDIRECTORY','ccmp/L3.5a/monthly/flk')
    NCOPATH = os.getenv('NCOPATH','/opt/local/bin')
    
    # read configuration from file co-located with this program
    thisdir = os.path.dirname(os.path.realpath(__file__))
    configFile = os.path.join(thisdir, CONFIG_FILE)
    if not os.path.exists(configFile):
        print "Configuration file %s not found, program terminated." % configFile
        sys.exit(-1)
        
    config = ConfigParser.RawConfigParser()
    config.optionxform = str # make keys case-sensitive
    try:
        config.read(configFile)
    except Exception as e:
        print "Error reading configuration file: %s" % configFile
        print e
        sys.exit(-1)

    # read products database
    years = []
    productsFile = os.path.join(thisdir, PRODUCTS_FILE)
    if os.path.exists(productsFile):
        years = [line.strip() for line in open(productsFile)]
    else:
        open(productsFile, 'a').close()
    print 'Product years already processed: %s' % years
 
    # '/usr/local/pge/JPL/input/ccmp/L3.5a/monthly/flk'
    inDir = os.path.join(INPUT_DIRECTORY, SUBDIRECTORY)
    
    # '/usr/local/pge/JPL/output/ccmp/L3.5a/monthly/flk'
    outDir = os.path.join(OUTPUT_DIRECTORY, SUBDIRECTORY)
    
    # dictionary of monthly files grouped by variable
    monthly_files = {}
    
    # '2010', '2011',...
    for year in os.listdir(inDir):
        
        # check products database
        if year in years:
            print "Year %s already processed, skipping..." % year
            
        else:
        
            inSubDir = os.path.join(inDir, year)
            outSubDir = os.path.join(outDir, year)
            
            # loop over sub-directories i.e. years    
            for file in os.listdir(inSubDir):
                inFile = os.path.join(inSubDir, file)
                # "month_20110101_v11l35flk.nc.gz.nc" --> ['month', '20110101', 'v11l35flk.nc.gz.nc']
                parts = file.split("_")
                _parts = parts[2].split(".")
                version = _parts[0]
                
                # create output directory if not existing already
                if not os.path.exists(outSubDir):
                    os.makedirs(outSubDir)
                    print "Created output directory: %s" % outSubDir
               
                for var in variables.keys():
                    
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
            for var in variables.keys():
                newvar = variables[var]
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
                    
                # rename variable (or simply remame file)
                yyyymmdd = datetime.date.today().strftime("%Y%m%D")
                newSumFile = os.path.join(outSubDir,"%s_ccmp_l35_v20131003_%s01_%s12.nc" % (newvar, year, year))
                if var != newvar:
                    command = os.path.join(NCOPATH,"ncrename")
                    command = command + " -h --overwrite -v %s,%s %s %s" % (var, newvar, sumFile, newSumFile)
                    execute(command)
                    # cleanup
                    os.remove(sumFile)
                    
                else: 
                    os.rename(sumFile, newSumFile)
                
                # remove all old global attributes
                command = os.path.join(NCOPATH, "ncatted")
                command = command + " -h -a ,global,d,, %s" % newSumFile
                execute(command)
                    
                # add new global and variable attributes from [default] and [variable] sections
                # examples:
                # mip_specs,global=CMIP5
                # calendar,time=standard
                for key, value in (dict(config.items('default') + config.items(newvar))).items():
                    
                    # special attribute processing
                    if key=='title,global':
                        value = replace(value, '$YEAR', year)
                    elif key=='creation_date,global':
                        value = replace(value, '$CREATION_DATE', datetime.datetime.now().strftime("%Y-%m-%dT%H:%M:%SZ"))
                    elif key=='tracking_id,global':
                        value = replace(value, '$UUID', uuid.uuid4().__str__()) # random UUID
                    
                    command = os.path.join(NCOPATH,"ncatted")
                    # o=overwrite existing
                    # c=character attribute type
                    command = command + " -h -a '%s',o,c,'%s' %s" % (key, value, newSumFile)
                    execute(command)

            # write out this year
            with open(productsFile, "a") as f:
                f.write("%s\n" % year)

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
    main()
    