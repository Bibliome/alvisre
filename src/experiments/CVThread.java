package experiments;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.commons.configuration.ConfigurationException;

import corpus.RelationType;
import corpus.Schema;
import representation.Candidate;
import tools.Dumper;
import tools.PrintStuff;
import weka.core.Instances;

public class CVThread implements Runnable {
	TreeMap<String, Double[]> printMap = new TreeMap<String, Double[]>();
	RelationType type;
	CrossValidation cv;
	Experiment exp;

	public CVThread(RelationType t, CrossValidation c, Experiment e) {
		type = t;
		cv = c;
		exp = e;
	}

	@Override
	public void run() {
		ArrayList<Candidate> cacheCandidates = new ArrayList<>();
		Instances cacheInstances = null;
		HashMap<Double, Double[]> results = new HashMap<>();
		for (Double d : exp.cValues) {
			System.out.println("Optimizing for relation type " + type.type);
			CrossValidation newcv;
			try {
				String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
				newcv = new CrossValidation(cv);
				ArrayList<RelationType> newTypes = new ArrayList<>();
				newTypes.add(type);
				newcv.schema = new Schema(cv.schema.DefinedTypes, newTypes, cv.schema.DefinedGroups);
				newcv.outputPath = cv.outputPath + "/" + type.type + "/" + d;
				newcv.setDumper(new Dumper(newcv.outputPath + "/" + runID + ".log", cv.config.getFile()));
//			    System.err.println("\nEXP Classifier options :"+Arrays.toString(exp.wekaClassifier.getOptions()));
//                System.err.println("\nCV Classifier options :"+Arrays.toString(cv.wekaClassifier.getOptions()));
//                System.err.println("\nNEWCV Classifier options :"+Arrays.toString(newcv.wekaClassifier.getOptions()));

				String[] options = newcv.wekaClassifier.getOptions();
				for (int i = 0; i < options.length; i++) {
					if (options[i].equals("-C")) {
						options[i + 1] = d.toString();
						break;
					}
				}
				newcv.wekaClassifier.setOptions(options);
//                System.err.println("\nNEWCV Classifier NEW options :"+Arrays.toString(newcv.wekaClassifier.getOptions()));

				if (!cacheCandidates.isEmpty()) {
					newcv.candidates = cacheCandidates;
				}
				if (cacheInstances != null) {
					newcv.set = cacheInstances;
				}
				newcv.perClass = false;
				TreeMap<String, Double[]> result  = Process.runCrossValidation(newcv);

				if (cacheCandidates.isEmpty()) {
					cacheCandidates = newcv.candidates;
				}
				if (cacheInstances == null) {
					cacheInstances = newcv.set;
				}
				results.put(d, result.get("Weighted Average"));
				HashMap<String, Double[]> cPrintMap = new HashMap<String, Double[]>();
				cPrintMap.put(type.type + " - C Value " + d, result.get("Weighted Average"));
				PrintStuff.printRecap(cPrintMap, "for this c value", newcv.getDumper());
			} catch (ConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * recap for this class
		 */
		printMap = new TreeMap<String, Double[]>();

		for (Double d : results.keySet()) {
			Double[] result = results.get(d);
			printMap.put(type.type + " - C Value " + d, result);
		}
	}

	public TreeMap<String, Double[]> getValue() {

		return printMap;
	}

}
