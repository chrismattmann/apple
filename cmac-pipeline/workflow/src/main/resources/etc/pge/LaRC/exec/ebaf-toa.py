#!/usr/bin/env python
import cmor,cdms2
import numpy,cdutil
fid=cdms2.open('CERES_EBAF-TOA_Ed2.7_Subset_200003-201304.nc')


signX='up';
nameX='toa_sw_all_mon';
cmornameX='rsut';
nameX='toa_lw_all_mon';
cmornameX='rlut';
nameX='toa_sw_clr_mon';
cmornameX='rsutcs';
nameX='toa_lw_clr_mon';
cmornameX='rlutcs';


signX='down';
nameX='solar_mon';
cmornameX='rsdt';


var1=fid(nameX,squeeze=0)


cdutil.times.setTimeBoundsMonthly(var1)
tvals = var1.getTime()
time_values = numpy.arange(158)
time_bounds = numpy.arange(159)
lonvals=var1.getLongitude()
latvals=var1.getLatitude()
print lonvals
print latvals
time_units = "days since 2000-03-01"
def main():


    cmor.setup(inpath='Tables',
               netcdf_file_action = cmor.CMOR_REPLACE)
    cmor.dataset('OBSERVATIONS', 
                 'NASA Langley Research Center, Hampton, Va, USA', 
                 "CERES retrievals", 'gregorian',
                 institute_id = 'NASA-LaRC', model_id = 'Obs-CERES-EBAF',
                 contact="Norman Loeb, norman.g.loeb@nasa.gov <mailto:norman.g.loeb@nasa.gov>",
                 references='http://ceres.larc.nasa.gov/cmip5_data.php')


    table = 'CMIP5_Amon_obs'
# add attributes here - once they figure them out

    cmor.set_cur_dataset_attribute('instrument','CERES')
    cmor.set_cur_dataset_attribute('processing_version','Ed2.7')
    cmor.set_cur_dataset_attribute('processing_level','L3B')
    cmor.set_cur_dataset_attribute('mip_specs','CMIP5')
    cmor.set_cur_dataset_attribute('data_structure','grid')
    cmor.set_cur_dataset_attribute('source_type','satellite_retrieval')
    cmor.set_cur_dataset_attribute('source_id','CERES-EBAF')
    cmor.set_cur_dataset_attribute('realm','atmos')
    cmor.set_cur_dataset_attribute('obs_project','EBAF')
#    cmor.set_cur_dataset_attribute('experiment','')
#
    cmor.load_table(table)
    axes = [ {'table_entry': 'time',
              'units': 'months since 2000-03-01 00:00:00',
              'coord_vals' : time_values,
              'cell_bounds': time_bounds
              },
             {'table_entry': 'latitude',
              'units': 'degrees_north',
              'coord_vals': latvals,
              'cell_bounds': latvals.getBounds()},
             {'table_entry': 'longitude',
              'units': 'degrees_east',
              'coord_vals': lonvals,
              'cell_bounds': lonvals.getBounds()},
             ]


    axis_ids = list()
    for axis in axes:
        axis_id = cmor.axis(**axis)
        axis_ids.append(axis_id)


    varid = cmor.variable(table_entry=cmornameX,units='W m-2',
                          axis_ids=axis_ids,
                          positive=signX,
                          history = 'variable history',
                          missing_value = 1e20,
                          original_name=nameX
                          )




    cmor.write(varid,var1)
    cmor.close()


if __name__ == '__main__':
    main()

