import string
import os, sys
try:
    import json
except ImportError:
    import simplejson as json
import subprocess

### url = 'http://bubba.jpl.nasa.gov:8083'
url = 'http://cmacws.jpl.nasa.gov:8083'

# function to print out mesg
def print_mesg(mesg, keyword):
    if keyword is 'Error':
	keyword1 = 'Err'
    else:
	keyword1 = keyword
    print '----- printing ', keyword1, ' information ------'
    m_split = mesg.split(';')
    ### print m_split
    subset = [item for item in m_split if item.find(keyword) >= 0]
    for item in subset:
	print item
    print ''

# function to upload data and configuration to the checker service
def upload_to_checker_service(datafile):
    if not os.path.isfile(datafile):
	print '****** File ', datafile, ' cannot be uploaded because it does not exist.'
	return

    command = 'curl -F datafile=@' + datafile + ' ' + url + '/upload'
    cmd = command.split(' ')
    cmdstring = string.join(cmd, ' ')
    print 'cmdstring: ', cmdstring
    proc=subprocess.Popen(cmd, cwd='.', stdout=subprocess.PIPE, stderr=subprocess.PIPE, close_fds=True)
    # wait for the process to finish
    stdout_value, stderr_value = proc.communicate()
    if stderr_value.find('couldn\'t connect to host') >= 0:
	print 'error: stderr_value: ', stderr_value
    else:
	print 'uploading to checker done. here comes the result ...'
	print 'stdout_value: ', stdout_value
        """
	result_dict = json.loads(stdout_value)
	mesg = result_dict['message']  # get the message part of dict
	mesg = mesg.encode('utf-8')    # encode to get string from unicode
	### print mesg
	# print any mesg with keyword
	print_mesg(mesg, 'Error')
	print_mesg(mesg, 'Warning')
	print_mesg(mesg, 'Info')
        """


if __name__ == '__main__':

    # example 1
    ### src_data = 'vas_Amon_obs_Obs-QuikSCAT_obs_r1i1p1_199908-200910.nc'

    # example 2
    ### src_data = 'atest.nc'

    # example 3
    ### src_data = '/home/pan/projects/jpl/cmip5/cmip5_checker/trunk/service/cache/FPAR_MODIS_2000feb-2009dec.ext'

    # example 4
    #src_data = 'ta_AIRS_L3_RetStd-v5_200209-201006.nc'

    src_data = sys.argv[1]

    # upload source data
    upload_to_checker_service(src_data)
    print ''

