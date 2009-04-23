#! /usr/bin/perl

# This script transforms the plugin.xml file so that the deployment
# dependencies are uncommented and the development ones are commented

use strict;

open(IN, "plugin.xml") or die "Can't open src plugin.xml: $!";
open(OUT, ">build/tod.plugin.wst/plugin.xml") or die "Can't open dest plugin.xml: $!";

while (<IN>)
{
	s/<!-- DEV-in -->/<!-- DEV-in/;
	s/<!-- DEV-out -->/DEV-out -->/;
	s/<!-- DEPLOY-in/<!-- DEPLOY-in -->/;
	s/DEPLOY-out -->/<!-- DEPLOY-out -->/;
	print OUT;
}
