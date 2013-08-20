#!/usr/bin/perl
#
# Update VDD's Selenium libraries.  This can also be used to downgrade
# the libraries as well, but that's somewhat less useful.
#
#
# Copyright 2013 SugarCRM Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you
# may not use this file except in compliance with the License.  You
# may may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied.  Please see the License for the specific language governing
# permissions and limitations under the License.
#

use strict;
use warnings;

if (@ARGV != 1 or $ARGV[0] =~ m/^(?:-h|--help)$/) {
  print <<"HELP";
Update the Selenium libraries in VDD.  This script expects to execute
out of the VDD root directory and on a Unix-like system.

Usage:
   $0 <path/to/selenium>
HELP
  exit;
}

check_branch();

my $selenium = $ARGV[0];
unless (-d $selenium) {
  die "'$selenium' is not an extracted selenium directory\n";
}
$selenium =~ s|/$||;

my $cur_prefix = "$ENV{PWD}/lib/libs";
my %cur = list_jars($cur_prefix);
my $upd_prefix = "$selenium/libs";
my %upd = list_jars($upd_prefix);

my @cp = ();
my @rm = ();
my @add = ();
foreach my $u (sort(keys(%upd))) {
  if (not defined $cur{$u}) {
    print "New lib $u\n";
    push @cp, $upd{$u}[1];
    push @add, $cur_prefix . '/' . $upd{$u}[2];
  } elsif ($upd{$u}[0] ne $cur{$u}[0]) {
    print "Updated lib $u\n";
    push @rm, $cur{$u}[1];
    push @cp, $upd{$u}[1];
    push @add, $cur_prefix . '/' . $upd{$u}[2];
  }
}

my @cmds = ();
push @cmds, "git rm " . join(' ', @rm) unless @rm == 0;
push @cmds, "cp " . join(' ', @cp) . " $cur_prefix" unless @cp == 0;
push @cmds, "git add " . join(' ', @add) unless @add == 0;

foreach my $cmd (@cmds) {
  print "$cmd\n";
  system($cmd);
}

#
# Check git to make sure execution is out of VDD dev.
#
sub check_branch {
  my $branch = `git branch 2>&1`;
  if ($? or $branch !~ m/^\* dev$/msg) {
    die "Execute this script in VDD root directory on dev branch\n";
  }
}

#
# List the jar files in the specified directory.
#
sub list_jars {
  my $d = shift;
  my %files = ();

  foreach my $f ((<$d/*.jar>, <$d/../*.jar>)) {
    next if $f =~ m/srcs\.jar$/;  # skip selenium source jar
    $f =~ m|/([^/\d]+)-([^/]+)\.jar$|;
    $files{$1} = [$2, $f, "$1-$2.jar"];
  }

  return %files;
}
