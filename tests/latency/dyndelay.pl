#!/usr/bin/perl
#
# dyndelay.pl --
#
#      Send an HTML snippet over the wire after a configurable delay.

use strict;
use warnings;

use CGI qw/:standard/;


my $time = param('time');
$time = 120 unless defined $time;

sleep($time);

print <<"ELEMENT";
Content-Type: text/html

<div style="background-color: orange; color: green;" id="lousy">
I waited $time seconds and all I got was this lousy div.
</div>
ELEMENT
