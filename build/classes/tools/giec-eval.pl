#! /usr/bin/perl 
use strict;

use Getopt::Long; 

##########################################################################################
#											 #
#	       Pre-process : dealing with options, reading dictionary			 #
#											 #
##########################################################################################

##########################
#Global technic variables#
##########################
my $script = "giec-eval.pl"; 
my $help = << "end_of_help;";

Usage: giec-eval.pl -k <keys-file> -p <predictions-file> -d <dictionary-file> [-v|-s:|-w|-b:]
Options :
	-h	help
	-k	path of the keys-file
	-p	path of the predictions-file
	-d	path of the dictionary
	-v	output will give counts for each sentence (scoring details)
	-s	number of significant digits for IE measures (default: 2)
	-w	removing warnings about missing or spurious IDs
	-b	value of beta for f-measure computing (default: 1)
please questioning and reporting bugs at lll05\@jouy.inra.fr

end_of_help;
my %options;

#############################
#Global conceptual variables#
#############################
my @ListOfFormsAndSyns;
my @ListOfCanonicalForms;
my @Responses;# Responses = Predictions
my @Keys;
my %ResponseBlock;
my $DigitsNumber=2;
my $beta=1;


#Dealing with options
GetOptions(\%options, 
	   "h",
           "k=s",    
	   "p=s",
	   "d=s",
	   "v",
	   "s=i",
	   "w",
	   "b=f"
           ); 

if (defined ($options{'h'})) {print $help;exit;};

if (not (defined ($options{'k'}))) {
		print "Caution : please give \"k\" option\n";
		print $help;
		exit;
		};


if (not (defined ($options{'p'}))) {
		print "Caution : please give \"p\" option\n";
		print $help;
		exit;
		};

if (not (defined ($options{'d'}))) {
		print "Caution : please give \"d\" option\n";
		print $help;
		exit;
		};

if (defined ($options{'s'})) {
	if (($options{'s'}>8) or ($options{'s'}<1)) {die "Error : between 1 and 8 significant digits ! \n"}
		else {$DigitsNumber=$options{'s'}};
	};

if (defined ($options{'b'})) {
	if (($options{'b'}>10) or ($options{'b'}<0)) {die "Error : beta should be between 0 and 10 ! \n"}
		else {$beta=$options{'b'}};
	};

if ($#ARGV>=0) {die "Error : $script takes no arguments ! \n"};

#Reading Dictionary
open(DIC,$options{'d'}) or die "$script: could not open file ($options{'d'}) ! \n";
my @dicLines=<DIC>;
close DIC;
foreach my $iRD (0..$#dicLines) {
	my @listOfOneFormAndSyns;
	if ($dicLines[$iRD]=~ s/^([^%|\r|\n|\t]+)[\r|\n|\t]//) {
		push(@ListOfCanonicalForms,$1);
		push(@listOfOneFormAndSyns,$1);
		while ($dicLines[$iRD]=~ s/^([^%|\r|\n|\t]+)[\r|\n|\t]//) {
			push(@listOfOneFormAndSyns,$1);
			};
		push (@ListOfFormsAndSyns,\@listOfOneFormAndSyns);
		};
	};


##########################################################################################
#											 #
#	       			       Main program					 #
#											 #
##########################################################################################

@Responses=readResponses($options{'p'});
@Keys=readKeys($options{'k'});
%ResponseBlock=checkResponses(\@Responses,\@Keys);
printScores(\%ResponseBlock);

##########################################################################################
#											 #
#	 		 Part I : Reading & checking keys				 #
#											 #
##########################################################################################

sub readKeys {
#read the key file, extract the IDs, targets, agents and interactions and check them

	my $nameFile=$_[0];
	open (FILE,$nameFile) or die "$script: could not open file ($nameFile) ! \n"; 
	my @lines=<FILE>;#array of lines from response file
	close FILE;

	my @list;#list of keys, one per ID
	my $irK=0;
	while (not ($irK>$#lines)) {
	
		# jumping blank lines
		while (not ($lines[$irK] =~ /^ID/)) {
			$irK++;
			if ($irK>$#lines) {last;}
			};
		if ($irK>$#lines) {last;}
		
		my %key;#key is a hash with 1 ID, 1 list of agents, 1 list of targets, and 1 list of interactions
	
		# reading ID
		if (not ($lines[$irK] =~ /^ID\t([0-9|-]+)[\r|\n]/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
			else {$key{'ID'}=$1};
		
		# jumping to next line
		$irK++;
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};

		# jumping sentence
		if (not ($lines[$irK] =~ /^sentence\t/)) {die "$script: format error \($nameFile line ".($irK+1)."\), sentence waited ! \n";} else {$irK++;};
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};

		# reading words and saving words
		my @wordsDeclared;
		if (not ($lines[$irK] =~ /^words\t/)) {die "$script: format error \($nameFile line ".($irK+1)."\), words waited ! \n";} else {
			while ($lines[$irK]=~ s/^words\t+word\([0-9]+,\'([^\']+)\'[^\t|\r|\n]+[\t|\r|\n]/words\t/){
				push (@wordsDeclared,$1);
				};
			$irK++;};
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};

		# jumping lemmas (optional)
		if ($lines[$irK] =~ /^lemmas\t/) {$irK++;};
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};

		# jumping syntactic_relations (optional)
		if ($lines[$irK] =~ /^syntactic_relations\t/) {$irK++;};
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};

		#reading agents IDs, finding the correspondant word and taking the canonical form
		if (not ($lines[$irK] =~ /^agents/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
			else {
			my @listAgents;
			while ($lines[$irK] =~ s/^agents\t([^\t|\r|\n]+)[\t|\r|\n]/agents\t/) {
				if (not ($1=~ /^agent\(([0-9]+)\)/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
					else {  my $canonicalForm=canonicalForm($wordsDeclared[$1]);
						if ($canonicalForm eq "") {die "Error : in $nameFile, for ID ".$key{'ID'}.", agent ".$wordsDeclared[$1]." is not referenced in the dictionary\n"} else {
							push (@listAgents,canonicalForm($wordsDeclared[$1]));
							};
						}
				};
			$key{'agents'}=\@listAgents;
			};
	
		# jumping to next line
		$irK++;
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};
	
		#reading targets, finding the correspondant word and taking the canonical form
		if (not ($lines[$irK] =~ /^targets/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
			else {
			my @listTargets;
			while ($lines[$irK] =~ s/^targets\t([^\t|\r|\n]+)[\t|\r|\n]/targets\t/) {
				if (not ($1=~ /^target\(([0-9]+)\)/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
					else {  my $canonicalForm=canonicalForm($wordsDeclared[$1]);
						if ($canonicalForm eq "") {die "Error : in $nameFile, for ID ".$key{'ID'}.", target ".$wordsDeclared[$1]." is not referenced in the dictionary\n"} else {
							push (@listTargets,canonicalForm($wordsDeclared[$1]));
							};
						}
				};
			$key{'targets'}=\@listTargets;
			};
	
		# jumping to next line
		$irK++;
		if ($irK>$#lines) {die "$script: format error \($nameFile line ".($irK+1)."\)! \n";};
	
		#reading interactions, checking if agents and target are declared
		if (not ($lines[$irK] =~ /^genic_interactions/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
			else {
			my @listInteractions;
			while ($lines[$irK] =~ s/^genic_interactions\t([^\t|\r|\n]+)[\t|\r|\n]/genic_interactions\t/) {
				if (not ($1=~ /^genic_interaction\(([0-9]+),([0-9]+)\)$/)) {die "$script: format error \($nameFile line ".($irK+1)."\) ! \n";}
				else {
					my %interaction;
					$interaction{'agent'}=canonicalForm($wordsDeclared[$1]);
					if ($interaction{'agent'} eq "") {die "Error : in $nameFile, for ID ".$key{'ID'}.", the ID $1 of an agent in an interaction is wrong or not declared\n"};
					$interaction{'target'}=canonicalForm($wordsDeclared[$2]);
					if ($interaction{'target'} eq "") {die "Error : in $nameFile, for ID ".$key{'ID'}.", the ID $2 of a target in an interaction is wrong ot not declared\n"};
					push(@listInteractions,\%interaction);
					};
				};
			$key{'interactions'}=\@listInteractions;
			};
	
		#verifying key's coherence
		verifyCoherence(\%key,$nameFile);

		#saving key in list
		push (@list,\%key);
	
		};
	return @list;
	};


sub verifyCoherence{
#verify the coherence of the fields for 1 ID, and dying with a message if an error is found

	my %responsevC=%{$_[0]};
	my $nameFilevC=$_[1];

	#making a list of all entities mentionned in the interactions
	my @listOfAgentsUsedInInteractions;
	my @listOfTargetsUsedInInteractions;
 	foreach my $interactionvC (@{$responsevC{'interactions'}}) {
		
		if (not (myInclude($interactionvC->{'agent'},\@listOfAgentsUsedInInteractions))) {
			push (@listOfAgentsUsedInInteractions,$interactionvC->{'agent'});
			};
		if (not (myInclude($interactionvC->{'target'},\@listOfTargetsUsedInInteractions))) {
			push (@listOfTargetsUsedInInteractions,$interactionvC->{'target'});
			};
		};

	#verifying that each agent is a canonical form, and is used in a interaction
	my @listOfAgentsDeclared;
	foreach my $agentDeclared (@{$responsevC{'agents'}}) {
		#verify that the agent is used in an interaction
		if (not (myInclude($agentDeclared,\@listOfAgentsUsedInInteractions))) {
			die "Error in $nameFilevC : for ID $responsevC{'ID'}, agent $agentDeclared seems to not interact\n";
			};
		#verify that the agent is in canonical form
		if (not (myInclude($agentDeclared,\@ListOfCanonicalForms))) {
			die "Error in $nameFilevC : for ID $responsevC{'ID'}, agent $agentDeclared is not recognized as a valid canonical form\n";
			};
		#making the list of agents or targets declared
		if (not (myInclude($agentDeclared,\@listOfAgentsDeclared))) {
			push (@listOfAgentsDeclared,$agentDeclared);
			};	
		};

	#verifying that each target is a canonical form, and is used in a interaction
	my @listOfTargetsDeclared;
	foreach my $targetDeclared (@{$responsevC{'targets'}}) {	
		#verify that the target is used in an interaction
		if (not (myInclude($targetDeclared,\@listOfTargetsUsedInInteractions))) {
			die "Error in $nameFilevC : for ID $responsevC{'ID'}, target $targetDeclared seems to not interact\n";
			};
		#verify that the target is in canonical form
		if (not (myInclude($targetDeclared,\@ListOfCanonicalForms))) {
			die "Error in $nameFilevC : for ID $responsevC{'ID'}, target $targetDeclared is not recognized as a valid canonical form\n";
			};
		#making the list of agents or targets declared
		if (not (myInclude($targetDeclared,\@listOfTargetsDeclared))) {
			push (@listOfTargetsDeclared,$targetDeclared);
			};	
		};

	#verifying that agents and targets are coherent with interactions
	foreach my $agentUsed (@listOfAgentsUsedInInteractions) {
		if (not (myInclude($agentUsed,\@listOfAgentsDeclared))) {
			die "Error in $nameFilevC : for ID $responsevC{'ID'}, agent $agentUsed interacts but is not declared\n";
			};
		};
	foreach my $targetUsed (@listOfTargetsUsedInInteractions) {
		if (not (myInclude($targetUsed,\@listOfTargetsDeclared))) {
			die "Error in $nameFilevC : for ID $responsevC{'ID'}, target $targetUsed interacts but is not declared\n";
			};
		};
	}

##########################################################################################
#											 #
#	 		 Part II : Reading & checking responses				 #
#											 #
##########################################################################################


sub readResponses {
#read the responses file, extract the IDs, targets, agents and interactions and check them

	my $nameFile=$_[0];
	open (FILE,$nameFile) or die "$script: could not open file ($nameFile) ! \n"; 
	my @lines=<FILE>;#array of lines from response file
	close FILE;

	my @list;#list of responses, one per ID
	my $irR=0;
	while (not ($irR>$#lines)) {
	
		# jumping blank lines
		while (not ($lines[$irR] =~ /^ID/)) {
			$irR++;
			if ($irR>$#lines) {last;}
			};
		if ($irR>$#lines) {last;}
		
		my %response;#response is a hash with 1 ID, 1 list of agents, 1 list of targets, and 1 list of interactions
	
		# reading ID
		if (not ($lines[$irR] =~ /^ID\t([0-9|-]+)[\r|\n]/)) {die "$script: format error0 \($nameFile line ".($irR+1)."\) ! \n";}
			else {$response{'ID'}=$1};
		
		# jumping to next line
		$irR++;
		if ($irR>$#lines) {die "$script: format error1 \($nameFile line ".($irR+1)."\)! \n";};

		#reading agents
		if (not ($lines[$irR] =~ /^agents/)) {die "$script: format error2 \($nameFile line ".($irR+1)."\) ! \n";}
			else {
			my @listAgents;
			while ($lines[$irR] =~ s/^agents\t([^\t|\r|\n]+)[\t|\r|\n]/agents\t/) {
				if (not ($1=~ /^agent\(\'(.+)\'\)/)) {die "$script: format error3 \($nameFile line ".($irR+1)."\) ! \n";}
					else {push (@listAgents,$1);}
				};
			$response{'agents'}=\@listAgents;
			};
	
		# jumping to next line
		$irR++;
		if ($irR>$#lines) {die "$script: format error4 \($nameFile line ".($irR+1)."\)! \n";};
	
		#reading targets
		if (not ($lines[$irR] =~ /^targets/)) {die "$script: format error5 \($nameFile line ".($irR+1)."\) ! \n";}
			else {
			my @listTargets;
			while ($lines[$irR] =~ s/^targets\t([^\t|\r|\n]+)[\t|\r|\n]/targets\t/) {
				if (not ($1=~ /^target\(\'(.+)\'\)/)) {die "$script: format error6 \($nameFile line ".($irR+1)."\) ! \n";}
					else {push (@listTargets,$1);}
				};
			$response{'targets'}=\@listTargets;
			};
	
		# jumping to next line
		$irR++;
		if ($irR>$#lines) {die "$script: format error \($nameFile line ".($irR+1)."\)! \n";};
	
		#reading interactions
		if (not ($lines[$irR] =~ /^genic_interactions/)) {die "$script: format8 error \($nameFile line ".($irR+1)."\) ! \n";}
			else {
			my @listInteractions;
			while ($lines[$irR] =~ s/^genic_interactions\t([^\t|\r|\n]+)[\t|\r|\n]/genic_interactions\t/) {
				if (not ($1=~ /^genic_interaction\(\'([^,]+)\',\'(.+)\'\)$/)) {die "$script: format9 error \($nameFile line ".($irR+1)."\) ! \n";}
				else {
					my %interaction;
					$interaction{'agent'}=$1;
					$interaction{'target'}=$2;
					push(@listInteractions,\%interaction);
					};
				};
			$response{'interactions'}=\@listInteractions;
			};
	
		#verifying response's coherence
		verifyCoherence(\%response,$nameFile);

		#saving response in list
		push (@list,\%response);
	
		};
	return @list;
	};


##########################################################################################
#											 #
#	 		 Part III : Comparing and Scoring				 #
#											 #
##########################################################################################


sub checkResponses{
#return a list of hashes (one per aligned ID) of COR, MIS & SPU

	my @responsescR=@{$_[0]};
	my @keyscR=@{$_[1]};

	#I use a list of boolean corresponding to ID responded, each initialized to 0. I check them to 1 when
	#I met the ID responded, in order to know at the end which aren't be treated (for warnings)
	my @listOfResponsesCheck;
	foreach my $icR (0..$#responsescR) {$listOfResponsesCheck[$icR]=0};
	#the same for keys
	my @listOfKeysCheck;
	my @listOfScores;#list of hashes with 'COR','MIS' and 'SPU', one hash per ID
	my %responseBlockcR;#hash with 'ID' the treated ID and 'listOfScores' the list of scores

	#for each ID in Keys...
	foreach my $icR (0..$#keyscR) {

		#finding the correspondant ID in responses
		my $jcR=-1;
		my $IDFound=0;
		while (($jcR<$#responsescR) and (not $IDFound)){
			$jcR++;
			if ($responsescR[$jcR]{'ID'} eq $keyscR[$icR]{'ID'}) {$IDFound=1};
			};

		#dealing with no correspondant ID
		if ($IDFound==0) {$listOfKeysCheck[$icR]=0;next}#it will be a MIS

		#dealing with normal case
			#key and response are met, so they're checked
			else {$listOfKeysCheck[$icR]=1;$listOfResponsesCheck[$jcR]=1;};

		#copying keys and responses, creating ckeck files
		my @listOfKeysInteractions=@{$keyscR[$icR]{'interactions'}};
		#I use the same strategy for the interactions : a check list initialized to 0, and checked to 1 when
		#the interaction is treated. So I know which interactions aren't treated (which will be MIS)
		my @listOfKeysInteractionsCheck;
		my @listOfResponsesInteractions=@{$responsescR[$jcR]{'interactions'}};
		#the same strategy for responses (it will be SPU)
		my @listOfResponsesInteractionsCheck;
		foreach my $jcR (0..$#listOfResponsesInteractions) {$listOfResponsesInteractionsCheck[$jcR]=0};

		#for each interaction in key, finding the correspondant interaction and check them
		foreach my $jcR (0..$#listOfKeysInteractions) {
			my $kcR=-1;
			my $interactionFound=0;
			#for each interaction in response...
			while (($kcR<$#listOfResponsesInteractions) and (not $interactionFound)){
				$kcR++;
				#condition is : having same targets and agents, and response not already checked
				if ((($listOfResponsesInteractions[$kcR]{'agent'} eq $listOfKeysInteractions[$jcR]{'agent'})
				 and ($listOfResponsesInteractions[$kcR]{'target'} eq $listOfKeysInteractions[$jcR]{'target'}))
				and ($listOfResponsesInteractionsCheck[$kcR]==0))
					 {$interactionFound=1};
				};
			#dealing with no correspondant interaction
			if ($interactionFound==0) {$listOfKeysInteractionsCheck[$jcR]=0;next}

			#dealing with normal case
				else {$listOfKeysInteractionsCheck[$jcR]=1;$listOfResponsesInteractionsCheck[$kcR]=1;};
			};

		#counting how many are checked or not in both camp
		my %scoreBlock;
		$scoreBlock{'ID'}=$keyscR[$icR]{'ID'};
		#counting corrects, ie how many keys checked 1
		#counting so missings, ie how many keys checked 0
		my $CORcount=0;
		my $MIScount=0;
		foreach my $jcR (0..$#listOfKeysInteractionsCheck) {
			if ($listOfKeysInteractionsCheck[$jcR]==0) {$MIScount++};
			if ($listOfKeysInteractionsCheck[$jcR]==1) {$CORcount++};
			};
		#counting spurious, ie how many responses checked 0
		my $SPUcount=0;
		foreach my $jcR (0..$#listOfResponsesInteractionsCheck) {
			if ($listOfResponsesInteractionsCheck[$jcR]==0) {$SPUcount++};
			};
		$scoreBlock{'COR'}=$CORcount;
		$scoreBlock{'MIS'}=$MIScount;
		$scoreBlock{'SPU'}=$SPUcount;
		push (@listOfScores,\%scoreBlock);
		};

	#returning values
	my @listOfIDMissing;
	foreach my $icR (0..$#listOfKeysCheck) {
		if ($listOfKeysCheck[$icR]==0) {push @listOfIDMissing,$keyscR[$icR]{'ID'}};
		};
	$responseBlockcR{'listOfIDMissing'}=\@listOfIDMissing;
	my @listOfIDSpurious;
	foreach my $icR (0..$#listOfResponsesCheck) {
		if ($listOfResponsesCheck[$icR]==0) {push @listOfIDSpurious,$responsescR[$icR]{'ID'}};
		};
	$responseBlockcR{'listOfIDSpurious'}=\@listOfIDSpurious;
	$responseBlockcR{'listOfScores'}=\@listOfScores;
	return %responseBlockcR;
	};


sub printScores{

	my %responseBlockpS=%{$_[0]};

	my @listOfIDMissing=@{$responseBlockpS{'listOfIDMissing'}};
	my @listOfIDSpurious=@{$responseBlockpS{'listOfIDSpurious'}};
	my @listOfScores=@{$responseBlockpS{'listOfScores'}};
	my $COR=0;
	my $MIS=0;
	my $SPU=0;
	my $PRE;
	my $REC;
	my $FM;

	#printing 1 blank line
	print "\n";

	#printing scoring details
	if (defined ($options{'v'})) {
		print "Scoring details \(verbose option\)\n";
		print "ID\t\tCOR\tMIS\tSPU\n";
		foreach my $ipS (0..$#listOfScores) {print "$listOfScores[$ipS]{'ID'}\t$listOfScores[$ipS]{'COR'}\t$listOfScores[$ipS]{'MIS'}\t$listOfScores[$ipS]{'SPU'}\n";};
		print "\n";
		};	

	#warning for missing IDs
	if ((not(defined($options{'w'}))) and ($#listOfIDMissing>=0)) {
		print "WARNING : ".($#listOfIDMissing+1)." ID seems to have been not treated : ";
		foreach my $jpS (0..$#listOfIDMissing) {
			if (not ($jpS==0)) {print ","};
			print $listOfIDMissing[$jpS];
			};
		print "\n\n";
		};

	#warning for spurious IDs
	if ((not(defined($options{'w'}))) and ($#listOfIDSpurious>=0)) {
		print "WARNING : ".($#listOfIDSpurious+1)." ID seems to have been treated for free : ";
		foreach my $jpS (0..$#listOfIDSpurious) {
			if (not ($jpS==0)) {print ","};
			print $listOfIDSpurious[$jpS];
			};
		print "\n\n";
		};

	#printing scores
	print "Scoring summary\n";
	#compute them
	foreach my $ipS (0..$#listOfScores) {
		$COR=$COR+$listOfScores[$ipS]{'COR'};
		$MIS=$MIS+$listOfScores[$ipS]{'MIS'};
		$SPU=$SPU+$listOfScores[$ipS]{'SPU'};
		};
	$PRE=$COR/($COR+$SPU);
	$REC=$COR/($COR+$MIS);
	$FM=($beta**2 + 1)*$PRE*$REC / ((($beta**2)*$PRE) + $REC);
	#rounding them
	$PRE=(int($PRE*(10**$DigitsNumber)))/(10**$DigitsNumber);
	$REC=(int($REC*(10**$DigitsNumber)))/(10**$DigitsNumber);
	$FM=(int($FM*(10**$DigitsNumber)))/(10**$DigitsNumber);
	#printing them
	print "COR:$COR\t";
	print "MIS:$MIS\t";
	print "SPU:$SPU\n";
	print "PRE:$PRE\t";
	print "REC:$REC\t";
	print "FM:$FM\n";
	print "\n";
	};


##########################################################################################
#											 #
#		 		     Little scripts					 #
#											 #
##########################################################################################

sub canonicalForm{
#return the canonical form of the entity (1st parameter) if it exists
	my $entitycF=$_[0];
	my $returnedEntity="";

	#trying if it's is a canonical form
	if (myInclude($entitycF,\@ListOfCanonicalForms)) {$returnedEntity=$entitycF}

		else{

	#trying if it's a synonym
		foreach my $icF (0..$#ListOfCanonicalForms) {
			my @listOfOneFormAndSyns=@{$ListOfFormsAndSyns[$icF]};
			foreach my $jcF (1..$#listOfOneFormAndSyns) {
				if ($entitycF eq $listOfOneFormAndSyns[$jcF]) {
					$returnedEntity=$listOfOneFormAndSyns[0];last;
					};
				};
			if (not ($returnedEntity eq "")) {last;};
			};
		};

	return $returnedEntity;
	};

sub myInclude{
#return 1 if the element (1st parameter) is an element of the array (2nd parameter), 0 if not

	my $element=$_[0];
	my @array=@{$_[1]};
	
	my $found=0;
	my $imI=0;
	while ((not $found) and ($imI<=$#array)) {
		if ($element eq $array[$imI]) {$found=1};
		$imI++;
		};

	return $found;
	}
