package experiments;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import weka.core.Instance;
import weka.core.InstanceComparator;
import weka.core.Instances;

public class ExperimenterPrecalcTest {
	
	@Test
	public void testItMain() throws ConfigurationException, IOException, ParseException, Exception {
		File xmlfile1 = new File("data/arabido.xml");
		Experiment freshexp = Preparation.parseConfig(xmlfile1);
		Predict fresh = (Predict) freshexp;
		Process.runPrediction(fresh);
		assert(!fresh.precalcPresent());
//		assertTrue(compareInstanceSets(canned.labeledSet, fresh.labeledSet));
		//assertTrue(compareInstanceSets(canned.unlabeledSet, fresh.unlabeledSet));

	}

	@Test
	public void testMain() throws ConfigurationException, IOException, ParseException, Exception {
		File xmlfile1 = new File("data/miniLLL.xml");
		File xmlfile2 = new File("conf_compare.xml");
		Experiment freshexp = Preparation.parseConfig(xmlfile1);
		Predict fresh = (Predict) freshexp;
		Process.runPrediction(fresh);
		Experiment cannedexp = Preparation.parseConfig(xmlfile2);
		Predict canned = (Predict) cannedexp;
		assert(!fresh.precalcPresent());
		assert(canned.precalcPresent());
		Process.runPrediction(canned);
//		assertTrue(compareInstanceSets(canned.labeledSet, fresh.labeledSet));
		assertTrue(compareInstanceSets(canned.unlabeledSet, fresh.unlabeledSet));

	}

	
	@SuppressWarnings("unused")
	private Boolean compareInstanceSets(Instances one, Instances two){
		InstanceComparator comp = new InstanceComparator(true);
		for (Instance inst1:one) {
			Boolean foundMatch = false;
			for (Instance inst2:two){
				int kaka = comp.compare(inst1, inst2);
				if (comp.compare(inst1, inst2) == 0) {
					foundMatch =  true;
				}
				else {
					foundMatch = false;
				}
			}
			if (!foundMatch) {
				return false;
			}
		}
		return true;
	}
}
