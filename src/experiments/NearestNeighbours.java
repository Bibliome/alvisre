package experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;

import corpus.Corpus;
import io.input.InputModule;
import representation.Candidate;
import weka.core.Instances;

public class NearestNeighbours extends Experiment {
	int numOfNNs = 1;
	ArrayList<Candidate> targetCandidates = new ArrayList<>();
	Corpus targetCorpus = new Corpus();
	Corpus allCorpus = new Corpus();
	ArrayList<Candidate> allCandidates = new ArrayList<>();
	HashMap<Candidate, ArrayList<Candidate>> targetCandidatesWithNeighbours = new HashMap<Candidate, ArrayList<Candidate>>();
	String allPath = "";
	String targetPath = "";
	Instances allSet = null;
	Instances targetSet = null;
	InputModule allInput = null;
	InputModule targetInput = null;
	int k = 10;
	Boolean allLabeled = true;
	Boolean targetLabeled = true;
			
	
	
	public NearestNeighbours(File xmlfile) throws ConfigurationException {
		super(xmlfile);
	}

	public NearestNeighbours(Experiment exp) {
		super(exp);
	}
	
	public NearestNeighbours(NearestNeighbours exp){
		super(exp);
		targetCandidates = exp.targetCandidates;
		targetCorpus = exp.targetCorpus;
		allCorpus=exp.allCorpus;
		allCandidates = exp.allCandidates;
		targetCandidatesWithNeighbours = exp.targetCandidatesWithNeighbours;
		allPath = exp.allPath;
		targetPath = exp.targetPath;
		allSet = exp.allSet;
		targetSet = exp.targetSet;
		k = exp.k;
		allLabeled = exp.allLabeled;
		targetLabeled = exp.targetLabeled;
	}
	
	public Boolean precalcPresent(){
		if (allCandidates.size() == 0)  return false; 
		if (targetCandidates.size() == 0) return false;
		if (allSet == null) return false;
		if (targetSet == null) return false;
		return true;
	}
	public Boolean precalcAllPresent(){
		if (allCandidates.size() == 0)  return false; 
		if (allSet == null) return false;
		return true;
	}
	
	public Boolean precalcTargetPresent(){
		if (targetCandidates.size() == 0) return false;
		if (targetSet == null) return false;
		return true;
	}

}
