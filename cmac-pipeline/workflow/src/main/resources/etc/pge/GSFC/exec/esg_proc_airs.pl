#!/usr/bin/perl 

=head1 NAME

esg_proc_airs.pl - script to create CMOR-compliant netCDF data for AIRS

=head1 SYNOPSIS

esg_proc_airs.pl
[B<-h> I<host_name>]
[B<-r> I<relative_root>]
[B<-s> I<output_directory_root>]
[B<-t> I<directory_for_nco_templates>]
[B<-m> I<history_file>]
[B<-c>]

=head1 ARGUMENTS

=over

=item B<-h> I<host_name>

Full host name of OPeNDAP server

=item B<-r> I<relative_root>

Optional directory relative to OPeNDAP root above datasets

=item B<-s> I<output_directory_root>

Root directory location for placing output files

=item B<-t> I<directory_for_nco_scripts>

Directory where NCO scripts are located

=item B<-m> I<history_file>

Optional history file for last-modify date/time

=item B<-l>

Optional switch for running from localhost

=head1 DESCRIPTION

This script parses through an OPeNDAP service directory, reading AIRS data
from THREDDS catalog, and creates CMOR-compliant data using NCO and scripts,
and places them at designated locations.

An optional history file recording last modified histories of files can
be read and written.

Requires cURL tool.

=head1 AUTHOR

Fan Fang, Adnet Systems, Inc.

=cut

################################################################################
# $Id: esg_proc_airs.pl,v 1.3 2013/05/04 03:44:01 ffang Exp $
# -@@@@@@ OGC, Version $Name:  $
################################################################################

use strict;
use Getopt::Std;
use File::Basename;
use File::Path;
use XML::LibXML;
use Time::Local;
use Data::UUID;
use vars qw($opt_h $opt_r $opt_s $opt_t $opt_m $opt_l);

# read and parse command line options
getopts('h:r:s:t:m:l') ;
my $hostname = $opt_h;
my $relativeRoot = '';
my $relativeRoot = $opt_r if ($opt_r);
my $stagingDir = $opt_s;
my $historyFile = $opt_m;
my $templateDir = $opt_t;
my $fromLocalhost = $opt_l;

# usage
my $usage = "Usage: " . basename($0) . " -h <fullHostName> -s <outputDir> -t <templateDir> [-r <rootDirRelativeToOpendap> -m <historyFile> -l]\n";
unless ($hostname && $stagingDir && $templateDir) {
    print STDERR $usage;
    exit (-1);
}

# define AIRS datasets and NCO processing scripts
my @datasets = ("AIRX3STM.005");
my @NCOTemplates = ("airs.nco", "geo_bnds.nco", "coord_attrs.nco",
                    "plev_attrs.nco", "global_attrs.nco");

# read history file if specified
my %lastModify;
if ($historyFile) {
    if (!(-e $historyFile)) {
        `touch $historyFile`;
    } else {
        unless ( open (HISTORY, "<$historyFile") ) {
            print STDERR "Error:  cannot open history file $historyFile to read\n";
            exit(-1);
        }
        while (<HISTORY>) {
            chomp;
            my ($filePath, $timeStamp) = split /\s+/, $_;
            $lastModify{$filePath} = $timeStamp;
        }
        close(HISTORY);
    }
}

# place NCO scripts
foreach my $script (@NCOTemplates) {
    my $copyFrom = $templateDir . "/$script";
    `cp $copyFrom .`;
    if($?) {
        print "Error copying file $copyFrom ($?)\n";
        exit(-1);
    }
}

# process
foreach my $ds (@datasets) {
    my $relPath = $relativeRoot . "/$ds";
    # do we need logic to ignore dataset here if url not valid?
    my $rootUrl = "http://" . $hostname . "/opendap/" . $relPath;
    my $execUrl;
    if ($fromLocalhost) {
        $execUrl = "http://localhost" . "/opendap/" . $relPath;
    } else {
        $execUrl = $rootUrl;
    }
    my $outDir = $stagingDir . "/$relPath";
    makeESG($rootUrl, $execUrl, $outDir, $relPath, \%lastModify);
}

# update history file if specified
if ($historyFile) {
    unless ( open (HISTORY, ">$historyFile") ) {
        print STDERR "Error:  cannot open history file $historyFile to write\n";
        exit(-1);
    }
    foreach my $file (keys %lastModify) {
        print HISTORY "$file  $lastModify{$file}\n";
    }
    close(HISTORY);
}
   
# remove copies of NCO templates
unlink @NCOTemplates;

print "Info: completed processing for ESG data\n";

exit( 0 );

sub makeESG {
    my ($url, $execUrl, $loc, $relPath, $lastModify) = @_;
    my $catalogUrl = $execUrl . "/catalog.xml";
    my $flag = 0;
    my %hash = {};
    $flag = getElement($catalogUrl, \%hash);
    #retry if BES error
    my $retry = 0;
    if($flag==-1) {
        $retry++;
        print "Info: retry $url counter $retry\n";
        makeESG($url, $execUrl, $loc, $relPath, $lastModify);
    }
    # recursive call to drill down data directory
    if ($flag==0) {
        foreach my $key (keys %hash) {
            next if ($key =~ /HASH/);
            my $nextUrl = $url . "/$key";
            my $nextExecUrl = $execUrl . "/$key";
            my $nextLoc = $loc . "/$key";
            my $nextRelPath = $relPath . "/$key";
            if (!(-e $nextLoc)) {
              `mkdir -p $nextLoc`;
              if($?) {
                  print "Error making directory $nextLoc ($?)\n";
                  exit(-1);
              }
            }
            makeESG($nextUrl, $nextExecUrl, $nextLoc, $nextRelPath, $lastModify);
        }
    } elsif ($flag==1) {
        foreach my $key (keys %hash) {
            next if ($key =~ /HASH/);
            next if ($key =~ /\.map\./);
            next if ( ($key !~ /AIRS/) );
            next if (${$hash{$key}}[1] le $lastModify->{$key});
            my $dataFile = $url . "/$key";
            my $outFile = $loc . "/ta_$key" . ".nc";
            $lastModify->{$key} = ${$hash{$key}}[1];
            writeNC($outFile, $dataFile);
        }
    }
}

sub getElement
{
    my ($url, $hashRef) = @_;
    print STDOUT "Info: contacting URL $url\n";
    my $xmlString;
    do {
        $xmlString = `curl -s $url`;
    } unless ($xmlString);
    return -1 if (($xmlString =~ /BESError/) || ($xmlString =~ /&nbsp;/));
    my $xmlParser = XML::LibXML->new();
    my $dom = $xmlParser->parse_string( $xmlString );
    my $doc = $dom->documentElement();
    my $tagName = "//thredds:catalog/thredds:dataset";
    my @nodeList = $doc->findnodes($tagName);
    my $aname = 'name';
    my $flag = 0;
    foreach my $node (@nodeList) {
        if ( $node->find('//thredds:catalogRef') ) {
            my @refNodeList = $node->findnodes('//thredds:catalogRef');
            foreach my $ref (@refNodeList) {
                $hashRef->{$ref->getAttribute( $aname )} = 'NULL';
            }
        } elsif ( $node->find('thredds:dataset') ) {
            my @dataNodeList = $node->findnodes('thredds:dataset');
            foreach my $ref (@dataNodeList) {
                my ($size) = $ref->findnodes('thredds:dataSize');
                my ($lastModified) = $ref->findnodes('thredds:date');
                push @{$hashRef->{$ref->getAttribute( $aname )}}, $size->textContent;
                push @{$hashRef->{$ref->getAttribute( $aname )}}, $lastModified->textContent;
            }
            $flag = 1;
        }
    }
    return $flag;
}

sub writeNC {
    my ($outFile, $dataFile) = @_;
    my @delete;
    # NCO fetch/subset data
    my $dataNc = $dataFile . ".nc?TempPrsLvlsU111,LandSeaMaskU273,SurfPres_AU22,SurfPres_DU138,Temperature_A_ctU54,Temperature_AU53,Temperature_DU169,Temperature_D_ctU170,LatitudeU271,LongitudeU272";
    my $fetchData = "fetch_ncks.nc";
    push @delete, $fetchData;
#    `ncks -O -v LandSeaMaskU273,SurfPres_AU22,SurfPres_DU138,Temperature_A_ctU54,Temperature_AU53,Temperature_DU169,Temperature_D_ctU170 $dataFile $fetchData`;
    `curl -s $dataNc > $fetchData`;
    if($?) {
        print "Error fetching data $dataFile ($?)\n";
        exit(-1);
    }
    # NCO process data
    my $procData = "proc_ncap2.nc";
    push @delete, $procData;
    `ncap2 -O -v -S airs.nco $fetchData $procData`;
    if($?) {
        print "Error processing data $fetchData ($?)\n";
        exit(-1);
    }
    # NCO reverse latitudes
    my $latRevData = "lat_rev_ncpdq.nc";
    push @delete, $latRevData;
    `ncpdq -O -a plev,-lat,lon $procData $latRevData`;
    if($?) {
        print "Error reversing latitudes in $procData ($?)\n";
        exit(-1);
    }
    # NCO remove relict dimensions/variables
    my $fixDimVarData = "fix_dim_var_ncks.nc";
    push @delete, $fixDimVarData;
    `ncks -O -v ta $latRevData $fixDimVarData`;
    if($?) {
        print "Error removing dimensions and variables in $latRevData ($?)\n";
        exit(-1);
    }
    # NCO add lat/lon bounds
    my $coordBoundsData = "add_bnds_ncap2.nc";
    push @delete, $coordBoundsData;
    `ncap2 -O -S geo_bnds.nco $fixDimVarData $coordBoundsData`;
    if($?) {
        print "Error adding bounds for lat/lon in $fixDimVarData ($?)\n";
        exit(-1);
    }
    # add time and bounds
    my $timeData = "addTime.nc";
    push @delete, $timeData;
    my $flag = addTime($coordBoundsData, $timeData);
    if ($flag) {
        print "Error adding time and bounds in $coordBoundsData ($?)\n";
        exit(-1);
    }
    # NCO add fixed-value attributes for coordinates
    my $fixCoordAttrsData = "coord_attrs_ncap2.nc";
    push @delete, $fixCoordAttrsData;
    `ncap2 -O -S coord_attrs.nco $timeData $fixCoordAttrsData`;
    if($?) {
        print "Error adding attributes for coordinates in $timeData ($?)\n";
        exit(-1);
    }
    # NCO fix global attributes for CMOR
    my $fixGaData = "global_attrs_ncatted.nc";
    push @delete, $fixGaData;
    my $ug = new Data::UUID;
    my $uuid = $ug->create_str();
#    my $uuid = $ug->create_from_name_str(<namespace>,$dataFile);
    my $date = `date -u +'%Y-%m-%dT%TZ'`;
    chomp($date);
    `ncatted -O -h -a ,global,d,, -a tracking_id,global,c,c,$uuid -a creation_date,global,c,c,$date $fixCoordAttrsData $fixGaData`;
    if($?) {
        print "Error fixing global attributes in $fixCoordAttrsData ($?)\n";
        exit(-1);
    }
    # add fixed CMOR global attributes for ESG
    my $cmorGaData = "global_attrs_ncap2.nc";
    push @delete, $cmorGaData;
    `ncap2 -O -h -S global_attrs.nco $fixGaData $cmorGaData`;
    if($?) {
        print "Error adding CMOR global attributes in $fixGaData ($?)\n";
        exit(-1);
    }
    # make time a record dimension
    `ncks -O -h --mk_rec_dmn time $cmorGaData $outFile`;
    if($?) {
        print "Error making record dimension in $cmorGaData ($?)\n";
        exit(-1);
    }
    # remove intermediate processed data
    foreach my $deleteFile (@delete) {
        `rm -rf $deleteFile`;
        if($?) {
            print "Failed to remove $deleteFile ($?)\n";
            exit(-1);
        }
    }
    print "Info: completed processing $dataFile\n";
}

sub addTime {
my ($infile, $outfile) = @_;
#my $outfile = shift @ARGV;
#$outfile ||= $infile;
my $sec_per_day = 24. * 60. * 60.;
# Get start date/time from core metadata
my @datetime = `ncdump -h $infile | grep -i 'RANGEBEGINNING.*VALUE'`;
my ($yyyy, $mon, $mday) = ($datetime[0] =~ m/"(\d\d\d\d)\-(\d\d)\-(\d\d)/);
my ($hh, $min, $ss) = ($datetime[1] =~ m/"(\d\d)\:(\d\d)\:(\d\d)/);
my $epoch_day = timegm(0, 0, 0, 1, 0, 100) / $sec_per_day;
my $start_days = timegm($ss, $min, $hh, $mday, $mon-1, $yyyy-1900) / $sec_per_day - $epoch_day;
$mon++;
if ($mon > 12) { $yyyy++; $mon=1; }
my $end_days = timegm($ss, $min, $hh, $mday, $mon-1, $yyyy-1900) / $sec_per_day - $epoch_day;
my $mid_month = ($start_days + $end_days) * 0.5;
my $str = "defdim(\"time\",1);time[\$time]=0.; time_bnds[\$time,\$bnds]=0.; time(0)=$mid_month; time_bnds(0,0)=$start_days; time_bnds(0,1)=$end_days; time\@units=\"days since 2000-01-01\";";
# Redefine variable 'ta'
$str .= " *fv=1.0e+20f;ta[\$time,\$plev,\$lat,\$lon]=ta;ta\@standard_name=\"air_temperature\";ta\@long_name=\"Air Temperature\";ta\@units=\"K\";ta\@missing_value=fv;ta\@cell_methods=\"time: mean\";ta\@cell_measures=\"area: areacella\";ta.change_miss(fv)";
my $cmd = "ncap2 -O -s '$str' $infile $outfile";
my $rc = system($cmd);
return 1 if ($rc != 0);
return 0;
}
