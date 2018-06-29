package experiments;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.python.google.common.primitives.Ints;

import corpus.Corpus;
import corpus.Document;
import corpus.Relation;
import corpus.Sentence;
import exceptions.UnexpectedDatasetFormat;
import io.output.AlvisNLPo;
import io.output.ArffWriter;
import io.output.BIo;
import io.output.LLL;
import io.output.OutputModule;
import io.output.SerializeMe;
import kernels.DummyKernel;
import kernels.EmpiricalKernelMap;
import kernels.GlobalAlignment;
import kernels.SimilarityFunction;
import representation.Candidate;
import tools.Dumper;
import tools.Evaluation;
import tools.PrintStuff;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.InstanceComparator;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class Process {

	public Process() {
	}

	public static void runPrediction(Predict exp) throws Exception {
		runPrediction(exp, new Double(1), true);
	}

	public static void runPredictionPerClass(Predict exp) throws Exception {
		runPredictionPerClass(exp, new Double(1));
	}

	/**
	 * This version of runPrediction is there for the scenario of AG vs CCG
	 * comparison. The idea is that AG produces a set of Candidates, C_ag and
	 * that CCG produces set C_ccg. C_ag ( C_ccg. But in order to compare the
	 * quality of the syntax an alysis and not the effect of the number of
	 * candidates we might want to run an experiment with CCG analysis on the
	 * pre-identified C_ag candidates only.
	 * 
	 * @param exp
	 * @throws Exception
	 */
	public static void runSpecialPrediction(Predict exp) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		GlobalAlignment kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		if (!exp.precalcPresent()) {
			throw (new UnexpectedDatasetFormat("Please provide path for cached objects"));
		}
		specialTransform(exp, kernel);
		writeObjects(exp, exp.writeObjects);
		WekaHelper.wekaClassify(exp, kernel);
		writePredictions(exp);
		if (exp.isTrainAndTest) {
			evaluate(exp, kernel, true);
		}
		else {
			evaluate(exp, kernel, false);
		}
		exp.getDumper().println("Statistics");
		HashMap<String, String> stringStats = exp.statistics.getStringStats();
		HashMap<String, Double> doubleStats = exp.statistics.getDoubleStats();
		HashMap<String, Integer> integerStats = exp.statistics.getIntegerStats();
		for (String key : stringStats.keySet()) {
			exp.getDumper().println(key + " " + stringStats.get(key));
		}
		for (String key : doubleStats.keySet()) {
			exp.getDumper().println(key + " " + stringStats.get(key));
		}
		for (String key : integerStats.keySet()) {
			exp.getDumper().println(key + " " + stringStats.get(key));
		}
		exp.getDumper().close(exp.outputPath);

	}

	public static void runPrediction(Predict exp, Double percent, Boolean firstpass) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		exp.getDumper().println(new Date().toString() + " Input Parsed, calculating representation");
		SimilarityFunction kernel = null;
		if (exp.fauxkernelconf.getStringValue("functionName").equals("GlobalAlignment")) {
			kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		}
		else if (exp.fauxkernelconf.getStringValue("functionName").equalsIgnoreCase("Dummy")) {
			kernel = new DummyKernel(exp.fauxkernelconf);
		}
		else {
			throw (new UnexpectedDatasetFormat("Unknown functionName"));
		}
		Classifier cModel = null;
		ArrayList<Candidate> referenceCandidates = new ArrayList<Candidate>();

		if (exp.hasReference()) {
			referenceCandidates.addAll(exp.referenceCandidates);
		}
		if (!exp.precalcPresent()) {
			if (exp.labeledCandidates.size() == 0) {
				exp.labeledCandidates = transformToCandidates(exp, exp.labeledCorpus, kernel, true, "labeled");
				referenceCandidates.addAll(exp.labeledCandidates);
				exp.labeledSet = transformToInstances(exp, referenceCandidates, kernel, true, exp.labeledCandidates);

			}
			else if (exp.labeledSet == null) {
				referenceCandidates.addAll(exp.labeledCandidates);
				exp.labeledSet = transformToInstances(exp, referenceCandidates, kernel, true, exp.labeledCandidates);
			}

			if (exp.unlabeledCandidates.size() == 0) {
				exp.unlabeledCandidates = transformToCandidates(exp, exp.unlabeledCorpus, kernel, (false || exp.isTrainAndTest), "unlabeled");
				exp.unlabeledSet = transformToInstances(exp, referenceCandidates, kernel, false, exp.unlabeledCandidates);

			}
			else if (exp.unlabeledSet == null) {
				exp.unlabeledSet = transformToInstances(exp, referenceCandidates, kernel, false, exp.unlabeledCandidates);
			}
			writeObjects(exp, exp.writeObjects);
			// writeObjects(exp, true);

		}
		else {
			filterPreset(exp, percent);
		}
		if (exp.modelPresent() && exp.precalcPresent()) {
			cModel = WekaHelper.wekaClassify(exp.trainedModel, exp, kernel);
		}
		else {
			cModel = WekaHelper.wekaClassify(exp, kernel);
			weka.core.SerializationHelper.write(exp.outputPath + "/weka.model", cModel);
			exp.getDumper().println(new Date().toString() + "Wrote trained model to: " + exp.outputPath + "/weka.model");
		}
		writePredictions(exp);
		if (exp.isTrainAndTest) {
			evaluate(exp, kernel, true);
		}
		else {
			evaluate(exp, kernel, false);
		}
		exp.getDumper().println("Statistics");
		ArrayList<String> stringStatsKeys = new ArrayList<String>(exp.statistics.getStringStats().keySet());
		Collections.sort(stringStatsKeys);
		ArrayList<String> doubleStatsKeys = new ArrayList<String>(exp.statistics.getDoubleStats().keySet());
		Collections.sort(doubleStatsKeys);
		ArrayList<String> integerStatsKeys = new ArrayList<String>(exp.statistics.getIntegerStats().keySet());
		Collections.sort(integerStatsKeys);
		for (String key : stringStatsKeys) {
			exp.getDumper().println(key + "\t" + exp.statistics.getStringStats().get(key));
		}
		for (String key : doubleStatsKeys) {
			exp.getDumper().println(key + "\t" + exp.statistics.getDoubleStats().get(key));
		}
		for (String key : integerStatsKeys) {
			exp.getDumper().println(key + "\t" + exp.statistics.getIntegerStats().get(key));
		}
		System.out.println("#laleled cands= " + exp.labeledCandidates.size());
		System.out.println("#laleled positive cands= " + countPositive(exp.labeledCandidates));
		System.out.println("#unlaleled cands= " + exp.unlabeledCandidates.size());
		System.out.println("#unlaleled positive cands= " + countPositive(exp.unlabeledCandidates));

		exp.getDumper().close(exp.outputPath);

	}

	private static Integer countPositive(ArrayList<Candidate> candidates) {
		Integer count = 0;
		for (Candidate cand : candidates) {
			if (!isNoneOrNull(cand.relation)) {
				count++;
			}
		}
		return count;
	}

	private static Boolean isNoneOrNull(Relation r) {
		if (r == null) return true;
		if (r.type.type.equals("none")) return true;
		return false;
	}

	public static void runPredictionPerClass(Predict exp, Double percent) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		exp.getDumper().println(new Date().toString() + " Input Parsed, calculating representation");
		SimilarityFunction kernel = null;
		if (exp.fauxkernelconf.getStringValue("functionName").equals("GlobalAlignment")) {
			kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		}
		else {
			if (exp.fauxkernelconf.getStringValue("functionName").equalsIgnoreCase("Dummy")) {
				kernel = new DummyKernel(exp.fauxkernelconf);
			}
		}

		exp.perClassLabeledCandidates = transformToCandidatesPerClass(exp, exp.labeledCorpus, kernel, true, "labeled");
		for (String type : exp.perClassLabeledCandidates.keySet()) {
			System.out.println("Labeled for Class: " + type);
			exp.labeledCandidates.addAll(exp.perClassLabeledCandidates.get(type));
			Instances instancesOfType = transformToInstances(exp, exp.perClassLabeledCandidates.get(type), kernel, true, exp.perClassLabeledCandidates.get(type));
			exp.perClassLabeledSet.put(type, instancesOfType);
		}

		exp.perClassUnlabeledCandidates = transformToCandidatesPerClass(exp, exp.unlabeledCorpus, kernel, false, "unlabeled");
		for (String type : exp.perClassUnlabeledCandidates.keySet()) {
			System.out.println("Unlabeled for Class: " + type);
			exp.unlabeledCandidates.addAll(exp.perClassUnlabeledCandidates.get(type));
			Instances instancesOfType = transformToInstances(exp, exp.perClassLabeledCandidates.get(type), kernel, true, exp.perClassUnlabeledCandidates.get(type));
			exp.perClassUnlabeledSet.put(type, instancesOfType);
		}
		WekaHelper.wekaClassifyPerClass(exp, kernel);
		if (exp.isTrainAndTest) {
			evaluate(exp, kernel, true);
		}
		else {
			evaluate(exp, kernel, false);
		}
		exp.getDumper().println("Statistics");
		ArrayList<String> stringStatsKeys = new ArrayList<String>(exp.statistics.getStringStats().keySet());
		Collections.sort(stringStatsKeys);
		ArrayList<String> doubleStatsKeys = new ArrayList<String>(exp.statistics.getDoubleStats().keySet());
		Collections.sort(doubleStatsKeys);
		ArrayList<String> integerStatsKeys = new ArrayList<String>(exp.statistics.getIntegerStats().keySet());
		Collections.sort(integerStatsKeys);
		for (String key : stringStatsKeys) {
			exp.getDumper().println(key + "\t" + exp.statistics.getStringStats().get(key));
		}
		for (String key : doubleStatsKeys) {
			exp.getDumper().println(key + "\t" + exp.statistics.getDoubleStats().get(key));
		}
		for (String key : integerStatsKeys) {
			exp.getDumper().println(key + "\t" + exp.statistics.getIntegerStats().get(key));
		}
		exp.getDumper().close(exp.outputPath);

	}

	private static String filterOtherClasses(String type, String text) {
		String res = "";
		String[] lines = text.split("\n");
		Pattern tp = Pattern.compile(type);
		Pattern zerop = Pattern.compile("0");
		for (String line : lines) {
			if (tp.matcher(line).find() || !zerop.matcher(line).find()) {
				res += "\n" + line;
			}
		}
		return res;
	}

	public static TreeMap<String, Double[]> runCrossValidation(CrossValidation exp) throws Exception {
		TreeMap<String, Double[]> allStats = new TreeMap<>();
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		exp.getDumper().println(new Date().toString() + " Input Parsed, calculating representation (PER CLASS)");
		SimilarityFunction kernel = null;
		if (exp.fauxkernelconf.getStringValue("functionName").equals("GlobalAlignment")) {
			kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		}
		else if (exp.fauxkernelconf.getStringValue("functionName").equalsIgnoreCase("Dummy")) {
			kernel = new DummyKernel(exp.fauxkernelconf);
		}
		else {
			throw (new UnexpectedDatasetFormat("Unknown functionName"));
		}
		ArrayList<Candidate> referenceCandidates = new ArrayList<Candidate>();
		if (exp.hasReference()) {
			referenceCandidates.addAll(exp.referenceCandidates);
		}
		/*
		 * The two if clauses below is here to avoid re-calculating
		 * transformation in case of parameter optimization
		 */
		if (exp.candidates.isEmpty()) {
			exp.candidates = transformToCandidates(exp, exp.corpus, kernel, true, "labeled");
			referenceCandidates.addAll(exp.candidates);

		}

		exp.perClassCandidates = transformToCandidatesPerClass(exp, exp.corpus, kernel, true, "all");
		ArrayList<Candidate> allCandidates = new ArrayList<Candidate>();
		for (String t:exp.perClassCandidates.keySet()) {
			allCandidates.addAll(exp.perClassCandidates.get(t));
		}
		exp.candidates = allCandidates;
		for (String type : exp.perClassCandidates.keySet()) {
			if (exp.selectedRelationTypes.contains(type)) {
				System.out.println("\nCV for Class: " + type);
				exp.candidates.addAll(exp.perClassCandidates.get(type));
				if (exp.perClass) {
					Instances instancesOfType = transformToInstances(exp, exp.perClassCandidates.get(type), kernel, true, exp.perClassCandidates.get(type));
					exp.perClassSet.put(type, instancesOfType);
				}
				else {
					Instances instancesOfType = transformToInstances(exp, exp.candidates, kernel, true, exp.perClassCandidates.get(type));
					exp.perClassSet.put(type, instancesOfType);
					if (exp.set == null) {
						// exp.set = transformToInstances(exp,
						// referenceCandidates, kernel, true,
						// exp.candidates);
						exp.set = instancesOfType;
					}
					else {
						exp.set.addAll(instancesOfType);
					}
				}
			}
		}
		writeObjects(exp, exp.writeObjects);
		Classifier cModel = exp.wekaClassifier;
		exp.getDumper().println("\nClassifier options :" + Arrays.toString(exp.wekaClassifier.getOptions()));

		if (exp.perClass) {
			HashMap<String, ArrayList<weka.classifiers.Evaluation>> evals = WekaHelper.wekaCrossValidatePerClass(exp, kernel);
			String checkModel = cModel.toString();
			if (!checkModel.equalsIgnoreCase("FilteredClassifier: No model built yet.")) {
				weka.core.SerializationHelper.write(exp.outputPath + "/weka.model", cModel);
				exp.getDumper().println(new Date().toString() + "Wrote trained model to: " + exp.outputPath + "/weka.model");
			}
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " done!");
			for (String type : exp.perClassCandidates.keySet()) {
				if (exp.selectedRelationTypes.contains(type)) {
					ArrayList<weka.classifiers.Evaluation> typeEvals = evals.get(type);
					int i = 1;
					for (weka.classifiers.Evaluation cross : typeEvals) {
						exp.getDumper().println("Run " + i);
						i++;
						exp.getDumper().println(filterOtherClasses(type, cross.toClassDetailsString()));
						exp.getDumper().flush();
						PrintStuff.printLabeledClassStats(exp.getDumper(), Preparation.getCandidatesClasses(exp.perClassCandidates.get(type)), exp.schema.getRelationTypesByName(type));
						exp.getDumper().flush();
						PrintStuff.printWeightedAvgClassFromWeka(exp.getDumper(), cross, Preparation.getCandidatesClasses(exp.perClassCandidates.get(type)), exp.schema.DefinedRelationTypes);
					}
					exp.getDumper().println("Summary for type " + type + ":");
					Double[] newStats = PrintStuff
							.printWeightedAvgClassFromWeka(exp.getDumper(), typeEvals, Preparation.getCandidatesClasses(exp.perClassCandidates.get(type)), exp.schema.DefinedRelationTypes)
							.get("Weighted Average");
					allStats.put(type, newStats);
				}
			}
			PrintStuff.printRecap(allStats, "for all classes", exp.getDumper());
			exp.getDumper().flush();
			exp.getDumper().close(exp.outputPath);
		}

		else { // not per Class
			ArrayList<weka.classifiers.Evaluation> evals = WekaHelper.wekaCrossValidate(exp, kernel);
			String checkModel = cModel.toString();
			if (!checkModel.equalsIgnoreCase("FilteredClassifier: No model built yet.")) {
				weka.core.SerializationHelper.write(exp.outputPath + "/weka.model", cModel);
				exp.getDumper().println(new Date().toString() + "Wrote trained model to: " + exp.outputPath + "/weka.model");
			}
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " done!");
			int i = 1;
			for (weka.classifiers.Evaluation cross : evals) {
				exp.getDumper().println("Run " + i);
				i++;
				exp.getDumper().println(cross.toClassDetailsString());
				exp.getDumper().flush();
				PrintStuff.printLabeledClassStats(exp.getDumper(), Preparation.getCandidatesClasses(exp.candidates), exp.schema.DefinedRelationTypes);
				exp.getDumper().flush();
				PrintStuff.printWeightedAvgClassFromWeka(exp.getDumper(), cross, Preparation.getCandidatesClasses(exp.candidates), exp.schema.DefinedRelationTypes);

			}

			HashMap<String, Double[]> newStats = PrintStuff.printWeightedAvgClassFromWeka(exp.getDumper(), evals, Preparation.getCandidatesClasses(exp.candidates), exp.schema.DefinedRelationTypes);
			allStats.putAll(newStats);
			PrintStuff.printRecap(allStats, "summary for this classification task", exp.getDumper());
			exp.getDumper().flush();
			exp.getDumper().close(exp.outputPath);
		}
		return allStats;
	}

	

	/**
	 * We're using MultiExperiment to run a cross validation with evaluation by
	 * an evaluation script and not Weka
	 * 
	 * @param multi
	 * @throws Exception
	 */
	public static void runCrossValidationByScript(MultipleExperiment multi) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		multi.setDumper(new Dumper(multi.outputPath + "/" + runID + ".log", multi.config.getFile()));
		Integer folds = multi.folds;
		Integer runs = multi.runs;
		if (multi.type.equalsIgnoreCase("crossvalidation")) {
			Predict exp = (Predict) multi.experiment;
			exp.setDumper(multi.getDumper());
			for (int i = 0; i < runs; i++) {
				multi.getDumper().reopen();
				multi.getDumper().println("Run " + i);
				Predict runExp = new Predict(multi);
				runExp.setOutputPath(runExp.outputPath + "/run" + i + "/");
				String unlabeledPath = runExp.outputPath + "unlabeled/";
				runExp.unlabeledPath = unlabeledPath;
				File unlabeledDir = new File(unlabeledPath);
				unlabeledDir.mkdirs();
				FileUtils.cleanDirectory(unlabeledDir);
				HashMap<String, Corpus> corpora = splitCorpus(runExp.labeledCorpus, folds);
				runExp.labeledCorpus = corpora.get("train");
				runExp.unlabeledCorpus = corpora.get("test");
				for (Document d : corpora.get("test").documents) {
					String orig_filename = runExp.labeledPath + d.id + ".a";
					File orig_file = new File(orig_filename);
					if (orig_file.exists()) {
						FileUtils.copyFileToDirectory(orig_file, unlabeledDir);
					}
					else {
						throw (new UnexpectedDatasetFormat(orig_filename + "  not found. You did wrong!"));
					}
				}
				runPrediction(runExp, new Double(1), true);
			}

		}
		else {
			throw (new UnexpectedDatasetFormat("unknown experiment type for multi! (this should never happen, but you never know..."));

		}

	}

	/**
	 * We're using MultiExperiment to run a cross validation with evaluation by
	 * an evaluation script and not Weka
	 * 
	 * @param multi
	 * @throws Exception
	 */
	public static void runCrossValidationByAlvisRE(MultipleExperiment multi) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		multi.setDumper(new Dumper(multi.outputPath + "/" + runID + ".log", multi.config.getFile()));
		Integer folds = multi.folds;
		Integer runs = multi.runs;
		if (multi.type.equalsIgnoreCase("crossvalidation")) {
			Predict exp = (Predict) multi.experiment;
			exp.setDumper(multi.getDumper());
			for (int i = 0; i < runs; i++) {

				// String unlabeledPath = runExp.outputPath+"unlabeled/";
				// runExp.unlabeledPath = unlabeledPath;
				// File unlabeledDir = new File(unlabeledPath);
				// unlabeledDir.mkdirs();
				// FileUtils.cleanDirectory(unlabeledDir);
				// HashMap<String, Corpus> corpora =
				// splitCorpus(runExp.labeledCorpus, folds);
				// runExp.labeledCorpus = corpora.get("train");
				// runExp.unlabeledCorpus = corpora.get("test");
				// for (Document d: corpora.get("test").documents) {
				// String orig_filename = runExp.labeledPath+d.id+".a";
				// File orig_file = new File(orig_filename);
				// if (orig_file.exists()){
				//// FileUtils.copyFileToDirectory(orig_file, unlabeledDir);
				// }
				// else {
				// throw (new UnexpectedDatasetFormat(orig_filename+" not found.
				// You did wrong!"));
				// }
				// }
				// runExp.l
				HashMap<Integer, ArrayList<Instances>> foldData = WekaHelper.splitForCVRun(exp.labeledSet, i, folds);
				for (int j = 0; j < folds; j++) {
					multi.getDumper().reopen();
					multi.getDumper().println(new Date().toString() + "Run " + i);
					Predict runExp = new Predict(multi);
					runExp.setOutputPath(runExp.outputPath + "/run" + i + "/fold" + j + "/");
					// runExp.labeledCandidates = foldData.get(j).get(0);
					runPrediction(runExp, new Double(1), true);
				}

			}

		}
		else {
			throw (new UnexpectedDatasetFormat("unknown experiment type for multi! (this should never happen, but you never know..."));

		}

	}

	public static void runNeighbourSearch(NearestNeighbours exp) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		exp.getDumper().println(new Date().toString() + " Input Parsed, calculating representation");
		SimilarityFunction kernel = null;
		if (exp.fauxkernelconf.getStringValue("functionName").equals("GlobalAlignment")) {
			kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		}
		else {
			if (exp.fauxkernelconf.getStringValue("functionName").equalsIgnoreCase("Dummy")) {
				kernel = new DummyKernel(exp.fauxkernelconf);
			}
		}
		if (!exp.precalcPresent()) {
			if (!exp.precalcAllPresent()) {
				exp.allCandidates = transformToCandidates(exp, exp.allCorpus, kernel, exp.allLabeled, "all");
				exp.allSet = transformToInstances(exp, exp.allCandidates, kernel, exp.allLabeled, exp.allCandidates);
			}
			else {

			}
			if (!exp.precalcTargetPresent()) {
				exp.targetCandidates = transformToCandidates(exp, exp.targetCorpus, kernel, exp.targetLabeled, "target");
				exp.targetSet = transformToInstances(exp, exp.allCandidates, kernel, exp.targetLabeled, exp.targetCandidates);
			}
		}
		HashMap<Instance, Instances> targetInstancesWithNeighbours = NeighbourSearch.runNNsearchForInstances(exp.allSet, exp.targetSet, exp.k);
		exp.targetCandidatesWithNeighbours = WekaHelper.transformToCandidates(targetInstancesWithNeighbours, exp.targetCandidates, exp.allCandidates);
		PrintStuff.printNeighbours(exp.getDumper(), exp.targetCandidatesWithNeighbours);
		exp.getDumper().close(exp.outputPath);
	}

	public static void runNeighbourSearchWithClassFilters(NearestNeighbours exp) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		exp.getDumper().println(new Date().toString() + " Input Parsed, calculating representation");
		SimilarityFunction kernel = null;
		if (exp.fauxkernelconf.getStringValue("functionName").equals("GlobalAlignment")) {
			kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		}
		else {
			if (exp.fauxkernelconf.getStringValue("functionName").equalsIgnoreCase("Dummy")) {
				kernel = new DummyKernel(exp.fauxkernelconf);
			}
			else {
				throw (new UnexpectedDatasetFormat("your functionName is wrong!"));
			}
		}
		if (!exp.precalcPresent()) {
			if (!exp.precalcAllPresent()) {
				exp.allCandidates = transformToCandidates(exp, exp.allCorpus, kernel, exp.allLabeled, "all");
			}
			else {

			}
			if (!exp.precalcTargetPresent()) {
				exp.targetCandidates = transformToCandidates(exp, exp.targetCorpus, kernel, exp.targetLabeled, "target");
			}
		}

		ArrayList<Candidate> subsetTargets = new ArrayList<Candidate>();
		if (exp.targetLabeled) {
			// subsetTargets =
			// removeDuplicates(filterNegativeCandidates(exp.targetCandidates
			// ));
			subsetTargets = exp.targetCandidates;

		}
		else {
			subsetTargets = exp.targetCandidates;

		}
		HashMap<Instance, Instances> targetInstancesWithNeighbours = new HashMap<Instance, Instances>();

		for (Candidate target : subsetTargets) {
			ArrayList<Candidate> subsetCandidateNeighbours = removeDuplicates(filterSameSentence(filterByCandidateRelationType(exp.allCandidates, target.candidateRelationType.type), target));
			Instances subsetNeighbourInstances = transformToInstances(exp, exp.allCandidates, kernel, exp.targetLabeled, subsetCandidateNeighbours);
			Instance targetInstance = transformToInstance(exp, kernel, exp.targetLabeled, target);
			Instances neighbourInstances = removeDuplicates(NeighbourSearch.runNNsearchForInstancesPerClass(subsetNeighbourInstances, targetInstance, exp.k));
			targetInstancesWithNeighbours.put(targetInstance, neighbourInstances);
		}

		exp.targetCandidatesWithNeighbours = filterNegativeCandidatesWithoutPositiveNeighbours(
				WekaHelper.transformToCandidates(targetInstancesWithNeighbours, exp.targetCandidates, exp.allCandidates));
		PrintStuff.printNeighbours(exp.getDumper(), exp.targetCandidatesWithNeighbours);
		exp.getDumper().close(exp.outputPath);
	}

	public static void runNeighbourSearchWithoutWeka(NearestNeighbours exp) throws Exception {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		exp.getDumper().println(new Date().toString() + " Input Parsed, calculating representation");
		SimilarityFunction kernel = null;
		if (exp.fauxkernelconf.getStringValue("functionName").equals("GlobalAlignment")) {
			kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		}
		else {
			if (exp.fauxkernelconf.getStringValue("functionName").equalsIgnoreCase("Dummy")) {
				kernel = new DummyKernel(exp.fauxkernelconf);
			}
		}
		exp.allCandidates = transformToCandidates(exp, exp.allCorpus, kernel, exp.allLabeled, "all");
		exp.targetCandidates = transformToCandidates(exp, exp.targetCorpus, kernel, exp.targetLabeled, "target");
		EmpiricalKernelMap ekm = new EmpiricalKernelMap(exp.allCandidates, kernel);
		exp.targetCandidatesWithNeighbours = NeighbourSearch.runNNsearchForCandidatesUsingEuclideanDistance(exp.allCandidates, exp.targetCandidates, exp.k, ekm);
		PrintStuff.printNeighbours(exp.getDumper(), exp.targetCandidatesWithNeighbours);
		exp.getDumper().close(exp.outputPath);

	}

	public static void extractCandidates(ExtractCandidates exp) throws IOException {
		String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
		if (exp.getDumper() == null) exp.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", exp.config.getFile()));
		SerializeMe serial = new SerializeMe(exp.outputPath + "/all/");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Calculating representation as Shortest Paths on the Dependency Graph");
		ArrayList<Candidate> candidates = Preparation.prepareRepresentation("Candidates", exp.corpus, exp, true, false);
		serial.writeCandidates(candidates, false);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Wrote Candidates");

	}

	private static Boolean keep(Double percent) {
		Double num = Math.random();
		return (num.compareTo(percent) <= 0.0);
	}

	private static Boolean toKeep(Instance instance, ArrayList<Candidate> candidates) {
		for (Candidate cand : candidates) {
			String iPredIndex = instance.stringValue(0);
			if (iPredIndex.equals(cand.index)) {
				return true;
			}
		}
		return false;
	}

	private static HashMap<String, Corpus> splitCorpus(Corpus corpus, Integer folds) {
		HashMap<String, Corpus> trainAndTest = new HashMap<String, Corpus>();
		ArrayList<Document> documents = new ArrayList<>();
		documents.addAll(corpus.documents);
		Collections.shuffle(documents);
		Integer partSize = documents.size() / folds;
		ArrayList<Document> test = new ArrayList<>();
		test.addAll(documents.subList(0, partSize));
		ArrayList<Document> train = new ArrayList<>();
		train.addAll(documents.subList(partSize + 1, documents.size() - 1));
		trainAndTest.put("train", new Corpus(train));
		trainAndTest.put("test", new Corpus(test));
		return trainAndTest;
	}

	private static Remove filterInstances(Instances set, ArrayList<Candidate> filteredCandidates) throws Exception {
		ArrayList<Integer> attributes2remove = new ArrayList<Integer>();
		for (int i = 0; i < set.numInstances(); i++) {
			if (!toKeep(set.get(i), filteredCandidates)) {
				set.remove(i);
				attributes2remove.add(i + 1);
			}
		}
		Remove remove = new Remove();
		int[] attrs = new int[attributes2remove.size()];
		attrs = Ints.toArray(attributes2remove);
		remove.setAttributeIndicesArray(attrs);
		remove.setInputFormat(set);
		return remove;
	}

	private static ArrayList<Candidate> filterCandidates(ArrayList<Candidate> candidates, Double percent) {
		if (percent.equals(new Double(1.0))) return candidates;
		ArrayList<Candidate> remainingCandidates = new ArrayList<Candidate>();
		for (Candidate candidate : candidates) {
			if (keep(percent)) remainingCandidates.add(candidate);
		}
		return remainingCandidates;
	}

	private static HashMap<String, ArrayList<Candidate>> filterCandidates(HashMap<String, ArrayList<Candidate>> candidates, Double percent) {
		if (percent.equals(new Double(1.0))) return candidates;
		HashMap<String, ArrayList<Candidate>> remainingCandidates = new HashMap<String, ArrayList<Candidate>>();
		for (String type : candidates.keySet()) {
			ArrayList<Candidate> remainingCandidatesOfType = new ArrayList<Candidate>();
			for (Candidate candidate : candidates.get(type)) {
				if (keep(percent)) remainingCandidatesOfType.add(candidate);
			}
			remainingCandidates.put(type, remainingCandidatesOfType);
		}
		return remainingCandidates;
	}

	private static HashMap<Candidate, ArrayList<Candidate>> filterNegativeCandidatesWithoutPositiveNeighbours(HashMap<Candidate, ArrayList<Candidate>> table) {
		HashMap<Candidate, ArrayList<Candidate>> filteredTable = new HashMap<Candidate, ArrayList<Candidate>>();
		for (Candidate candidate : table.keySet()) {
			if (candidate.relation != null) {
				if (!candidate.relation.type.type.equals("none")) {
					filteredTable.put(candidate, table.get(candidate));
				}
				else {
					Boolean hasNonNegativeNeighbours = false;
					for (Candidate neighbour : table.get(candidate)) {
						if (neighbour.relation != null) {
							if (!neighbour.relation.type.type.equals("none")) {
								hasNonNegativeNeighbours = true;
							}
						}
						else {
							hasNonNegativeNeighbours = true;
						}

					}
					if (hasNonNegativeNeighbours) {
						filteredTable.put(candidate, table.get(candidate));
					}
				}
			}
			else {
				filteredTable.put(candidate, table.get(candidate));
			}
		}
		return filteredTable;
	}

	private static ArrayList<Candidate> filterByCandidateRelationType(ArrayList<Candidate> candidates, String type) {
		ArrayList<Candidate> filteredCandidates = new ArrayList<Candidate>();
		for (Candidate candidate : candidates) {
			if (candidate.candidateRelationType != null) {
				if (candidate.candidateRelationType.type.equals(type)) {
					filteredCandidates.add(candidate);
				}
			}
		}

		return filteredCandidates;
	}

	private static ArrayList<Candidate> filterSameSentence(ArrayList<Candidate> candidates, Candidate target) {

		ArrayList<Candidate> filteredCandidates = new ArrayList<>();
		Set<Candidate> candidateTree = new TreeSet<Candidate>(new Comparator<Candidate>() {

			@Override
			public int compare(Candidate c1, Candidate c2) {
				return c1.sentence.id.compareTo(c2.sentence.id);
			}
		});
		candidateTree.addAll(candidates);
		filteredCandidates.addAll(candidateTree);
		return filteredCandidates;

	}

	private static HashMap<String, ArrayList<Candidate>> removeDuplicates(HashMap<String, ArrayList<Candidate>> candidates) {
		HashMap<String, ArrayList<Candidate>> filteredCandidates = new HashMap<String, ArrayList<Candidate>>();
		for (String type : candidates.keySet()) {
			ArrayList<Candidate> filteredCandidatesOfType = new ArrayList<Candidate>();
			Set<Candidate> candidateTree = new TreeSet<Candidate>(new Comparator<Candidate>() {
				@Override
				public int compare(Candidate c1, Candidate c2) {
					return c1.index.compareTo(c2.index);
				}
			});
			candidateTree.addAll(candidates.get(type));
			filteredCandidatesOfType.addAll(candidateTree);
			filteredCandidates.put(type, filteredCandidatesOfType);

		}
		return filteredCandidates;
	}

	private static ArrayList<Candidate> removeDuplicates(ArrayList<Candidate> candidates) {
		ArrayList<Candidate> filteredCandidates = new ArrayList<>();
		Set<Candidate> candidateTree = new TreeSet<Candidate>(new Comparator<Candidate>() {

			@Override
			public int compare(Candidate c1, Candidate c2) {
				return c1.index.compareTo(c2.index);
			}
		});
		candidateTree.addAll(candidates);
		filteredCandidates.addAll(candidateTree);
		return filteredCandidates;
	}

	private static Instances removeDuplicates(Instances instances) {
		Instances filteredInstances = new Instances(instances, instances.size());
		Set<Instance> instanceTree = new TreeSet<Instance>(new InstanceComparator(true));
		instanceTree.addAll(instances);
		filteredInstances.addAll(instanceTree);
		return filteredInstances;
	}

	private static Instance transformToInstance(NearestNeighbours exp, SimilarityFunction kernel, Boolean targetLabeled, Candidate candidate) throws Exception {
		EmpiricalKernelMap ekm = new EmpiricalKernelMap(exp.allCandidates, kernel);
		ArrayList<Candidate> labeled = exp.allCandidates;
		ArrayList<Attribute> wekavector = WekaHelper.wekaBuildFeatureVector(exp, kernel, exp.allCandidates.size(), labeled, false);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " >> Calculating distances for the subset dataset (This can take time).");
		ArrayList<Candidate> candidates = new ArrayList<>();
		candidates.add(candidate);
		Instances instances = WekaHelper.wekaSet(exp.verbose, exp.allCandidates.size(), wekavector, kernel, ekm, candidates, Preparation.getCandidatesClasses(candidates), exp.threads,
				exp.hasTriggerWords);
		return instances.get(0);

	}

	private static ArrayList<Candidate> transformToCandidates(Experiment exp, Corpus sourceCorpus, SimilarityFunction kernel, Boolean isLabeled, String desc) throws Exception {
		return transformToCandidates(exp, sourceCorpus, kernel, isLabeled, desc, new Double(1.0));
	}

	private static ArrayList<Candidate> transformToCandidates(Experiment exp, Corpus sourceCorpus, SimilarityFunction kernel, Boolean isLabeled, String desc, Double percent) throws Exception {
		ArrayList<Candidate> destinationCandidates = new ArrayList<Candidate>();
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Calculating representation as Shortest Paths on the Dependency Graph [Target Candidates]");
		destinationCandidates = filterCandidates(Preparation.prepareRepresentation(desc, sourceCorpus, exp, isLabeled, false), percent);
		destinationCandidates = removeDuplicates(destinationCandidates);
		return destinationCandidates;
	}

	private static HashMap<String, ArrayList<Candidate>> transformToCandidatesPerClass(Experiment exp, Corpus sourceCorpus, SimilarityFunction kernel, Boolean isLabeled, String desc)
			throws Exception {
		return transformToCandidatesPerClass(exp, sourceCorpus, kernel, isLabeled, desc, new Double(1.0));
	}

	private static HashMap<String, ArrayList<Candidate>> transformToCandidatesPerClass(Experiment exp, Corpus sourceCorpus, SimilarityFunction kernel, Boolean isLabeled, String desc, Double percent)
			throws Exception {
		HashMap<String, ArrayList<Candidate>> destinationCandidates = new HashMap<String, ArrayList<Candidate>>();
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Calculating representation as Shortest Paths on the Dependency Graph [Target Candidates]");
		destinationCandidates = filterCandidates(Preparation.prepareRepresentationPerClass(desc, "ShortestDependencyPath", sourceCorpus, exp, isLabeled, false), percent);
		destinationCandidates = removeDuplicates(destinationCandidates);
		return destinationCandidates;
	}

	private static Instances transformToInstances(Experiment exp, ArrayList<Candidate> referenceCandidates, SimilarityFunction kernel, Boolean isLabeled, ArrayList<Candidate> candidates)
			throws Exception {
		EmpiricalKernelMap ekm = new EmpiricalKernelMap(referenceCandidates, kernel);
		ArrayList<Candidate> labeled = new ArrayList<Candidate>();
		if (exp instanceof Predict) {
			labeled = ((Predict) exp).labeledCandidates;
		}
		if (exp instanceof CrossValidation) {
			labeled = ((CrossValidation) exp).candidates;
		}
		ArrayList<Attribute> wekavector = WekaHelper.wekaBuildFeatureVector(exp, kernel, referenceCandidates.size(), labeled, false);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " >> Calculating distances for the subset dataset (This can take time).");
		return WekaHelper.wekaSet(exp.verbose, referenceCandidates.size(), wekavector, kernel, ekm, candidates, Preparation.getCandidatesClasses(candidates), exp.threads, exp.hasTriggerWords);
	}

	/**
	 * For the specificities of this "special" version see specialTransform
	 * 
	 * @param exp
	 * @param kernel
	 * @throws Exception
	 */
	private static void specialTransform(Predict exp, GlobalAlignment kernel) throws Exception {
		// todo
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Calculating representation as Shortest Paths on the Dependency Graph [TRAINING]");
		exp.labeledCandidates = Preparation.specialPrepareRepresentation("ShortestDependencyPath", exp.labeledCorpus, exp, exp.labeledCandidates, true);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Calculating representation as Shortest Paths on the Dependency Graph [TEST]");
		exp.unlabeledCandidates = Preparation.specialPrepareRepresentation("ShortestDependencyPath", exp.unlabeledCorpus, exp, exp.unlabeledCandidates, false);
		// todo
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + "  Preparing transformation in weka instances");
		EmpiricalKernelMap ekm = new EmpiricalKernelMap(exp.labeledCandidates, kernel);
		ArrayList<Attribute> wekavector = WekaHelper.wekaBuildFeatureVector(exp, kernel, exp.labeledCandidates.size(), false);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " >> Calculating distances for the labeled(training) dataset (This can take time).");
		exp.labeledSet = WekaHelper.wekaSet(exp.verbose, exp.labeledCandidates.size(), wekavector, kernel, ekm, exp.labeledCandidates, Preparation.getCandidatesClasses(exp.labeledCandidates),
				exp.threads, exp.hasTriggerWords);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " >> Map calculated for training.");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " >> Calculating distances for the unlabeled(test) dataset (This can take time).");
		exp.unlabeledSet = WekaHelper.wekaSet(exp.verbose, exp.labeledCandidates.size(), wekavector, kernel, ekm, exp.unlabeledCandidates, new HashMap<String, String>(), exp.hasTriggerWords);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " >> Map calculated for test.");
	}

	private static void filterPreset(Predict exp, Double percent) throws Exception {
		if (!percent.equals(new Double(1.0))) {
			Integer lalbeledInitsize = exp.labeledCandidates.size();
			Integer unlalbeledInitsize = exp.unlabeledCandidates.size();
			exp.labeledCandidates = filterCandidates(exp.labeledCandidates, percent);
			exp.unlabeledCandidates = filterCandidates(exp.unlabeledCandidates, percent);
			Remove remove = filterInstances(exp.labeledSet, exp.labeledCandidates);
			filterInstances(exp.unlabeledSet, exp.unlabeledCandidates);
			exp.unlabeledSet = Filter.useFilter(exp.unlabeledSet, remove);
			exp.labeledSet = Filter.useFilter(exp.labeledSet, remove);
			exp.getDumper()
					.println("-----------------------------------------------------------------------------\n" + "Filtering at " + percent + ". Candidates kept: " + exp.labeledCandidates.size()
							+ " out of " + lalbeledInitsize + " and " + exp.unlabeledCandidates.size() + " out of " + unlalbeledInitsize + " test \n"
							+ "-----------------------------------------------------------------------------");
			ArffWriter arffwriter = new ArffWriter(exp.outputPath);
			arffwriter.writeArff(exp.labeledSet, "labeled");
			arffwriter.writeArff(exp.unlabeledSet, "unlabeled");
		}

	}

	private static void writePredictions(Predict exp) throws IOException {
		ArffWriter arffwriter = new ArffWriter(exp.outputPath);
		arffwriter.writeArff(exp.predictedSet, "predictions");
		if (exp.writeObjects) {
			SerializeMe serialPredicted = new SerializeMe(exp.outputPath + "/predicted/");
			serialPredicted.writeCandidates(exp.predictedCandidates);
		}
	}

	private static void evaluate(Predict exp, SimilarityFunction kernel, Boolean withAnswers) throws IOException, InterruptedException {
		OutputModule bioutput = null;
		if (exp.inputType.equals("BI")) {
			bioutput = new BIo(exp.outputPath);
		}
		else if (exp.outputFormat.equals("Challenge") || exp.outputFormat.equals("AlvisNLP")) {
			bioutput = new AlvisNLPo(exp.outputPath);
		}

		for (Document doc : exp.unlabeledCorpus.documents) {
			for (Sentence s : doc.sentences) {
				if ((exp.verbose > 2) && withAnswers) {
					exp.getDumper().println("------");
					PrintStuff.printSentenceAndCandidatesTextAndID(exp.getDumper(), s);
					PrintStuff.printRelations(s, exp.getDumper(), "Actual Relations:");
				}
				s.updateRelations(exp.predictedCandidates);
				if ((exp.verbose > 2)) {
					PrintStuff.printRelations(s, exp.getDumper(), "Predicted Relations:");
				}
			}
			ArrayList<Sentence> sentences = new ArrayList<>();
			sentences.addAll(doc.sentences);
			if (exp.inputType.equals("BI")) {
				((BIo) bioutput).writeFiles(sentences);
			}
		}
		if (withAnswers) {
			exp.getDumper().flush();
			exp.getDumper().println("SCORES_____________");
			if (exp.verbose > 0) {
				exp.getDumper().println("Candidate Level Scores (Weka or other):");
				PrintStuff.givePredictionStats(exp.getDumper(), exp.predictedCandidates, exp.unlabeledCandidates, exp.schema.DefinedRelationTypes);
			}
			if (exp.inputType.equals("BI")) {
				exp.getDumper().println("Challenge Evaluation Tool (BI)");
				exp.getDumper().close();
				String[] biargs = { exp.referencePath, exp.outputPath };
				PrintStream ps = exp.getDumper().getPrintStream();
				PrintStream original = new PrintStream(System.out);
				org.bibliome.interaction.eval.EvaluateInteractions.main(biargs);
				System.setOut(ps);
				org.bibliome.interaction.eval.EvaluateInteractions.main(biargs);
				System.setOut(original);
				ps.flush();
				ps.close();

			}
			else if (exp.outputFormatChallenge.equals("biotopes")) {
				exp.getDumper().println("Challenge Evaluation Tool (Biotopes)");
				exp.getDumper().close();
				Double[] stats = Evaluation.evaluateBB(exp.evaluationScript, exp.unlabeledPath, exp.txtPath, exp.outputPath, exp.referencePath);
				exp.getDumper().reopen();
				exp.getDumper().println("Precision " + stats[0] + " Recall " + stats[1] + " F-measure " + stats[2]);
			}
			exp.getDumper().reopen();
		}
		else {
			if (exp.outputFormat != null) {
				if (exp.outputFormat.equalsIgnoreCase("Challenge") || exp.outputFormat.equalsIgnoreCase("Both")) {
					if (exp.outputFormatChallenge.equalsIgnoreCase("LLL")) {
						LLL llloutput = new LLL(exp.outputPath, exp.LLLout);
						if (exp.LLLtest != null) {
							llloutput.writeFile(exp.unlabeledCorpus.documents, exp.LLLtest);
						}
						else {
							llloutput.writeFile(exp.unlabeledCorpus.documents);
						}
						exp.getDumper().println(new Date().toString() + " Wrote io.output");
					}
					if (exp.outputFormatChallenge.equalsIgnoreCase("biotopes") || exp.outputFormatChallenge.equalsIgnoreCase("seedev")) {
						bioutput = new AlvisNLPo(exp.outputPath);
						for (Document doc : exp.unlabeledCorpus.documents) {
							for (Sentence s : doc.sentences) {
								if (exp.verbose > 2) {
									PrintStuff.printRelations(s, exp.getDumper(), "Predicted Relations:");
								}
							}
						}
						String prefix = "";
						if (exp.outputFormatChallenge.equalsIgnoreCase("seedev")) prefix = "SeeDev-binary-";
						((AlvisNLPo) bioutput).writeFiles(exp.unlabeledCorpus.documents, exp.writeEntities, prefix);
						if (exp.outputFormatChallenge.equalsIgnoreCase("seedev")) {
							((AlvisNLPo) bioutput).a2Zip();
						}
						else {
						((AlvisNLPo) bioutput).a2TarGZip();
						}
						exp.getDumper().println("wrote files to " + bioutput.outputDir + " and tgz to " + ((AlvisNLPo) bioutput).tgz);
						exp.getDumper().flush();
					}

				}
			}
			if (exp.verbose > 2) System.out.println("TRAINING CANDIDATES____________________________________________\n" + "____________________________________________");

			if (exp.verbose > 2) for (Document doc : exp.labeledCorpus.documents) {
				for (Sentence sentence : doc.sentences) {
					PrintStuff.printSentenceAndCandidatesTextAndID(exp.getDumper(), sentence);
					PrintStuff.printRelations(sentence, exp.getDumper(), "Actual Relations:");
				}
			}
			if (exp.verbose > 2) System.out.println("TEST CANDIDATES________________________________________________\n____________________________________________");

			if (exp.verbose > 2) for (Document doc : exp.unlabeledCorpus.documents) {
				for (Sentence sentence : doc.sentences) {
					PrintStuff.printSentenceAndCandidatesTextAndID(exp.getDumper(), sentence);
					PrintStuff.printRelations(sentence, exp.getDumper(), "Predicted Relations: ");
					// exp.getDumper().println(new Date().toString()+"
					// Done");
				}
			}
			if (kernel instanceof GlobalAlignment) {
				PrintStuff.printDepdendencySimilarities(((GlobalAlignment) kernel).getDependencyMap(), exp.getDumper());

			}
			if (exp.outputFormat != null) {
				if (exp.outputFormat.equals("Challenge")) {
					if (exp.outputFormatChallenge.equals("LLL")) exp.getDumper().println(tools.PostToLLL.postToLLLContest(new File(exp.outputPath + "/" + exp.LLLout), exp.LLLout));
					if (exp.outputFormatChallenge.equalsIgnoreCase("biotopes")) {
						String tgz = exp.outputPath + "/pred.tgz";
						File tgzfile = new File(tgz);
						exp.getDumper().println(tools.PostToBB.postToBBContest(tgzfile, tgz, exp.bionlpTask));
					}

				}
			}
			else throw new UnexpectedDatasetFormat("outputFormat not specified.");
		}

	}

	private static void writeObjects(Predict exp, Boolean serial) throws IOException {
		if (serial) {
			final SerializeMe serialLabeled = new SerializeMe(exp.outputPath + "/labeled/");
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Saving serialized labeled  candidates");
			serialLabeled.writeCandidates(exp.labeledCandidates);
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Written");
			final SerializeMe serialUnlabeled = new SerializeMe(exp.outputPath + "/unlabeled/");
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Saving serialized unlabeled candidates");
			serialUnlabeled.writeCandidates(exp.unlabeledCandidates);
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + "Written");
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Writing the arff for later use");
			ArffWriter arffwriter = new ArffWriter(exp.outputPath);
			arffwriter.writeArff(exp.labeledSet, "labeled");
			arffwriter.writeArff(exp.unlabeledSet, "unlabeled");
		}
	}

	private static void writeObjects(CrossValidation exp, Boolean serial) throws IOException {
		if (serial) {
			final SerializeMe serialLabeled = new SerializeMe(exp.outputPath + "/");
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Saving serialized  candidates (before CV)");
			serialLabeled.writeCandidates(exp.candidates);
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Written");
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString() + " Writing the arff (before CV) for later use");
			ArffWriter arffwriter = new ArffWriter(exp.outputPath);
			arffwriter.writeArff(exp.set, "cv");
		}
	}
}
