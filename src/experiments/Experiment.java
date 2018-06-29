package experiments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import corpus.Schema;
import io.output.OutputModule;
import kernels.PartSimilarityTable;
import kernels.SimilarityFunctionConfiguration;
import kernels.TriggerWordTable;
import representation.Candidate;
import semantictools.WordToSemClassTable;
import tools.Dumper;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;

public  class Experiment {

	public XMLConfiguration config = new XMLConfiguration();
	protected Schema schema = new Schema();
	public SimilarityFunctionConfiguration fauxkernelconf = new SimilarityFunctionConfiguration();
	public Double verbose;
	protected String name;
	public List<PartSimilarityTable> simTables = new ArrayList<PartSimilarityTable>();
	protected WordToSemClassTable wordToSemClassTable = new WordToSemClassTable();
	protected TriggerWordTable triggerWordTable = new TriggerWordTable();
	public String outputPath;
	String logPath;
	String inputType;
	FilteredClassifier wekaClassifier;
	OutputModule output;
	File xmlfile;
	public Boolean writeObjects;
	Integer threads = 0;
	private  Dumper dumper;
	public Statistics statistics = new Statistics();
	public String representation ;
	public boolean writeEntities;
	public int bionlpTask ;
	Classifier trainedModel = null; 
	public Boolean hasTriggerWords = false;
	public ArrayList<Candidate> referenceCandidates = new ArrayList<Candidate>();
	public Boolean relations_on_relations = false;
	public Boolean perClass = false;
	ArrayList<Double> cValues = new ArrayList<>();
	String [] wordTags = null;
	public ArrayList<String> selectedRelationTypes = new ArrayList<String>();
	public HashMap<String, Double> classCosts = new HashMap<>();
	/**
	 * An experiment run: it takes an configuration xml file as an io.input and performs the experiment described in that xml
	 * @param args one single parameter: the xml file name
	 * @throws ConfigurationException 
	 * @throws IOException 
	 * @throws ParseException during parsing of a command-line
	 * @throws Exception 
	 */
	public Experiment(File xmlfile) throws ConfigurationException  {
		config = new XMLConfiguration(xmlfile);
		config.load();
		this.xmlfile = xmlfile;
	}
	
	public Experiment(Experiment exp){
		clone(exp);
	}
	
	public Schema getSchema(){
		return schema;
	}
	
	public void setOutputPath (String path){
		outputPath = path;
	}
	
	public String getOutputPath (){
		return outputPath;
	}
	public void clone(Experiment exp){
		config = exp.config ;
		schema = exp.schema;
		fauxkernelconf = exp.fauxkernelconf;
		verbose = exp.verbose;
		name = exp.name;
		simTables  = exp.simTables;
		setOutputPath(exp.getOutputPath());
		logPath = exp.logPath;
		inputType = exp.inputType;
		wekaClassifier = exp.wekaClassifier;
		output = exp.output;
		xmlfile = exp.xmlfile;
		dumper = exp.dumper;
		writeObjects = exp.writeObjects;
		writeEntities = exp.writeEntities;
		threads = exp.threads;
		representation = exp.representation;
		bionlpTask = exp.bionlpTask;
		trainedModel = exp.trainedModel;
		hasTriggerWords = exp.hasTriggerWords;
		referenceCandidates = exp.referenceCandidates;
		relations_on_relations = exp.relations_on_relations;
		perClass = exp.perClass;
		cValues = exp.cValues;
		wordTags = exp.wordTags;
		selectedRelationTypes = exp.selectedRelationTypes;
		classCosts = exp.classCosts;
	}

	public  Dumper getDumper() {
		return dumper;
	}

	public  void setDumper(Dumper dumper) {
		this.dumper = dumper;
	}
	
	public SimilarityFunctionConfiguration getConf(){
		return fauxkernelconf;
	}

	public boolean hasReference() {
		if (referenceCandidates.size() == 0) return false;
		return true;
	}

}