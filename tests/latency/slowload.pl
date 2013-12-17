#!/usr/bin/perl
#
# slowload.pl --
#
#      Test that loads a page very slowly.  This simulates loading a
#      page over a very slow internet connection.
#

use strict;
use warnings;


my $page_header = <<"HEADER";
Content-type: text/html

<html>
<head>
<title>This will take a while...</title>
</head>
<body>
<h1>Wait for it...</h1>
HEADER

foreach my $line (split(/\n/, $page_header)) {
  slowprint("$line\n");
}

foreach my $k (0..100) {
  slowprint("<p>Line $k.</p>\n");
}

slowprint('<p><a href="#" id="end_of_test" onclick="window.close();">End of Test</a></p>');

slowprint("</body></html>\n");


sub slowprint {
  print $_[0];
  sleep(1);
}
