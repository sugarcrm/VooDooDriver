#!/usr/bin/perl
#
# latency.pl --
#
#      Main module for generating different latency types.  Functionality
#      is provided to report what latency types are available.
#
#      The concept of the latency test module is that software (VDD)
#      can select what type of latency for the page to have, and then
#      run the test.
#
#      Latency types are selected via checkboxes at the top of the
#      main page (in the "testsel" div).  A "Run Test" button is then
#      clicked to start the test.  Depending on the test, content is
#      loaded in the "content" div, or the page is reloaded.  An
#      element or the SUGAR object can be waited on, using either
#      SugarWait or Selenium waiting or explicit waits.  In general,
#      VDD should either pass or fail gracefully in the presence of
#      the waits.
#
#


use strict;
use warnings;
use CGI qw/:standard/;


# Master list of latency tests.
# TODO: Convert each test to a standalone module.
my %actions = (load  => {name => undef, fcn  => \&page_load},
               sugar => {name => 'SUGAR load delay', fcn  => \&sugar_delay},
               slow  => {name => 'Slow page load', fcn => \&slow_load},
               dyn   => {name => 'Dynamic object delay', fcn  => \&dyn_delay});

my $out = '';

foreach my $p (param()) {
  unless (defined($actions{$p})) {
    $out .= error("Invalid param '$p'");
    last;
  }

  &{$actions{$p}->{fcn}}(param($p));
}

output($out);


#
# output --
#
#      Print the output, prepended with HTTP headers.
#
#      NOTE: Should only be called once per script invocation.
#

sub output {
  print <<"OUTPUT";
Content-Type: text/html

$_[0]
OUTPUT
  exit;
}


#
# error --
#
#      Format an error message.
#

sub error {
  return qq(<span class="error">$_[0]</span>);
}


#
# page_load --
#
#      Routine called on initial page load.
#

sub page_load {

  my @tests = ();

  foreach my $k (keys(%actions)) {
    next unless defined $actions{$k}->{name};

    push @tests, <<"RADIO";
<label><input type="radio" name="selected_test" value="$k" id="$k"/>$actions{$k}->{name}</label>
RADIO
  }

  my $testlist = join("<br/>\n  ", @tests);

  my $form = <<"FORM";
<form>
  $testlist
  <br/>
  <button type="submit" name="run" id="run" onclick="return runtests();">
    Run Tests
  </button>
</form>
FORM

  output($form);
}


#
# sugar_delay --
#
#      Delay switch of the SUGAR isloaded property.
#

sub sugar_delay {
  output('<p>SUGAR delay test.</p>');
}


#
# slow_load --
#
#      Load a page, but very slowly.  This simulates a low bandwidth link.
#

sub slow_load {
  output(<<"SLOWLOAD");
<div style="border: 1px solid; text-align: center;">
Slow-loading page Test<br/>
<a href="slowload.pl" target="_blank" id="slowload">Click Here</a>
</div>
SLOWLOAD
}


#
# dyn_delay --
#
#      Dynamic object delay.
#

sub dyn_delay {
  output(<<"DYNDELAY");
<div style="border: 1px solid; text-align: center;">
Dynamic object delay test.<br/>
<a href="dyndelay.html" target="_blank" id="dyndelay">Click Here</a>
</div>
DYNDELAY
}
