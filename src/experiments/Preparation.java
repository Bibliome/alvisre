package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import corpus.Corpus;
import corpus.Document;
import corpus.NEtype;
import corpus.RelationType;
import corpus.SchemaType;
import corpus.Sentence;
import exceptions.UnexpectedDatasetFormat;
import io.input.AlvisAEjsonParser;
import io.input.AlvisNLP;
import io.input.BI;
import io.input.SchemaSignaturesParser;
import io.input.SeeDevSchemaSignaturesParser;
import io.output.SerializeMe;
import kernels.DummyClassifier;
import kernels.PartSimilarity;
import kernels.PartSimilarityTable;
import kernels.TriggerWords;
import representation.Candidate;
import representation.EntireSentence;
import representation.Path;
import representation.ShortestDependencyPath;
import representation.SurfacePath;
import representation.Transform;
import semantictools.SemanticClass;
import tools.PrintStuff;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

public class Preparation {

	/**
	 * A helper method to give the vector of classes (event type) of a list of candidates, so that it can be used for classification
	 * @param candidates the candidates
	 * @return a list of the class types as strings
	 */
	public static HashMap<String,String> getCandidatesClasses(ArrayList<Candidate> candidates){
		HashMap<String,String> classes = new HashMap<String,String>();
		for (Candidate candidate: candidates){
			if (candidate.relation == null) {
				classes.put(candidate.index,"none");
			}
			else {
				classes.put(candidate.index,candidate.relation.type.type);
			}
		}
		return classes;
	}
	/**
	 * Prepare the representation, only ShortestDependencyPath is supported for now
	 * @param the title (labeled/unlabeled/etc) for identification in the logs
	 * @param corpus the corpus on which we want to produce the Candidates
	 * @param the experiment
	 * @param if they are labeled or not
	 * @return the list of the candidates 
	 * @throws IOException 
	 */
	public static ArrayList<Candidate> prepareRepresentation(String candTitle, Corpus corpus, Experiment exp, Boolean isLabeled ,Boolean relations_on_relations) throws IOException{
		ArrayList<Candidate> allCandidates = new ArrayList<Candidate>();
		Integer candidatesize= 0;
		Integer couplesize = 0;
		Integer positiveCoupleSize = 0;
		Integer positiveDisconnectedCoupleSize = 0;
		Double positiveUniqueCoupleSize = 0.0;
		Double positiveUniqueDisconnectedCoupleSize = 0.0;
		Double verbose = exp.verbose;

		int documentCounter = 1;
		for (Document d : corpus.documents) {
			int candCounter = 0;
			if (verbose>4) {
				System.out.println(new Date().toString()+" Document "+documentCounter+"  out of "+corpus.documents.size());
			}
			documentCounter++;
			int sentenceCounter = 1;
			for (Sentence s : d.sentences) {
				if (verbose >5){
					System.out.println(new Date().toString()+"\t\t\tSentence "+sentenceCounter+" out of "+d.sentences.size());
				}
				sentenceCounter++;
				if (exp.fauxkernelconf.getBooleanValue("maskTerms")) s.maskTerms();
				Transform sp = null;
				if (exp.representation.equals("ShortestDependencyPath")) {
					sp = new ShortestDependencyPath(s,d);

				}
				else if (exp.representation.equals("SurfacePath")){
					sp = new SurfacePath(s,d);

				}
				else if (exp.representation.equals("EntireSentence")){
					sp = new EntireSentence(s,d);
				}
				else {
					throw (new UnexpectedDatasetFormat("Unknown representation choice"));
				}
				List<Candidate> sCands = sp.CalculateTransformation(exp, relations_on_relations);
				s.setCandidates(sCands);
				if (verbose >5){
					System.out.println(new Date().toString()+"\t\t\tFound  "+s.candidates.size()+" candidates");
				}
				candCounter+= s.candidates.size();
				allCandidates.addAll(sCands);
				candidatesize += sp.candidatesize;
				couplesize += sp.couplesize;
				positiveCoupleSize += sp.positiveCoupleSize;
				positiveDisconnectedCoupleSize += sp.positiveDisconnectedCoupleSize;
				positiveUniqueCoupleSize += sp.positiveUniqueCoupleSize;
				positiveUniqueDisconnectedCoupleSize += sp.positiveUniqueDisconnectedCoupleSize;
			}
			if (verbose>4) {
				System.out.println(new Date().toString()+" Found "+candCounter+ " candidates (document)");
			}

		}
		if (verbose>3) {
			System.out.println(new Date().toString()+" Found "+allCandidates.size()+" candidates (total)");
		}
		for (Candidate candidate: allCandidates) {
			if (candidate instanceof Path) {
				Path p = (Path) candidate;
				p.findRelation(isLabeled);
			}
			else { //the candidate is not a Path, we do not yet support this
				throw new UnexpectedDatasetFormat("We do not yet support things that aren't Paths. If you don't know what this means, then I have seriously fucked up!");
			}
		}


		//		/*
		//		 * debugging part
		//		 */
		//		for (Candidate candidate: allCandidates) {
		//			RelationArgument arg1 = candidate.arg1;
		//			RelationArgument arg2 = candidate.arg2;
		//			Candidate mirror = null;
		//			for (Candidate candmirr: allCandidates){
		//				if (candmirr.arg1.argument.equals(arg2.argument) && candmirr.arg2.argument.equals(arg1.argument)) {
		//					mirror = candmirr;
		//					break;
		//				}
		//			}
		//			if (mirror == null){
		//				System.out.println("problem d");
		//			}
		//			else {
		//				if (candidate.relation == null) {
		//					if (mirror.relation != null) {
		//						System.out.println("Problem a");
		//					}
		//					else {
		////						System.out.println("ok a");
		//					}
		//				}
		//				else {
		//					if (mirror.relation == null) {
		//						System.out.println("problem c");
		//					}
		//					else if (mirror.relation.equals(candidate.relation)) {
		////						System.out.println("ok b");
		//					}
		//					else {
		//						if (candidate.relation.type.type.equals("none")) {
		////							System.out.println("ok b2");
		//						}
		//						else {
		//							System.out.println("problem b");
		//						}
		//					}
		//				}
		//			}
		//		}
		//
		//		/*
		//		 * end debugging part
		//		 */

		exp.getDumper().println(candTitle+": found "+couplesize+" possible couples and "+candidatesize+" connected candidates among them.");
		exp.statistics.addStat(candTitle+" couples total\t", couplesize);
		exp.statistics.addStat(candTitle+" couples positive", positiveCoupleSize +" ("+positiveUniqueCoupleSize.intValue()+" unique)");
		exp.statistics.addStat(candTitle+" connected couples total\t", candidatesize);
		Integer connectedPos = positiveCoupleSize - positiveDisconnectedCoupleSize;
		Double uniqconnectedPos = positiveUniqueCoupleSize - positiveUniqueDisconnectedCoupleSize;
		exp.statistics.addStat(candTitle+" connected couples positive", connectedPos+" ("+uniqconnectedPos.intValue()+" unique)");

		return allCandidates;
	}





	/**
	 * Prepare the representation, only ShortestDependencyPath is supported for now
	 * @param type this should be "ShortestDependencyPath" for now
	 * @param corpus the corpus on which we want to produce the Candidates
	 * @param the experiment 
	 * @return the candidates in a <class, candidate list> map
	 * @throws IOException 
	 */
	public static  HashMap<String, ArrayList<Candidate>> prepareRepresentationPerClass(String trainOrTest,String type, Corpus corpus, Experiment exp, Boolean isLabeled, Boolean relations_on_relations) throws IOException{
		if (type.equals("ShortestDependencyPath")){
			HashMap<String, ArrayList<Candidate>> allCandidates = new HashMap<String, ArrayList<Candidate>>();
			ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
			Integer candidatesize= 0;
			Integer couplesize = 0;
			Integer positiveCoupleSize = 0;
			Integer positiveDisconnectedCoupleSize = 0;
			for (Document d : corpus.documents) {
				for (Sentence s : d.sentences) {
					if (exp.fauxkernelconf.getBooleanValue("maskTerms")) s.maskTerms();
					if (exp.representation.equals("ShortestDependencyPath")) {
						ShortestDependencyPath sp = new ShortestDependencyPath(s,d);
						List<Candidate> sCands = sp.CalculateTransformation(exp, relations_on_relations);
						s.setCandidates(sCands);
						candidateList.addAll(sCands);
						for (Candidate cand:sCands) {
							String reltype = cand.candidateRelationType.type;
							if (allCandidates.containsKey(reltype)) {
							}
							else {
								ArrayList<Candidate> thisCands = new ArrayList<>();
								allCandidates.put(reltype, thisCands);
							}
							allCandidates.get(reltype).add(cand);
						}
						candidatesize += sp.candidatesize;
						couplesize += sp.couplesize;
						positiveCoupleSize += sp.positiveCoupleSize;
						positiveDisconnectedCoupleSize += sp.positiveDisconnectedCoupleSize;
					}
					else if (exp.representation.equals("SurfacePath")){
						SurfacePath sp = new SurfacePath(s,d);
						List<Candidate> sCands = sp.CalculateTransformation(exp, relations_on_relations);
						s.setCandidates(sCands);
						candidateList.addAll(sCands);
						for (Candidate cand:sCands) {
							String reltype = cand.candidateRelationType.type;
							if (allCandidates.containsKey(reltype)) {
							}
							else {
								ArrayList<Candidate> thisCands = new ArrayList<>();
								allCandidates.put(reltype, thisCands);
							}
							allCandidates.get(reltype).add(cand);
						}					
						candidatesize += sp.candidatesize;
						couplesize += sp.couplesize;
						positiveCoupleSize += sp.positiveCoupleSize;
						positiveDisconnectedCoupleSize += sp.positiveDisconnectedCoupleSize;
					}
					else if (exp.representation.equals("EntireSentence")){
						EntireSentence sp = new EntireSentence(s,d);
						List<Candidate> sCands = sp.CalculateTransformation(exp, relations_on_relations);
						s.setCandidates(sCands);
						candidateList.addAll(sCands);
						for (Candidate cand:sCands) {
							String reltype = cand.candidateRelationType.type;
							if (allCandidates.containsKey(reltype)) {
							}
							else {
								ArrayList<Candidate> thisCands = new ArrayList<>();
								allCandidates.put(reltype, thisCands);
							}
							allCandidates.get(reltype).add(cand);
						}						
						candidatesize += sp.candidatesize;
						couplesize += sp.couplesize;
						positiveCoupleSize += sp.positiveCoupleSize;
						positiveDisconnectedCoupleSize += sp.positiveDisconnectedCoupleSize;					}
				}
			}
			for (Candidate candidate: candidateList) {
				if (candidate instanceof Path) {
					Path p = (Path) candidate;
					p.findRelation(isLabeled);
				}
				else { //the candidate is not a Path, we do not yet support this
					throw new UnexpectedDatasetFormat("We do not yet support things that aren't Paths. If you don't know what this means, then I have seriously fucked up!");
				}
			}
			exp.getDumper().println(trainOrTest+": found "+couplesize+" possible couples and "+candidatesize+" connected candidates among them.");
			exp.statistics.addStat(trainOrTest+" couples total\t", couplesize);
			exp.statistics.addStat(trainOrTest+" couples positive", positiveCoupleSize);
			exp.statistics.addStat(trainOrTest+" connected couples total\t", candidatesize);
			Integer connectedPos = positiveCoupleSize - positiveDisconnectedCoupleSize;
			exp.statistics.addStat(trainOrTest+" connected couples positive", connectedPos);

			return allCandidates;
		}
		//		else if (type.equals("Dummy")){
		//			
		//		}
		else throw new UnexpectedDatasetFormat("I only know ShortestDependencyPath as a representation!");
	}


	public static ArrayList<Candidate> specialPrepareRepresentation(String type, Corpus corpus, Experiment exp, ArrayList<Candidate> candidates, Boolean isLabeled) throws IOException{
		if (type.equals("ShortestDependencyPath")){
			ArrayList<Candidate> allCandidates = new ArrayList<Candidate>();
			Integer candidatesize= 0;
			Integer couplesize = 0;
			for (Document d : corpus.documents) {
				for (Sentence s : d.sentences) {
					if (exp.fauxkernelconf.getBooleanValue("maskTerms")) s.maskTerms();
					ShortestDependencyPath sp = new ShortestDependencyPath(s,d);
					List<Candidate> sCands = sp.specialCalculateTransformation(exp, candidates);
					s.setCandidates(sCands);
					allCandidates.addAll(sCands);
					candidatesize += sp.candidatesize;
					couplesize += sp.couplesize;
				}
			}
			for (Candidate candidate: allCandidates) {
				if (candidate instanceof Path) {
					Path p = (Path) candidate;
					p.findRelation(isLabeled);
				}
				else { //the candidate is not a Path, we do not yet support this
					throw new UnexpectedDatasetFormat("We do not yet support things that aren't Paths. If you don't know what this means, then I have seriously fucked up!");
				}
			}
			exp.getDumper().println("Found "+couplesize+" possible couples and "+candidatesize+" connected candidates among them.");
			return allCandidates;
		}
		else throw new UnexpectedDatasetFormat("I only know ShortestDependencyPath as a representation!");
	}

	/**
	 * Parse the XML configuration file and send this configuration to the right places (schema, similarity function etc)
	 * This is a long and detailed method of 200 lines of code. As it is tied to the configuration file, it is the first thing to change if the needs change.
	 * It can be split into three parts, mainly. First the pure "experiment" attributes: where the files will be read from and written to. Depending on different io.input types we also have different schemas (entities and relations possible).
	 * What type of experiment are we creating? Once we know these two things, we can create the appropriate experiment instance and read the necessary files, creating the necessary corpus( >=1). 
	 * We also define the type of Similarity Function we will be using and create its instance with all the partial similarities loaded. Finally we create the weka classifier that will be used, according to the config.  
	 * @throws Exception 
	 */
	public static Experiment parseConfig(File xmlfile) throws Exception {

		XMLConfiguration config = new XMLConfiguration(xmlfile);
		config.load();
		Experiment exp = new Experiment(xmlfile);
		String action = config.getString("action");
		Integer runs = config.getInteger("runs", new Integer(1));
		exp.threads = config.getInteger("threads", new Integer(0));

		Double percent = config.getDouble("percent", new Double(1));
		Integer folds = config.getInteger("folds", new Integer(10));
		try  {
			exp.verbose = exp.config.getDouble("verbose", new Double(1));
		}
		catch (ConversionException e){
			System.out.println("your verbose parameter seems to not be a number! Suggested values are 0 (min),1 (what it is doing),2 (in more detail) ,3 (with detailed predictions)  ");
			try  {
				Boolean verbs = exp.config.getBoolean("verbose", true);
				if (verbs) exp.verbose = new Double(1);
			}
			catch (ConversionException e2){
				System.out.println("your verbose parameter seems to not be a boolean either!: "+e2);
			}

		}

		String[] cValuesString = exp.config.getStringArray("C");
		for (String val:cValuesString) {
			Double d = Double.valueOf(val);
			if (!exp.cValues.contains(d)) {
				exp.cValues.add(d);
			}
		}
		exp.wordTags = exp.config.getStringArray("wordTags");

		exp.writeObjects = exp.config.getBoolean("writeObjects", false);
		exp.writeEntities = exp.config.getBoolean("writeEntities", false);
		exp.relations_on_relations = exp.config.getBoolean("relations_on_relations", false);
		exp.perClass = exp.config.getBoolean("perClass", false);

		exp.bionlpTask = exp.config.getInt("BioNLPTask",2);

		exp.name = exp.config.getString("name");

		String modelPath = exp.config.getString("modelPath", "");
		if (!modelPath.isEmpty()){
			exp.trainedModel = (Classifier) weka.core.SerializationHelper.read(modelPath);
		}

		String refCand = exp.config.getString("referenceCandidates", "");
		if (!refCand.isEmpty()){
			SerializeMe serial = new SerializeMe(exp.config.getString("referenceCandidates"));
			exp.referenceCandidates = serial.readCandidates();
		}
		File tmpfile = new File(System.getProperty("user.dir"),  exp.config.getString("outputPath"));
		if (exp.config.getString("outputPath").length() > 0 && exp.config.getString("outputPath").charAt(0) == '/') {
			tmpfile = new File(exp.config.getString("outputPath"));
		}

		exp.outputPath = tmpfile.getPath();
		if (!tmpfile.exists()){
			tmpfile.mkdir();
			if(exp.verbose > 0) System.out.println("Output directory created: "+exp.outputPath);
		}
		exp.inputType = exp.config.getString("type");


		exp.representation = exp.config.getString("Representation", "ShortestDependencyPath");
		HierarchicalConfiguration fauxkernelParameters = exp.config.configurationAt("SimilarityFunction(0)");
		HierarchicalConfiguration wekaConf = exp.config.configurationAt("Weka(0)");


		HashMap<String, String> stringConf = new HashMap<String, String>();
		HashMap<String, Double> doubleConf = new HashMap<String, Double>();
		HashMap<String, Boolean> booleanConf = new HashMap<String, Boolean>();
		doubleConf.put("verbose", exp.verbose);
		/*
		 * ###########################################################################################################
		 * SimilarityFunction (ex Kernel) Parameters
		 */
		stringConf.put("functionName", fauxkernelParameters.getString("functionName", "GlobalAlignment"));
		doubleConf.put("gapPenalty", fauxkernelParameters.getDouble("gapPenalty", 0.033));

		/*
		 * whether or not to mask entities by their entity type. e.g "LEC1" by "GENE" 
		 */
		booleanConf.put("maskTerms", fauxkernelParameters.getBoolean("maskTerms", false));
		/*
		 *  score to give to the similarity of two words with identical or different canonical forms 
		 */
		doubleConf.put("canonicalDifferent", fauxkernelParameters.getDouble("canonicalDifferent", 0.2));
		doubleConf.put("canonicalIdentical", fauxkernelParameters.getDouble("canonicalIdentical", 1.0));
		/*
		 *  score when two compared words belong to the same semantic class 
		 */
		doubleConf.put("sameSemanticClass",  fauxkernelParameters.getDouble("sameSemanticClass", 0.0));
		/*
		 * score when two compared words have the same or different POS tags
		 */
		doubleConf.put("posDifferent", fauxkernelParameters.getDouble("posDifferent", 0.2));
		doubleConf.put("posIdentical", fauxkernelParameters.getDouble("posIdentical", 1.0));

		/*
		 * score when two compared words have the same or different surface form (simple text comparison)
		 */
		doubleConf.put("surfaceDifferent", fauxkernelParameters.getDouble("surfaceDifferent", 0.2));
		doubleConf.put("surfaceIdentical", fauxkernelParameters.getDouble("surfaceIdentical", 0.7));
		/* 
		 * similarity score between two dependency relations, by comparing the name of that relation. the "family" trick consists in comparing 
		 * the first letters of the names, as in some parsers the prefix expresses some similarity
		 */ 
		doubleConf.put("sameSyntacticDependencyType", fauxkernelParameters.getDouble("sameSyntacticDependencyType", 1.0));
		doubleConf.put("differentSyntacticDependencyType", fauxkernelParameters.getDouble("differentSyntacticDependencyType", 0.2));
		doubleConf.put("sameSyntacticDependencyFamily",  fauxkernelParameters.getDouble("sameSyntacticDependencyFamily", 0.5));

		/* 
		 * weights of the above measures
		 */
		doubleConf.put("surfaceSimilarityWeight", fauxkernelParameters.getDouble("surfaceSimilarityWeight", 0.5));
		doubleConf.put("canonicalSimilarityWeight", fauxkernelParameters.getDouble("canonicalSimilarityWeight", 1.0));
		doubleConf.put("POSWeight", fauxkernelParameters.getDouble("POSWeight", 0.1));
		doubleConf.put("discoWeight", fauxkernelParameters.getDouble("discoWeight", 0.0));
		doubleConf.put("word2vecWeight", fauxkernelParameters.getDouble("word2vecWeight", 0.0));
		doubleConf.put("WordNetWeight", fauxkernelParameters.getDouble("WordNetWeight", 0.0));
		/* 
		 * wordpath-related weights
		 */
		doubleConf.put("wordpathCost", fauxkernelParameters.getDouble("wordpathCost", 3.0));
		doubleConf.put("wordpathSyntaxSimilarity", fauxkernelParameters.getDouble("wordpathSyntaxSimilarity",0.7));

		/* 
		 * external tools parameters
		 */
		stringConf.put("canonicalStringSimilarity", fauxkernelParameters.getString("canonicalStringSimilarity", "sd"));
		stringConf.put("discoDir", fauxkernelParameters.getString("discoDir","en-PubMedOA-20070501/"));
		stringConf.put("word2vecVectorFile", fauxkernelParameters.getString("word2vecVectorFile", "/bibdev/travail/word2vec/corpus/pubmed_ontobiotope.vectors.bin"));
		stringConf.put("surfaceStringSimilarity", fauxkernelParameters.getString("surfaceStringSimilarity", "sd"));
		stringConf.put("WordNetAlgorithm",fauxkernelParameters.getString("WordNetAltogirhm","HSO"));


		/*
		 * per class c (cost) parameter for SVMs
		 */
		List<HierarchicalConfiguration> cPerClassEntries = exp.config.configurationsAt("classCosts.entry");
		HashMap<String, Double> classCosts = new HashMap<String, Double>();
		for (Iterator<HierarchicalConfiguration> en = cPerClassEntries.iterator(); en.hasNext();) {
			HierarchicalConfiguration ensub = en.next();
			ArrayList<String> classes = new ArrayList<String>();
			classes.addAll(Arrays.asList(ensub.getStringArray("class")));
			Double classCost = ensub.getDouble("cost", 0.1);
			for (String className:classes) {
				classCosts.put(className, classCost);
			}
		}
		exp.classCosts = classCosts;

		/*
		 * Similarity tables when defined give manual or pre-calculated similarity tables for words or in the special case of syntacticDependencies below, 
		 * semantic dependencies
		 */
		List<HierarchicalConfiguration> similarityTables = fauxkernelParameters.configurationsAt("partSimilarityTables.similarityTable");
		for (Iterator<HierarchicalConfiguration> it = similarityTables.iterator(); it.hasNext();) {
			HierarchicalConfiguration sub = it.next();
			String layer = sub.getString("layer");
			PartSimilarityTable simtable = new PartSimilarityTable(layer);
			exp.simTables.add(simtable);
			List<HierarchicalConfiguration> entries = sub.configurationsAt("entry");
			for (Iterator<HierarchicalConfiguration> en = entries.iterator(); en.hasNext();) {
				HierarchicalConfiguration ensub = en.next();
				String tag1 = ensub.getString("tag(0)");
				String tag2 = ensub.getString("tag(1)");
				Double similarity = ensub.getDouble("similarity", 0.5);
				PartSimilarity sim = new PartSimilarity(tag1, tag2, similarity);
				simtable.addEntry(sim);
			}
		}
		PartSimilarityTable syntacticDependencyDistance = new PartSimilarityTable("syntacticDependencies");
		List<HierarchicalConfiguration> entries = fauxkernelParameters.configurationsAt("syntacticDependencyDistance.entry");
		for (Iterator<HierarchicalConfiguration> en = entries.iterator(); en.hasNext();) {
			HierarchicalConfiguration ensub = en.next();
			String tag1 = ensub.getString("type(0)");
			String tag2 = ensub.getString("type(1)");
			Double similarity = ensub.getDouble("similarity", 0.5);
			PartSimilarity sim = new PartSimilarity(tag1, tag2, similarity);
			syntacticDependencyDistance.addEntry(sim);
		}
		exp.simTables.add(syntacticDependencyDistance);
		List<HierarchicalConfiguration> sementries = fauxkernelParameters.configurationsAt("SemanticClasses.SemanticClass");
		for(Iterator<HierarchicalConfiguration> it = sementries.iterator(); it.hasNext();){
			HierarchicalConfiguration entry = it.next();
			SemanticClass semclass = new SemanticClass();
			semclass.addAll(Arrays.asList(entry.getStringArray("words")));
			exp.wordToSemClassTable.populate(semclass);
		}

		/*
		 * Trigger words can be defined for each type of relation
		 */
		List<HierarchicalConfiguration> triggerwordentries = fauxkernelParameters.configurationsAt("TriggerWords.TriggerWordSet");
		for (Iterator<HierarchicalConfiguration> it =triggerwordentries.iterator(); it.hasNext(); ){
			HierarchicalConfiguration entry = it.next();
			TriggerWords trwords = new TriggerWords();
			trwords.addAll(Arrays.asList(entry.getStringArray("words")));
			String relationtype = entry.getString("RelationType");
			exp.triggerWordTable.put(relationtype,trwords);
			exp.hasTriggerWords = true;
		}

		exp.fauxkernelconf.SetConfiguration(stringConf, doubleConf, booleanConf, exp.simTables, exp.wordToSemClassTable, exp.triggerWordTable);

		/*
		 * End of Similarity function parameters 
		 * #########################################################################################################################################
		 */
		Boolean jsonconf = exp.config.containsKey("schemaJSON");
		Boolean signatures = exp.config.containsKey("schemaSignatures");
		Boolean signaturesDir = exp.config.containsKey("schemaSignaturesDir");
		if (jsonconf) {
			String jsonfile = exp.config.getString("schemaJSON");
			exp.schema = AlvisAEjsonParser.parseJSON(jsonfile);
			//System.out.println("Parsed schema from "+jsonfile);
		}
		else if (signatures){
			String signatureFile = exp.config.getString("schemaSignatures");
			exp.schema = SchemaSignaturesParser.parseFile(signatureFile);
			System.out.println("Parsed schema from "+signatureFile);
		}
		else if (signaturesDir) {
			String signatureDir = exp.config.getString("schemaSignaturesDir");
			exp.schema = SeeDevSchemaSignaturesParser.parseDirectory(signatureDir);
			Boolean filterRelationTypes = exp.config.containsKey("selectedRelationTypes");
			if (filterRelationTypes) {
				List<String> selectedRelationsTypes = Arrays.asList(exp.config.getStringArray("selectedRelationTypes"));
				exp.selectedRelationTypes.addAll(selectedRelationsTypes);
				// only works for per class CV for now
				List<RelationType> keptRelationTypes = new ArrayList<RelationType>();
				for (RelationType rtype: exp.schema.DefinedRelationTypes) {
					if (exp.selectedRelationTypes.contains(rtype.type)) {
						keptRelationTypes.add(rtype);
					}
				}
				exp.schema.DefinedRelationTypes = keptRelationTypes;
			}
			else {
				for (RelationType rtype: exp.schema.DefinedRelationTypes) {
					exp.selectedRelationTypes.add(rtype.type);
				}
			}
			System.out.println("Parsed schema from "+signatureDir);
			PrintStuff.printSchema(exp.schema, exp.selectedRelationTypes);
		}
		else {
			if (!exp.inputType.equals("BI")){
				/*
				 * Schema
				 */
				HierarchicalConfiguration schemaconf = exp.config.configurationAt("schema(0)");
				List<HierarchicalConfiguration> netypes = schemaconf.configurationsAt("NEtypes.NEtype");
				for (Iterator<HierarchicalConfiguration> ne = netypes.iterator(); ne.hasNext();) {
					HierarchicalConfiguration nesub = ne.next();
					String nename = nesub.getString("name");
					NEtype namedEntity = new NEtype(nename);
					exp.schema.addType(namedEntity);
				}
				List<HierarchicalConfiguration> rtypes = schemaconf.configurationsAt("Rtypes.Rtype");
				HashMap<String, ArrayList<ArrayList<String>>> rels_with_relargs = new HashMap<String, ArrayList<ArrayList<String>>>();
				for (Iterator<HierarchicalConfiguration> r = rtypes.iterator(); r.hasNext();) {
					HierarchicalConfiguration rsub = r.next();
					String rname = rsub.getString("name");
					HierarchicalConfiguration a1sub = rsub.configurationAt("arguments.argument(0)");
					HierarchicalConfiguration a2sub = rsub.configurationAt("arguments.argument(1)");
					List<NEtype> arg1 = new ArrayList<NEtype>();
					List<NEtype> arg2 = new ArrayList<NEtype>();
					String role1 = a1sub.getString("role");
					String role2 = a2sub.getString("role");
					List<HierarchicalConfiguration> a1types = a1sub.configurationsAt("type");
					Boolean takes_rel_arg = false;
					Boolean takes_ne_arg = false;
					ArrayList<String> arg1rels = new ArrayList<String>();
					ArrayList<String> arg2rels = new ArrayList<String>();
					for (Iterator<HierarchicalConfiguration> t = a1types.iterator(); t.hasNext();) {
						HierarchicalConfiguration tsub = t.next();
						String tname = tsub.getString("name");
						NEtype netype = exp.schema.getNETypeByName(tname);
						if (netype == null) {
							takes_rel_arg = true;
						}
						else {
							takes_ne_arg = true;
							arg1.add(netype);
						}
						arg1rels.add(tname);


					}
					List<HierarchicalConfiguration> a2types = a2sub.configurationsAt("type");
					for (Iterator<HierarchicalConfiguration> t = a2types.iterator(); t.hasNext();) {
						HierarchicalConfiguration tsub = t.next();
						String tname = tsub.getString("name");
						NEtype netype = exp.schema.getNETypeByName(tname);
						if (netype == null) {
							takes_rel_arg = true;
						}
						else {
							takes_ne_arg = true;
							arg2.add(netype);
						}		
						arg2rels.add(tname);

					}

					if (takes_rel_arg) {
						ArrayList<String> roles = new ArrayList<>();
						//						roles.add(rname);
						roles.add(role1);
						roles.add(role2);
						ArrayList<ArrayList<String>> args = new ArrayList<ArrayList<String>>();
						args.add(roles);
						args.add(arg1rels);
						args.add(arg2rels);
						rels_with_relargs.put(rname, args);
						RelationType rtype = new RelationType(rname);
						rtype.relation_on_relations = true;
						exp.schema.addRelationType(rtype);
					}
					else if (takes_ne_arg) {
						RelationType rtype = new RelationType(rname, role1, arg1, role2, arg2);
						exp.schema.addRelationType(rtype);
					}
				}

				for (RelationType rtype:exp.schema.DefinedRelationTypes){
					if (rtype.relation_on_relations){
						ArrayList<ArrayList<String>> args = rels_with_relargs.get(rtype.type);
						ArrayList<String> roles = args.get(0);
						ArrayList<String> arg1rels = args.get(1);
						ArrayList<String> arg2rels = args.get(2);
						ArrayList<SchemaType> arg1reltypes = new ArrayList<SchemaType>();
						ArrayList<SchemaType> arg2reltypes = new ArrayList<SchemaType>();
						for (String arg1type:arg1rels){
							ArrayList<SchemaType> thesearg1reltypes = new ArrayList<SchemaType>();
							thesearg1reltypes.addAll(exp.schema.getRelationTypesByName(arg1type));
							thesearg1reltypes.add(exp.schema.getNETypeByName(arg1type));
							arg1reltypes.addAll(thesearg1reltypes);
						}
						for (String arg2type:arg2rels){
							ArrayList<SchemaType> thesearg2reltypes = new ArrayList<>();
							thesearg2reltypes.addAll(exp.schema.getRelationTypesByName(arg2type));
							thesearg2reltypes.add(exp.schema.getNETypeByName(arg2type));
							arg2reltypes.addAll(thesearg2reltypes);
						}
						rtype.updateRelation(roles.get(0), arg1reltypes, roles.get(1), arg2reltypes);
					}
				}


			}
		}
		//		Boolean nondirect = exp.config.containsKey("nonDirectional");
		//		if (nondirect) {
		//			//			HierarchicalConfiguration entry = exp.config.configurationAt("nonDirectional");
		//
		//			List<String> nondirectionals = Arrays.asList(exp.config.getStringArray("nonDirectional"));
		//			for (String rel:nondirectionals) {
		//				RelationType reltype = exp.schema.getRelationTypeByName(rel);
		//				reltype.directional = false;
		//				System.out.println(rel+" declared as non-directional. (harmless if duplicate message)");
		//			}
		//		}



		Remove rm = new Remove();
		rm.setAttributeIndices("1");
		FilteredClassifier filteredClassifier = new FilteredClassifier();
		filteredClassifier.setFilter(rm);
		exp.wekaClassifier = filteredClassifier;

		if (wekaConf.getString("Algorithm").equals("Advanced")){

			switch(wekaConf.getString("Classifier").toLowerCase()){
			case "ibk":
				IBk ibk = new IBk();
				ibk.setOptions(weka.core.Utils.splitOptions( wekaConf.getString("Options")));	
				filteredClassifier.setClassifier(ibk);
				break;
			case "liblinear":
				LibLINEAR liblinear = new LibLINEAR();
				liblinear.setOptions(weka.core.Utils.splitOptions( wekaConf.getString("Options")));		
				filteredClassifier.setClassifier(liblinear);
				break;
			case "libsvm":
				LibSVM libsvm  = new LibSVM();
				libsvm.setOptions(weka.core.Utils.splitOptions( wekaConf.getString("Options")));
				filteredClassifier.setClassifier(libsvm);
				break;
			case "dummy":
				DummyClassifier dummy = new DummyClassifier();
				dummy.setOptions(weka.core.Utils.splitOptions( wekaConf.getString("Options")));
				filteredClassifier.setClassifier(dummy);
				break;
			default:
				AbstractClassifier tmp = (AbstractClassifier) AbstractClassifier.forName(wekaConf.getString("Classifier"), weka.core.Utils.splitOptions( wekaConf.getString("Options")));
				//				AbstractClassifier.forName("weka.classifiers.trees.J48", null);
				filteredClassifier.setClassifier(tmp);
				break;
			}
		}
		else if (wekaConf.getString("Algorithm").equals("Preset")){
			switch(wekaConf.getString("Classifier").toLowerCase()){
			case "1nn":
				IBk nn1 = new IBk(1);
				filteredClassifier.setClassifier(nn1);
				break;
			case "2nn":
				IBk nn2 = new IBk(2);
				filteredClassifier.setClassifier(nn2);
				break;
			case "3nn":
				IBk nn3= new IBk(3);
				filteredClassifier.setClassifier(nn3);
				break;
			case "libsvm-linear":
				LibSVM libsvm = new LibSVM();
				libsvm.setOptions(weka.core.Utils.splitOptions("-K 0")); // linear kernel
				filteredClassifier.setClassifier(libsvm);
				break;
			case "liblinear-l2l2p":
				LibLINEAR l2l2p  = new LibLINEAR();
				l2l2p.setOptions(weka.core.Utils.splitOptions("-S 2")); //     2 = L2-loss support vector machines (primal)
				filteredClassifier.setClassifier(l2l2p);
				break;
			case "liblinear-l2l2d":
				LibLINEAR l2l2d = new LibLINEAR();
				l2l2d.setOptions(weka.core.Utils.splitOptions("-S 1")); //    1 = L2-loss support vector machines (dual)
				filteredClassifier.setClassifier(l2l2d);
				break;
			case "liblinear-multi":
				LibLINEAR multi = new LibLINEAR();
				multi.setOptions(weka.core.Utils.splitOptions("-S 4")); //    4 = multi-class support vector machines by Crammer and Singer
				filteredClassifier.setClassifier(multi);
			case "dummy":
				DummyClassifier dummy = new DummyClassifier();
				filteredClassifier.setClassifier(dummy);
				break;
			default: 
				LibLINEAR def = new LibLINEAR();
				def.setOptions(weka.core.Utils.splitOptions("-S 2")); //     2 = L2-loss support vector machines (primal)
				filteredClassifier.setClassifier(def);
				break;
			}
		}


		if (exp.verbose > 0)
			System.out.println(new Date().toString()+" XML Configuration File parsed with success.");

		if (action.equalsIgnoreCase("CrossValidation")){
			if (exp.config.getString("evaluation", "Weka").equalsIgnoreCase("Weka")) {
				return processWekaCV(exp);

			}
			else if (exp.config.getString("evaluation").equalsIgnoreCase("evalscript")){
				if (folds <= 1) { 
					throw (new UnexpectedDatasetFormat("folds should be more than 1"));
				}
				else  {
					Predict tat = new Predict(exp);
					tat.labeledPath = tat.config.getString("inputPath");
					//					tat.unlabeledPath = tat.config.getString("inputPath");
					tat.isTrainAndTest = true;

					if (tat.config.containsKey("inputCandidates")){
						System.out.println("Reading saved trainCandidates");
						SerializeMe serial = new SerializeMe(tat.config.getString("inputCandidates"));
						tat.labeledCandidates = serial.readCandidates();
						if (tat.config.containsKey("inputArff")){
							System.out.println("Reading saved inputArff");
							tat.labeledSet = new Instances(new BufferedReader(new FileReader(tat.config.getString("inputArff"))));
							tat.labeledSet.setClassIndex(tat.labeledSet.numAttributes() - 1);
						}
						tat.writeObjects = false;

					}
					else {
						if (tat.inputType.equals("AlvisNLP")) {
							tat.labeledInput = new AlvisNLP(tat.labeledPath, exp.schema);
							tat.labeledInput.setSchema(tat.schema);
							tat.labeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
							tat.labeledCorpus = tat.labeledInput.getCorpus();
						}
						else if (tat.inputType.equals("BI")){
							tat.labeledInput = new BI(tat.labeledPath);
							tat.labeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
							tat.schema = tat.labeledInput.schema;
							tat.labeledCorpus = tat.labeledInput.getCorpus();
							tat.referencePath = tat.config.getString("BIreferencePath");
							if (tat.referencePath == null) tat.referencePath = tat.config.getString("referencePath");
						}
						else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
					}




					tat.outputFormat = tat.config.getString("outputFormat", "other");
					//				if ((cv.outputFormat.equalsIgnoreCase("Challenge")) || (cv.outputFormat.equalsIgnoreCase("Both"))){
					tat.outputFormatChallenge = tat.config.getString("outputFormatChallenge", "other");
					if (tat.outputFormat.equals("Challenge") && tat.outputFormatChallenge.equals("biotopes")){
						tat.evaluationScript = tat.config.getString("evaluationScript");
						tat.txtPath = tat.config.getString("txtPath");
						tat.referencePath = tat.config.getString("referencePath");
					}
					return new MultipleExperiment(tat,runs, folds);
				}
			}
		}
		if (action.equalsIgnoreCase("TrainAndTest")){
			Predict tat = new Predict(exp);
			tat.labeledPath = tat.config.getString("trainPath");
			tat.unlabeledPath = tat.config.getString("testPath");
			tat.isTrainAndTest = true;

			if (tat.config.containsKey("trainCandidates")){
				System.out.println("Reading saved trainCandidates");
				SerializeMe serial = new SerializeMe(tat.config.getString("trainCandidates"));
				tat.labeledCandidates = serial.readCandidates();
				System.out.println("Reading saved trainArff");
				if  (tat.config.containsKey("trainArff")){
					tat.labeledSet = new Instances(new BufferedReader(new FileReader(tat.config.getString("trainArff"))));
					tat.labeledSet.setClassIndex(tat.labeledSet.numAttributes() - 1);
				}
				tat.writeObjects = false;
				if (tat.config.containsKey("testCandidates")&& tat.config.containsKey("testArff")){
					System.out.println("Reading saved testCandidates");
					serial = new SerializeMe(tat.config.getString("testCandidates"));
					tat.unlabeledCandidates = serial.readCandidates();
					System.out.println("Reading saved testArff");
					tat.unlabeledSet = new Instances(new BufferedReader(new FileReader(tat.config.getString("testArff"))));
					tat.unlabeledSet.setClassIndex(tat.unlabeledSet.numAttributes() - 1);
					tat.writeObjects = false;

				}
				else {
					if (tat.inputType.equals("AlvisNLP")) {
						tat.unlabeledInput = new AlvisNLP(tat.unlabeledPath, exp.schema);
						tat.unlabeledInput.setSchema(tat.schema);
						tat.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
						tat.unlabeledCorpus = tat.unlabeledInput.getCorpus();
					}
					else if (tat.inputType.equals("BI")){
						tat.unlabeledInput = new BI(tat.unlabeledPath);
						tat.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
						tat.schema = tat.labeledInput.schema;
						tat.unlabeledCorpus = tat.unlabeledInput.getCorpus();
						tat.referencePath = tat.config.getString("BIreferencePath");
						if (tat.referencePath == null) tat.referencePath = tat.config.getString("referencePath");
					}
					else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
				}

			}
			else {
				if (tat.inputType.equals("AlvisNLP")) {
					tat.labeledInput = new AlvisNLP(tat.labeledPath, exp.schema);
					tat.labeledInput.setSchema(tat.schema);
					tat.labeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					tat.labeledCorpus = tat.labeledInput.getCorpus();
					tat.unlabeledInput = new AlvisNLP(tat.unlabeledPath, exp.schema);
					tat.unlabeledInput.setSchema(tat.schema);
					tat.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					tat.unlabeledCorpus = tat.unlabeledInput.getCorpus();
				}
				else if (tat.inputType.equals("BI")){
					tat.labeledInput = new BI(tat.labeledPath);
					tat.labeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					tat.schema = tat.labeledInput.schema;
					tat.labeledCorpus = tat.labeledInput.getCorpus();
					tat.referencePath = tat.config.getString("BIreferencePath");
					if (tat.referencePath == null) tat.referencePath = tat.config.getString("referencePath");
					tat.unlabeledInput = new BI(tat.unlabeledPath);
					tat.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					tat.schema = tat.labeledInput.schema;
					tat.unlabeledCorpus = tat.unlabeledInput.getCorpus();
					tat.referencePath = tat.config.getString("BIreferencePath");
					if (tat.referencePath == null) tat.referencePath = tat.config.getString("referencePath");
				}
				else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
			}



			tat.outputFormat = tat.config.getString("outputFormat", "other");
			//				if ((cv.outputFormat.equalsIgnoreCase("Challenge")) || (cv.outputFormat.equalsIgnoreCase("Both"))){
			tat.outputFormatChallenge = tat.config.getString("outputFormatChallenge", "other");
			if (tat.outputFormat.equals("Challenge") && tat.outputFormatChallenge.equals("biotopes")){
				tat.evaluationScript = tat.config.getString("evaluationScript");
				tat.txtPath = tat.config.getString("txtPath");
				tat.referencePath = tat.config.getString("referencePath");
			}
			if (percent == 1) { 
				return tat;
			}
			else if (percent < 1) {
				return new MultipleExperiment(tat,runs, percent);
			}
			else throw(new UnexpectedDatasetFormat("runs should be >0"));
		}
		if (action.equalsIgnoreCase("Predict")){
			Predict pr = new Predict(exp);
			pr.labeledPath = pr.config.getString("labeledPath");
			pr.unlabeledPath = pr.config.getString("unlabeledPath");

			if (pr.config.containsKey("labeledCandidates")){
				System.out.println("Reading saved labeledCandidates");
				SerializeMe serial = new SerializeMe(pr.config.getString("labeledCandidates"));
				pr.labeledCandidates = serial.readCandidates();
				if (pr.config.containsKey("labeledArff")){
					System.out.println("Reading saved labeledArff");
					pr.labeledSet = new Instances(new BufferedReader(new FileReader(pr.config.getString("labeledArff"))));
					pr.labeledSet.setClassIndex(pr.labeledSet.numAttributes() - 1);
				}
				pr.writeObjects = false;
				if (pr.config.containsKey("unlabeledCandidates") && pr.config.containsKey("unlabeledArff")){
					System.out.println("Reading saved unlabeledCandidates");
					serial = new SerializeMe(pr.config.getString("unlabeledCandidates"));
					pr.unlabeledCandidates = serial.readCandidates();
					System.out.println("Reading saved unlabeledArff");
					pr.unlabeledSet = new Instances(new BufferedReader(new FileReader(pr.config.getString("unlabeledArff"))));
					pr.unlabeledSet.setClassIndex(pr.unlabeledSet.numAttributes() - 1);
					pr.writeObjects = false;
				}
				else {
					if (pr.inputType.equals("AlvisNLP")) {
						pr.unlabeledInput = new AlvisNLP(pr.unlabeledPath, exp.schema);
						pr.unlabeledInput.setSchema(pr.schema);
						pr.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
						pr.unlabeledCorpus = pr.unlabeledInput.getCorpus();
					}
					else if (pr.inputType.equals("BI")){
						pr.unlabeledInput = new BI(pr.unlabeledPath);
						pr.schema = pr.labeledInput.schema;
						pr.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
						pr.unlabeledCorpus = pr.unlabeledInput.getCorpus();
					}
					else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");

				}

			}
			else {
				if (pr.inputType.equals("AlvisNLP")) {
					pr.labeledInput = new AlvisNLP(pr.labeledPath, exp.schema);
					pr.labeledInput.setSchema(pr.schema);
					pr.labeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					pr.labeledCorpus = pr.labeledInput.getCorpus();
					pr.unlabeledInput = new AlvisNLP(pr.unlabeledPath, exp.schema);
					pr.unlabeledInput.setSchema(pr.schema);
					pr.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					pr.unlabeledCorpus = pr.unlabeledInput.getCorpus();
				}
				else if (pr.inputType.equals("BI")){
					pr.labeledInput = new BI(pr.labeledPath);
					pr.schema = pr.labeledInput.schema;
					pr.labeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					pr.labeledCorpus = pr.labeledInput.getCorpus();
					pr.unlabeledInput = new BI(pr.unlabeledPath);
					pr.schema = pr.labeledInput.schema;
					pr.unlabeledInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					pr.unlabeledCorpus = pr.unlabeledInput.getCorpus();
				}
				else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");

			}





			pr.outputFormat = pr.config.getString("outputFormat", "other");
			//			if (pr.outputFormat !=null){ //TODO
			//				if ((pr.outputFormat.equalsIgnoreCase("Challenge")) || (pr.outputFormat.equalsIgnoreCase("Both"))){
			pr.outputFormatChallenge = pr.config.getString("outputFormatChallenge","other");
			if(pr.outputFormatChallenge.equals("LLL")){
				pr.LLLkey = pr.config.getString("LLLkey");
				pr.LLLdictionary = pr.config.getString("LLLdictionary");
				pr.LLLtool = pr.config.getString("LLLtool");
				pr.LLLtest = pr.config.getString("LLLtest");
				pr.LLLout = pr.config.getString("LLLout");
			}
			//				}
			//			}
			if (percent.equals(new Double(1))) { 
				return pr;
			}
			else if (percent.compareTo(new Double(1)) < 0)  {
				return new MultipleExperiment(pr, runs, percent);
			}
			else throw(new UnexpectedDatasetFormat("runs should be >0"));

		}
		else if (action.equalsIgnoreCase("NNsearch")){
			NearestNeighbours nn = new NearestNeighbours(exp);
			nn.allPath = nn.config.getString("allPath");
			nn.targetPath = nn.config.getString("targetPath");
			nn.k = nn.config.getInt("k");
			nn.allLabeled = nn.config.getBoolean("allLabeled", true);
			nn.targetLabeled = nn.config.getBoolean("targetLabeled", false);

			if (nn.config.containsKey("allCandidates") && nn.config.containsKey("allArff")){
				System.out.println("Reading saved allCandidates");
				SerializeMe serial = new SerializeMe(nn.config.getString("allCandidates"));
				nn.allCandidates = serial.readCandidates();
				System.out.println("Reading saved allArff");
				nn.allSet = new Instances(new BufferedReader(new FileReader(nn.config.getString("allArff"))));
				nn.allSet.setClassIndex(nn.allSet.numAttributes() -1);
				nn.writeObjects = false;

				if (nn.config.containsKey("targetCandidates")&& nn.config.containsKey("targetArff")){
					System.out.println("Reading saved targetCandidates");
					serial = new SerializeMe(nn.config.getString("targetCandidates"));
					nn.targetCandidates = serial.readCandidates();
					System.out.println("Reading saved targetArff");
					nn.targetSet = new Instances(new BufferedReader(new FileReader(nn.config.getString("targetArff"))));
					nn.targetSet.setClassIndex(nn.targetSet.numAttributes() -1);
					nn.writeObjects = false;
				}
				else {
					if (nn.inputType.equals("AlvisNLP")){
						nn.targetInput = new AlvisNLP(nn.targetPath, exp.schema);
						nn.targetInput.setSchema(exp.schema);
						nn.targetInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
						nn.targetCorpus = nn.targetInput.getCorpus();
					}
					else if (nn.inputType.equals("BI")){
						nn.targetInput  = new BI(nn.targetPath);
						nn.targetInput.setSchema(exp.schema);
						nn.targetInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
						nn.targetCorpus = nn.targetInput.getCorpus();
					}
					else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
				}

			}
			else {
				if (nn.inputType.equals("AlvisNLP")){
					nn.allInput = new AlvisNLP(nn.allPath, exp.schema);
					nn.allInput.setSchema(exp.schema);
					nn.allInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					nn.allCorpus = nn.allInput.getCorpus();
					nn.targetInput = new AlvisNLP(nn.targetPath, exp.schema);
					nn.targetInput.setSchema(exp.schema);
					nn.targetInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					nn.targetCorpus = nn.targetInput.getCorpus();
				}
				else if (nn.inputType.equals("BI")){
					nn.allInput = new BI(nn.allPath);
					nn.allInput.setSchema(exp.schema);
					nn.allInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					nn.allCorpus = nn.allInput.getCorpus();
					nn.targetInput  = new BI(nn.targetPath);
					nn.targetInput.setSchema(exp.schema);
					nn.targetInput.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
					nn.targetCorpus = nn.targetInput.getCorpus();
				}
				else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
			}
			return nn;	
		}



		else if (action.equalsIgnoreCase("Extract")){
			ExtractCandidates extr = new ExtractCandidates(exp);
			extr.path = extr.config.getString("path");
			extr.labeled = extr.config.getBoolean("labeled", true);
			extr.externalCorpus = extr.config.getBoolean("externalCorpus", true);

			if (extr.inputType.equals("AlvisNLP")){
				extr.input = new AlvisNLP(extr.path, exp.schema);
				extr.input.setSchema(exp.schema);
				extr.input.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
				extr.corpus = extr.input.getCorpus();
			}
			else if (extr.inputType.equals("BI")){
				extr.input = new BI(extr.path);
				extr.input.setSchema(exp.schema);
				extr.input.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
				extr.corpus = extr.input.getCorpus();
			}
			else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
			return extr;	
		}
		throw new UnexpectedDatasetFormat("Can only recognize CrossValidation,TrainAndTest and Predict as scenarios. Please check your xml configuration file!");
	}
	private static Experiment processWekaCV(Experiment exp) throws Exception {

		CrossValidation cv =  new CrossValidation(exp);
		String filename = System.getProperty("user.dir")+"/"+cv.config.getString("inputPath");
		if (cv.config.getString("inputPath").length() > 0 && cv.config.getString("inputPath").charAt(0) == '/'){
			filename = cv.config.getString("inputPath");
		}
		cv.inputPath = new File(filename).getAbsolutePath();

		cv.outputFormat = cv.config.getString("outputFormat", "other");
		cv.outputFormatChallenge = cv.config.getString("outputFormatChallenge", "other");
		if (cv.config.containsKey("inputCandidates")){
			System.out.println("Reading saved inputCandidates");
			SerializeMe serial = new SerializeMe(cv.config.getString("inputCandidates"));
			cv.candidates = serial.readCandidates();
			if (cv.config.containsKey("inputArff")) {
				System.out.println("Reading saved inputArff");
				cv.set = new Instances(new BufferedReader(new FileReader(cv.config.getString("inputArff"))));
				cv.set.setClassIndex(cv.set.numAttributes()-1);
			}
			cv.writeObjects = false;

		}
		else {
			if (cv.inputType.equals("AlvisNLP")) {
				cv.input  = new AlvisNLP(cv.inputPath, exp.schema);
				cv.input.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
				cv.corpus = cv.input.getCorpus();
				//TODO: do the rest	|| later comment :WHAT DOES THIS MEAN???? || later again: i think it's ok actually :P 
			}
			else if (exp.inputType.equals("BI")){
				cv.input = new BI(cv.inputPath);
				cv.schema = cv.input.schema;
				cv.input.importFiles(exp.verbose, (exp.threads >1), exp.wordTags);
				cv.corpus = cv.input.getCorpus();
			}
			else throw new UnexpectedDatasetFormat("Error E63: Input type uknown or not yet implemented. Supported types: AlvisNLP, BI");
		}

		cv.runs = cv.config.getInteger("runs", 1);
		cv.folds = cv.config.getInteger("folds", 10);
		if (cv.runs < 1){
			throw (new UnexpectedDatasetFormat("runs should be at least 1"));
		}
		if (cv.folds <= 1) { 
			throw (new UnexpectedDatasetFormat("folds should be more than 1"));
		}
		return cv;
	}



}
