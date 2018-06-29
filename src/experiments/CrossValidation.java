package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;

import corpus.Corpus;
import io.input.InputModule;
import representation.Candidate;
import weka.core.Instances;

/**
 * @author Dialekti Valsamou
 * @version
 * This is the experiment, the main thing. It should be the only thing that can be called externally. 
 *
 */
public class CrossValidation extends Experiment {
	String inputPath;
	InputModule input;
	Corpus corpus;
	ArrayList<Candidate> candidates = new ArrayList<Candidate> ();
	String outputFormat;
	String outputFormatChallenge;
	String LLLkey;
	String LLLdictionary;
	String LLLtool;
	String LLLtest;
	Integer runs;
	Instances set = null;
	Integer folds = 10;
	public HashMap<String, ArrayList<Candidate>> perClassCandidates = new HashMap<String, ArrayList<Candidate>>();
	public HashMap<String, Instances> perClassSet = new HashMap<String, Instances>();

	public CrossValidation(File xmlfile) throws ConfigurationException{
		super(xmlfile);
	}
	
	public CrossValidation(Experiment exp) throws ConfigurationException{
		super(exp);
		if (exp instanceof CrossValidation) {
			this.inputPath = ((CrossValidation) exp).inputPath;
			this.input = ((CrossValidation) exp).input;
			this.corpus = ((CrossValidation) exp).corpus;
			this.candidates = ((CrossValidation) exp).candidates;
			this.outputFormat = ((CrossValidation) exp).outputFormat;
			this.outputFormatChallenge = ((CrossValidation) exp).outputFormatChallenge;
			this.LLLkey = ((CrossValidation) exp).LLLkey;
			this.LLLdictionary = ((CrossValidation) exp).LLLdictionary;
			this.LLLtool = ((CrossValidation) exp).LLLtool;
			this.LLLtest = ((CrossValidation) exp).LLLtest;
			this.runs = ((CrossValidation) exp).runs;
			this.set = ((CrossValidation) exp).set;
			this.folds = ((CrossValidation) exp).folds;
			this.perClassCandidates = ((CrossValidation) exp).perClassCandidates;
			this.perClassSet = ((CrossValidation) exp).perClassSet;
		}
		
	}
	


}

