#! /usr/bin/perl

# This script transforms the plugin.xml file so that the deployment
# dependencies are uncommented and the development ones are commented

use strict;

open(IN, "tod-agent-skel.cpp") or die "Can't open src tod-agent-skel.cpp: $!";
open(OUT, ">obf/tod-agent-skel.cpp") or die "Can't open dest obf/agentcpp: $!";

while (<IN>)
{
	s/Standard version/------- Obfuscated version/;
	s/tod\/agent/tod\/agentX/;
	s/tod\/core/tod\/coreX/;
	s/tod\/tools/tod\/toolsX/;
	s/zz\/utils/zz\/utilsX/;
	s/tod_agent/tod_agentX/;

	print OUT;
}
