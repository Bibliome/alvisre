package experiments;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;

import corpus.Corpus;
import io.input.InputModule;
import representation.Candidate;
import weka.core.Instances;


/**
 * 
 * @author mba
 *
 */
public class Predict extends Experiment {
	String labeledPath;
	String unlabeledPath;
	String outputFormat;
	String outputFormatChallenge;
	InputModule labeledInput;
	InputModule unlabeledInput;
	public Corpus labeledCorpus = new Corpus();
	public Corpus unlabeledCorpus = new Corpus();
	String LLLkey;
	String LLLdictionary;
	String LLLtool;
	String LLLtest;
	String LLLout;
	public ArrayList<Candidate> labeledCandidates = new ArrayList<Candidate>();
	public ArrayList<Candidate> unlabeledCandidates = new ArrayList<Candidate>();
	public ArrayList<Candidate> predictedCandidates = new ArrayList<Candidate>();
	Instances labeledSet = null;
	Instances unlabeledSet = null;
	Instances predictedSet = null;
	String evaluationScript; 
	String referencePath;
	String txtPath;
	Boolean isTrainAndTest = false;
	public HashMap<String, ArrayList<Candidate>> perClassLabeledCandidates = new HashMap<String, ArrayList<Candidate>>();
	public HashMap<String, ArrayList<Candidate>> perClassUnlabeledCandidates = new HashMap<String, ArrayList<Candidate>>();
	public HashMap<String, ArrayList<Candidate>> perClassPredictedCandidates = new HashMap<String, ArrayList<Candidate>>();
	public HashMap<String, Instances> perClassLabeledSet = new HashMap<String, Instances>();
	public HashMap<String, Instances> perClassUnlabeledSet = new HashMap<String, Instances>();
	public HashMap<String, Instances> perClassPredictedSet = new HashMap<String, Instances>();
	

	public Predict(Experiment exp) throws ConfigurationException{
		super(exp);
	}
	
	public Predict(Predict exp)  throws ConfigurationException{
		super(exp);
		labeledPath = exp.labeledPath;
		unlabeledPath = exp.unlabeledPath;
		outputFormat = exp.outputFormat;
		outputFormatChallenge = exp.outputFormatChallenge;
		labeledInput = exp.labeledInput;
		unlabeledInput = exp.unlabeledInput;
		labeledCorpus = exp.labeledCorpus;
		unlabeledCorpus = exp.unlabeledCorpus;
//		labeledCandidates = (ArrayList<Candidate>) exp.labeledCandidates.clone();
		labeledCandidates = new ArrayList<Candidate>(exp.labeledCandidates);
		unlabeledCandidates = new ArrayList<Candidate>(exp.unlabeledCandidates);
		labeledSet = new Instances(exp.labeledSet);
		unlabeledSet = new Instances(exp.unlabeledSet);
		if (exp.predictedSet != null) predictedSet = new Instances(exp.predictedSet);
//		Collections.copy(labeledSet, exp.labeledSet);
//		Collections.copy(unlabeledSet, exp.unlabeledSet);
//		Collections.copy(predictedSet, exp.predictedSet);
		LLLkey = exp.LLLkey;
		LLLdictionary= exp.LLLdictionary;
		LLLtool = exp.LLLtool;
		LLLtest = exp.LLLtest;
		LLLout = exp.LLLout;
		 evaluationScript = exp.evaluationScript; 
		 referencePath = exp.referencePath;
		 txtPath = exp.txtPath;
//		 writeObjects = exp.writeObjects;
		 isTrainAndTest  = exp.isTrainAndTest;
		 hasTriggerWords = exp.hasTriggerWords;
		
	}
	
	
	public Predict(MultipleExperiment multi)  throws ConfigurationException{
		super(multi.experiment);
		Predict exp = (Predict) multi.experiment;
		labeledPath = exp.labeledPath;
		unlabeledPath = exp.unlabeledPath;
		outputFormat = exp.outputFormat;
		outputFormatChallenge = exp.outputFormatChallenge;
		labeledInput = exp.labeledInput;
		unlabeledInput = exp.unlabeledInput;
		labeledCorpus = exp.labeledCorpus;
		unlabeledCorpus = exp.unlabeledCorpus;
		LLLkey = exp.LLLkey;
		LLLdictionary= exp.LLLdictionary;
		LLLtool = exp.LLLtool;
		LLLtest = exp.LLLtest;
		LLLout = exp.LLLout;
		 evaluationScript = exp.evaluationScript; 
		 referencePath = exp.referencePath;
		 txtPath = exp.txtPath;
		 isTrainAndTest  = exp.isTrainAndTest;
		 hasTriggerWords = exp.hasTriggerWords;
		
	}

	public Boolean precalcPresent(){
		if (labeledCandidates.size() == 0)  return false; 
		if (unlabeledCandidates.size() == 0) return false;
		if (labeledSet == null) return false;
		if (unlabeledSet == null) return false;
		return true;
	}
	
	
	public Boolean modelPresent(){
		if (trainedModel == null) return false;
		return true;
	}






}
