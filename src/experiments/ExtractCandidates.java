package experiments;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.configuration.ConfigurationException;

import corpus.Corpus;
import io.input.InputModule;
import representation.Candidate;
import weka.core.Instances;

public class ExtractCandidates extends Experiment {
	Corpus corpus = new Corpus();
	ArrayList<Candidate> candidates = new ArrayList<>();
	String path = "";
	Instances set = null;
	InputModule input = null;
	Boolean labeled = true;
	Boolean externalCorpus = true;
	
	
	public ExtractCandidates(File xmlfile) throws ConfigurationException {
		super(xmlfile);
	}

	public ExtractCandidates(Experiment exp) {
		super(exp);
	}
	
	public ExtractCandidates(ExtractCandidates exp){
		super(exp);
		corpus=exp.corpus;
		candidates = exp.candidates;
		path = exp.path;
		set = exp.set;
		labeled = exp.labeled;
		externalCorpus = exp.externalCorpus;
	}
	
	public Boolean precalcPresent(){
		if (candidates.size() == 0)  return false;
		if (set == null) return false;
		return true;
	}
	

}
