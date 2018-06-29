package experiments;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import corpus.ID;
import corpus.Relation;
import corpus.RelationType;
import corpus.Schema;
import exceptions.UnexpectedDatasetFormat;
import kernels.DummyKernel;
import kernels.EmpiricalKernelMap;
import kernels.SimilarityFunction;
import representation.Candidate;
import representation.Path;
import tools.ProgressBar;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializedObject;

public class WekaHelper {


	/**
	 * Build the weka feature vector object, the description/columns etc
	 * @param exp the experiment object that we're using
	 * @param kernel 
	 * @param num the number of features the vectors will have (class not included), that is to say the number of original training examples
	 * @return the feature vector object
	 */
	public static ArrayList<Attribute> wekaBuildFeatureVector(Experiment exp,SimilarityFunction kernel, Integer num, ArrayList<Candidate> candsWithLabels, Boolean relations_on_relations){
		ArrayList<Attribute> fvWekaAttributes = new ArrayList<Attribute>();
		ArrayList<String> fvClassVal = new ArrayList<String>(exp.schema.DefinedRelationTypes.size());
		for (RelationType r : exp.schema.DefinedRelationTypes) {
			if (relations_on_relations || !r.relation_on_relations) {
				fvClassVal.add(r.type);
			}
		}
		for (Candidate cand:candsWithLabels) {
			String type = cand.relation.type.type;
			if (!fvClassVal.contains(type)) {
				fvClassVal.add(type);
			}
		}
		if (!fvClassVal.contains("none")) {
			fvClassVal.add("none");
		}
		ArrayList<String> nullvector = null;
		Attribute idAttribute = new Attribute("id", nullvector);
		fvWekaAttributes.add(idAttribute);
		if (kernel instanceof DummyKernel) {

			/**
			 * for the dummy kernel, the following attribute is where we keep the class that was predicted (by co-occurrence)
			 */
			Attribute candRelTypeAttr = new Attribute("candRelType", fvClassVal);
			fvWekaAttributes.add(candRelTypeAttr);

		}
		else {
			for (int i = 0; i < num ; i++){
				Attribute attr = new Attribute("DistTo"+i);
				fvWekaAttributes.add(attr);
			}
		}
		if (exp.hasTriggerWords) {
			for (String relType: fvClassVal) {
				if (!relType.endsWith("none")) {
					Attribute attr = new Attribute(relType);
					fvWekaAttributes.add(attr);
				}
			}
		}
		Attribute ClassAttribute = new Attribute("EventType", fvClassVal);
		fvWekaAttributes.add(ClassAttribute);
		return fvWekaAttributes;
	}

	public static ArrayList<Attribute> wekaBuildFeatureVector(Experiment exp,SimilarityFunction kernel, Integer num, Boolean relations_on_relations){
		return wekaBuildFeatureVector(exp, kernel, num, new ArrayList<Candidate>(), relations_on_relations);

	}

	public static Instances wekaSet(Double verbose,Integer numOfTrainingInstances, ArrayList<Attribute> fvWekaAttributes, SimilarityFunction kernel, EmpiricalKernelMap EKM, ArrayList<Candidate> candidates, HashMap<String, String> classes, Boolean useTriggerWords) throws Exception{
		return wekaSet(verbose,numOfTrainingInstances, fvWekaAttributes, kernel, EKM, candidates, classes, new Integer(0), useTriggerWords);
	}
	/**
	 * Create a weka LABELED training dataset by using an Empirical Kernel Map object.
	 * @param numOfFeatures the number of EKM features, that is to say the number of the training candidates
	 * @param classCol the column number of the class column in the weka features vectors
	 * @param fvWekaAttributes the Weka feature vector object description initialized before
	 * @param EKM the EmpiricalKernelMap object that has already been initialized, which will give the distance vectors
	 * @param classes the list of class values that go with the candidates used to build the EKM
	 * @return the labeled training set with its distance vectors and their class in weka format
	 * @throws Exception 
	 */

	public static Instances wekaSet(Double verbose, Integer numOfTrainingInstances, ArrayList<Attribute> fvWekaAttributes, SimilarityFunction kernel, EmpiricalKernelMap EKM, ArrayList<Candidate> candidates, HashMap<String, String> classes, Integer noOfThreads, Boolean useTriggerWords) throws Exception{
		if (classes.size() == 0)  classes= initializeClasses(candidates);
		//		Integer classCol = fvWekaAttributes.size()-1;
		Instances tSet = new Instances("Rel", fvWekaAttributes, numOfTrainingInstances);
		tSet.setClassIndex(fvWekaAttributes.size()-1);  
		Integer length = candidates.size();
		int percent = 0;
		int size = candidates.size();
		int cores = Runtime.getRuntime().availableProcessors()-2;

		if (!noOfThreads.equals(new Integer(0))) {
			cores = noOfThreads;
		}
		if (verbose > 2) {
			System.out.println("Using threads: "+cores);
		}
		int range = size /cores ;
		if (range == 0) range =size;
		int start =0;
		int end = range;
		ArrayList<Thread> threads = new ArrayList<Thread>();
		ArrayList<WHThread> whs = new ArrayList<WHThread>();
		ProgressBar bar = new ProgressBar(length);
		if (verbose > 5) {
			System.out.println(new Date().toString()+" Starting transformation of "+size+ " candidates");

		}
		else {
			bar.update(percent, length);
		}
		while (end < size){
			Instances subSet = new Instances("Rel", fvWekaAttributes, range);
			WHThread wh = new WHThread(whs.size(),verbose,new ArrayList<Candidate>(candidates.subList(start, end)), kernel, EKM, fvWekaAttributes, numOfTrainingInstances, subSet, classes, bar, useTriggerWords) ;
			Thread th = new Thread(wh, end+"");
			th.start();
			threads.add(th);
			whs.add(wh);
			start = end;
			end += range;
		}
		Instances lastSet = new Instances("Rel", fvWekaAttributes, range);
		WHThread wh = new WHThread(whs.size(),verbose,new ArrayList<Candidate>(candidates.subList(start, size)), kernel, EKM, fvWekaAttributes, numOfTrainingInstances, lastSet, classes, bar, useTriggerWords);
		Thread th = new Thread(wh, end+"");
		th.start();
		threads.add(th);
		whs.add(wh);
		for (Thread thr : threads) {
			thr.join();
			if (verbose > 5) {
			}
			else {
				bar.updatePlusPlus();
			}
			tSet.addAll(whs.get(threads.indexOf(thr)).getValue());
		}
		return tSet;
	}
	/**
	 * Train a classifier given a training dataset
	 * @param cModel the (Classifier) model to train
	 * @param trainingSet the dataset to train on
	 */
	public static  void wekaBuildClassifier(Classifier cModel, Instances trainingSet) throws Exception{
		cModel.buildClassifier(trainingSet);
	}

	/**
	 * Perform the entire cross validation task at once with one single dataset. 
	 * @param cModel the (Classifier) model to train and evaluate
	 * @param crossSet the data to use for cross validation
	 * @return the Evaluation object (weka) that can then be used to print stuff like this
	 * <code><pre>
	 * String strSummary = eTest.toSummaryString()l
	 * System.out.println(strSummary);
	 * </pre></code>
	 */
	public static ArrayList<Evaluation> wekaCrossValidate(CrossValidation exp, SimilarityFunction kernel) throws Exception{
		ArrayList<Evaluation> evals = new ArrayList<Evaluation>();
		Classifier cModel = exp.wekaClassifier;
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Creating the Kernel");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Using Weka for CV: "+exp.runs+"x "+exp.folds+"-fold runs");
		for (int n = 0; n< exp.runs; n++) {
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Run no #"+(n+1));
			Evaluation eCross = new Evaluation(exp.set);
			if (exp.set.size() >= exp.folds) {
				eCross.crossValidateModel(cModel, exp.set, exp.folds, new Random(n));
				evals.add(eCross);
			}
		}
		//		weka.core.SerializationHelper.write("/tmp/model", cModel);
		// deserialize:  Classifier cls = (Classifier) weka.core.SerializationHelper.read("/some/where/j48.model");
		return evals;
	}

	/**
	 * Perform the entire cross validation task at once with one single dataset, PER CLASS (relation type)
	 * @param cModel the (Classifier) model to train and evaluate
	 * @param crossSet the data to use for cross validation
	 * @return the Evaluation object (weka) that can then be used to print stuff like this
	 * <code><pre>
	 * String strSummary = eTest.toSummaryString()l
	 * System.out.println(strSummary);
	 * </pre></code>
	 */
	public static HashMap<String,ArrayList<Evaluation>> wekaCrossValidatePerClass(CrossValidation exp, SimilarityFunction kernel) throws Exception{
		HashMap<String,ArrayList<Evaluation>> evals = new HashMap<String,ArrayList<Evaluation>>();
		Classifier cModel = exp.wekaClassifier;
		for (String type: exp.perClassCandidates.keySet()){
			if (exp.selectedRelationTypes.contains(type)) {
				ArrayList<Evaluation> typeEvals = new ArrayList<>();
				System.out.println("Cross-Validation for Class: "+type);
				if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Creating the Kernel for class "+type);
				if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Using Weka for CV: "+exp.runs+"x "+exp.folds+"-fold runs");
				Instances set = exp.perClassSet.get(type);
				for (int n = 0; n< exp.runs; n++) {
					if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Run no #"+(n+1));
					Evaluation eCross = new Evaluation(set);
					eCross.crossValidateModel(cModel, set, exp.folds, new Random(n));
					typeEvals.add(eCross);
				}
				evals.put(type, typeEvals);
			}
		}
		return evals;
	}

	/**
	 * Split Instances to train and test for folded CV
	 * @param data the Instances to split
	 * @param seed a seed (to increment for each run)
	 * @param folds number of folds
	 * @return a map of int (fold number) and (for each fold) a list of length two train & test Instances
	 * @throws Exception
	 */
	public static HashMap<Integer,ArrayList<Instances>> splitForCVRun(Instances data, int seed, int folds) throws Exception {
		HashMap<Integer,ArrayList<Instances>> foldData = new HashMap<Integer,ArrayList<Instances>>();
		Random rand = new Random(seed); 
		Instances randData = new Instances(data);
		randData.randomize(rand);
		randData.stratify(folds);
		for (int n = 0; n < folds; n++) {
			ArrayList<Instances> traintest = new ArrayList<Instances>();
			Instances train = randData.trainCV(folds, n);
			traintest.add(train);
			Instances test = randData.testCV(folds, n);
			traintest.add(test);
			foldData.put(n, traintest);
		}
		return foldData;
	}


	/**
	 * Takes a previously trained model and new data and gives back the data with prediction. 
	 * @param cModel the trained model
	 * @param unlabeled the unlabeled data in Instances format
	 * @return labeled data in Instances format
	 */
	public static Instances wekaPredict(Classifier cModel, Instances unlabeled) throws Exception{
		Instances labeled = new Instances(unlabeled);
		for (int i = 0; i < unlabeled.numInstances(); i++) {
			double clsLabel = cModel.classifyInstance(unlabeled.instance(i));
			//			   if (unlabeled.instance(i).classValue() == labeled.instance(i).classValue()){
			//				   System.out.println("it's the same");
			//			   }
			//			   else System.out.println("it is different!");
			//			if (clsLabel == 1.0) {
			//				System.out.println("caught one! :D");
			//			}
			labeled.instance(i).setClassValue(clsLabel);
		}
		return labeled;
	}


	/**
	 * This  method does the opposite of "wekaUnlabeledSet". It is given wekas predictions and it translates them into Candidates, so that we can later io.output them in the desired format (eg for a Challenge, for AlvisAE etc) 
	 * @return
	 */
	public static ArrayList<Candidate> wekaLabelSet( Schema schema, Instances predictedSet, Integer classCol,  ArrayList<Candidate> unlabeledCands){
		ArrayList<Candidate> predictedCandidates = new ArrayList<Candidate>();
		Integer eventid = 0;
		int i = 0;
		for (Candidate cand:unlabeledCands){
			Instance iPrediction = predictedSet.get(i);
			String iPredIndex =  iPrediction.attribute(0).value(0);
			if (cand != null) { 
				String candIndex = cand.index;
				//			iPrediction.attribute(0).value(0);
				if ((iPredIndex != null) && candIndex != null) {
					if (iPredIndex.equals(candIndex)){
						Path path;
						String rtype = iPrediction.stringValue(classCol);
						if (cand instanceof Path){
							path = new Path((Path) cand);
							if (cand.candidateRelationType.type.equals(rtype)){
								eventid++;
								path.relation = new Relation(cand.candidateRelationType,  path.arg1,  path.arg2	, new ID(eventid, ID.Type.relation));
							}

							else {
								path.relation = null; //it wasn't actually a possible relation that weka gave us, so we don't use it.
							}
						}
						else throw new UnexpectedDatasetFormat("We only know how to handle paths. Something went wrong");
						predictedCandidates.add(path);
					}
					else {
						for (Instance prediction: predictedSet){
							if (prediction != null) {
								String predIndex =  prediction.stringValue(0);
								if (predIndex != null) {
									if (predIndex.equals(candIndex)){
										Path path;
										String rtype = iPrediction.stringValue(classCol);
										if (cand instanceof Path){
											path = new Path((Path) cand);
											if (cand.candidateRelationType.type.equals(rtype)){
												eventid++;
												path.relation = new Relation(cand.candidateRelationType,  path.arg1, path.arg2	, new ID(eventid, ID.Type.relation));
											}

											else {
												path.relation = null; //it wasn't actually a possible relation that weka gave us, so we don't use it.
											}
											predictedCandidates.add(path);
											break;
										}
										else throw new UnexpectedDatasetFormat("We only know how to handle paths. Something went wrong");
									}
								}
								else {
									System.out.println("Attention, could not retrieve prediction index string. Moving on.");
								}

								//					 else {
								//						 System.out.println("well");
								//					 }
							}
							else {
								System.out.println("Attention, prediction was null. Moving on.");
							}
						}
					}
				}
				else {
					System.out.println("Attention, could not retrieve candidate index string. Moving on.");
				}
			}
			else {
				System.out.println("Attention, this candidate was null. Moving on.");
			}
			i++;

		}

		return predictedCandidates;
	}





	private static HashMap<String, String> initializeClasses(ArrayList<Candidate> cands){
		HashMap<String, String> classes = new HashMap<String, String>(); 
		for (int i = 0; i < cands.size(); i++){
			classes.put(cands.get(i).index, "none");
		}
		return classes;
	}

	/**
	 * Run the classification of the unlabeled data (use weka to predict labels and label the result), training data first
	 * @param exp
	 * @param kernel
	 * @return the model
	 * @throws Exception
	 */
	public static Classifier  wekaClassify(Predict exp, SimilarityFunction kernel) throws Exception{
		Classifier cModel = exp.wekaClassifier;
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Creating the Kernel");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Building the classifier");
		WekaHelper.wekaBuildClassifier(cModel, exp.labeledSet);
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" done!");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Using classifier to predict (Predict)");
		exp.predictedSet = WekaHelper.wekaPredict(cModel, exp.unlabeledSet);
		exp.predictedCandidates = WekaHelper.wekaLabelSet(exp.schema, exp.predictedSet,  exp.predictedSet.classIndex(), exp.unlabeledCandidates);
		//		for (Document doc: exp.unlabeledCorpus.documents){
		//			for (Sentence s: doc.sentences){
		//				s.updateRelations(exp.predictedCandidates);
		//			}
		//		}

		System.out.println("NOT UPDATING RELATIONS HERE!");
		return cModel;
	}
	public static void wekaClassifyPerClass(Predict exp, SimilarityFunction kernel) throws Exception{
//		HashMap<String, Double> cPerClass = new HashMap<String, Double>();
		FilteredClassifier cModel = exp.wekaClassifier;
		for (String type: exp.perClassUnlabeledCandidates.keySet()){
			System.out.println("Classification for Class: "+type);
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Creating the Kernel for class "+type);
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Building the classifier for class "+type);
			Instances labeled = exp.perClassLabeledSet.get(type);
			ArrayList<Candidate> unlabeledCandidates = exp.perClassUnlabeledCandidates.get(type);
			AbstractClassifier baseClassifier  = (AbstractClassifier) cModel.getClassifier();
			if (baseClassifier instanceof LibSVM){
				Double cost = exp.classCosts.get(type);
				if (cost == null)  cost = new Double(0.1); 
				((LibSVM) baseClassifier).setCost(cost);
				if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" changed c to "+cost);
			}
			WekaHelper.wekaBuildClassifier(cModel,labeled );
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" done for class "+type);
			if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Using classifier to predict for class "+type);
			Instances predicted = WekaHelper.wekaPredict(cModel, exp.perClassUnlabeledSet.get(type));
			exp.perClassPredictedSet.put(type,predicted);
			//			exp.predictedSet.addAll(predicted); //is this a good idea? i don't think so!!!
			ArrayList<Candidate> predictedCandidates = WekaHelper.wekaLabelSet(exp.schema, predicted,  predicted.classIndex(), unlabeledCandidates);
			exp.perClassPredictedCandidates.put(type, predictedCandidates);
			exp.predictedCandidates.addAll(predictedCandidates);
		}
		//		for (Document doc: exp.unlabeledCorpus.documents){
		//			for (Sentence s: doc.sentences){
		//				s.updateRelations(exp.predictedCandidates);
		//			}
		//		}
		System.out.println("NOT UPDATING RELATIONS HERE!");

	}

	/**
	 * Run the classification of the unlabeled data (use weka to predict labels and label the result), using pre-trained model
	 * @param cModel
	 * @param exp
	 * @param kernel
	 * @return the model 
	 * @throws Exception
	 */
	public static Classifier  wekaClassify(Classifier cModel,Predict exp, SimilarityFunction kernel) throws Exception{
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Creating the Kernel");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Using preloaded model");
		if (exp.verbose > 0) exp.getDumper().println(new Date().toString()+" Using classifier to predict (Predict with cModel)");
		try {
			exp.predictedSet = WekaHelper.wekaPredict(cModel, exp.unlabeledSet);
		}
		catch (Exception modelProblem) {
			System.out.println(modelProblem+ "\n There is probably a problem with the loaded model. Are you sure it was trained on the provided data?");
		}
		exp.predictedCandidates = WekaHelper.wekaLabelSet(exp.schema, exp.predictedSet,  exp.predictedSet.classIndex(), exp.unlabeledCandidates);
		//		for (Document doc: exp.unlabeledCorpus.documents){
		//			for (Sentence s: doc.sentences){
		//				s.updateRelations(exp.predictedCandidates);
		//			}
		//		}
		System.out.println("NOT UPDATING RELATIONS HERE!");

		return cModel;
	}

	public static HashMap<Integer, HashMap<String, Instances>> splitDataIntoFolds(Classifier classifier,
			Instances data, int numFolds, Random random)
					throws Exception {

		HashMap<Integer, HashMap<String, Instances>> foldData = new HashMap<Integer, HashMap<String, Instances>>();
		// Make a copy of the data we can reorder
		data = new Instances(data);
		data.randomize(random);
		if (data.classAttribute().isNominal()) {
			data.stratify(numFolds);
		}



		// Do the folds
		for (int i = 0; i < numFolds; i++) {
			HashMap<String, Instances> thisFoldData = new HashMap<String, Instances>();
			Instances train = data.trainCV(numFolds, i, random);
			Classifier copiedClassifier = AbstractClassifier.makeCopy(classifier);
			copiedClassifier.buildClassifier(train);
			Instances test = data.testCV(numFolds, i);
			/**/
			thisFoldData.put("train", train);
			thisFoldData.put("test", test);
			foldData.put(i, thisFoldData);
		}

		return foldData;
	}

	public static HashMap<Candidate, ArrayList<Candidate>> transformToCandidates(
			HashMap<Instance, Instances> targetInstancesWithNeighbours, ArrayList<Candidate> targetCandidates, ArrayList<Candidate> allCandidates) {
		// TODO Auto-generated method stub
		HashMap<Candidate, ArrayList<Candidate>>  targetCandidatesWithNeighbours = new HashMap<Candidate, ArrayList<Candidate>>();
		HashMap<Instance, Candidate> instancesToCandidates = new HashMap<Instance, Candidate>();
		for (Instance targetInstance:targetInstancesWithNeighbours.keySet()){
			Candidate targetCandidate = null;
			if (instancesToCandidates.containsKey(targetInstance)) {
				targetCandidate = instancesToCandidates.get(targetInstance);
			}
			else { 
				String instanceIndex = targetInstance.stringValue(0);
				for (Candidate cand:targetCandidates) {
					String candIndex = cand.index; 
					if (instanceIndex.equalsIgnoreCase(candIndex)){
						instancesToCandidates.put(targetInstance, cand);
						targetCandidate = cand;
					}
				}
			}
			for (Instance neighbourInstance:targetInstancesWithNeighbours.get(targetInstance)){
				Candidate neighbourCandidate = null;
				if (instancesToCandidates.containsKey(neighbourInstance)) {
					neighbourCandidate = instancesToCandidates.get(neighbourInstance);
				}
				else {
					String instanceIndex = neighbourInstance.stringValue(0);
					for (Candidate cand:allCandidates){
						String candIndex = cand.index;
						if (instanceIndex.equalsIgnoreCase(candIndex)){
							instancesToCandidates.put(neighbourInstance, cand);
							neighbourCandidate = cand;
						}
					}
				}
				if (targetCandidatesWithNeighbours.containsKey(targetCandidate)) {
					targetCandidatesWithNeighbours.get(targetCandidate).add(neighbourCandidate);
				}
				else {
					ArrayList<Candidate> newList = new ArrayList<Candidate>();
					newList.add(neighbourCandidate);
					targetCandidatesWithNeighbours.put(targetCandidate, newList);
				}
			}
		}
		return targetCandidatesWithNeighbours;
	}

	/**
	 * 
//			String id = cand.sentence.id;
			Instance iPrediction = predictedSet.get(i);
			String iPredIndex =  iPrediction.stringValue(0);
			String candIndex = cand.index;
			if (iPredIndex.equals(candIndex)){
				Path path;
				String rtype = iPrediction.stringValue(classCol);
				if (cand instanceof Path){
					path = new Path((Path) cand);
					if (cand.candidateRelationType.type.equals(rtype)){
						eventid++;
						path.relation = new Relation(cand.candidateRelationType, cand.candidateRelationType.arg1role, path.arg1,cand.candidateRelationType.arg2role	,  path.arg2	, new ID(eventid, ID.Type.relation));
					}

					else {
						path.relation = null; //it wasn't actually a possible relation that weka gave us, so we don't use it.
					}
				}
				else throw new UnexpectedDatasetFormat("We only know how to handle paths. Something went wrong");
				predictedCandidates.add(path);
			}
			else {
				for (Instance prediction: predictedSet){
					String predIndex =  prediction.stringValue(0);
					if (predIndex.equals(candIndex)){
						Path path;
						String rtype = iPrediction.stringValue(classCol);
						if (cand instanceof Path){
							path = new Path((Path) cand);
							if (cand.candidateRelationType.type.equals(rtype)){
								eventid++;
								path.relation = new Relation(cand.candidateRelationType, cand.candidateRelationType.arg1role, path.arg1,cand.candidateRelationType.arg2role	,  path.arg2	, new ID(eventid, ID.Type.relation));
							}

							else {
								path.relation = null; //it wasn't actually a possible relation that weka gave us, so we don't use it.
							}
							predictedCandidates.add(path);
							break;
						}
						else throw new UnexpectedDatasetFormat("We only know how to handle paths. Something went wrong");
					}

					//					 else {
					//						 System.out.println("well");
					//					 }
				}
			}
			i++;

		}
	 */



}
