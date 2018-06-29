package kernels;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.junit.Test;

import experiments.Experiment;
import experiments.Predict;
import experiments.Preparation;
import experiments.WekaHelper;
import tools.Dumper;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.InstanceComparator;
import weka.core.Instances;

public class EKMTest {

	@Test
	public void EKMshitTester() throws Exception {
		String xmlfilename = "miniBB.xml";
		File xmlfile = new File(xmlfilename);
		Experiment experm = Preparation.parseConfig(xmlfile);
		Predict exp = (Predict) experm;
		GlobalAlignment kernel = new GlobalAlignment(exp.fauxkernelconf, exp.simTables);
		if (exp.getDumper() == null)  exp.setDumper(new Dumper(exp.outputPath+"/"+"kaka"+".log", exp.config.getFile()));

		System.out.println(new Date().toString()+" Calculating representation as Shortest Paths on the Dependency Graph [TRAINING]");
		exp.labeledCandidates = Preparation.prepareRepresentation( xmlfilename, exp.labeledCorpus, exp, true, false);
		System.out.println(new Date().toString()+" Calculating representation as Shortest Paths on the Dependency Graph [TEST]");
		exp.unlabeledCandidates = Preparation.prepareRepresentation( xmlfilename, exp.unlabeledCorpus, exp, false, false);
		System.out.println(new Date().toString()+"  Preparing transformation in weka instances");
		EmpiricalKernelMap ekm = new EmpiricalKernelMap(exp.labeledCandidates, kernel);
		ArrayList<Attribute> wekavector = WekaHelper.wekaBuildFeatureVector(exp, kernel, exp.labeledCandidates.size(), false);
		Instances set1  = WekaHelper.wekaSet(new Double(15), exp.labeledCandidates.size(), wekavector, kernel, ekm, exp.labeledCandidates,Preparation.getCandidatesClasses(exp.labeledCandidates),false);
		Instances set2 = WekaHelper.wekaSet(new Double(15),exp.labeledCandidates.size(),  wekavector, kernel, ekm, exp.labeledCandidates, new HashMap<String, String>(), false);
		assertTrue(compareInstanceSets(set1, set2));
		System.out.println("kaka");
	}


	private Boolean compareInstanceSets(Instances one, Instances two){
		InstanceComparator comp = new InstanceComparator(false);
		if (one.numInstances() != two.numInstances()) return false;
		for (int i =0; i < one.numInstances(); i++) {
			Instance inst1 = one.get(i);
			Instance inst2 = two.get(i);
			Boolean foundMatch = false;
			if (comp.compare(inst1, inst2) == 0) {
				foundMatch =  true;
			}
			else {
				foundMatch = false;
			}
			if (!foundMatch) {
				return false;
			}
		}
		return true;
	}
}
