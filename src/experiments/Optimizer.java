package experiments;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;

import corpus.RelationType;
import exceptions.UnexpectedDatasetFormat;
import tools.Dumper;
import tools.PrintStuff;

public class Optimizer {
	public static void main(String[] args) throws ConfigurationException, IOException, ParseException, Exception {
		String xmlfilename = args[0];
		File xmlfile = new File(xmlfilename);
		Experiment exp = Preparation.parseConfig(xmlfile);
		if (exp.perClass) {
			System.out.println("================\nProcessing each relation type seperately\n===============");
		} else {
			System.out.println("================\nProcessing all relation types together\n===============");
		}
		TreeMap<String, Double[]> finalPrintMap = new TreeMap<String, Double[]>();

		if (exp instanceof CrossValidation) {
			CrossValidation cv = (CrossValidation) exp;
			if (cv.perClass) {
				String runID = new SimpleDateFormat("'run-'yyyy'.'MM'.'dd'.'HH':'mm':'ss").format(new Date());
				if (cv.getDumper() == null)
					cv.setDumper(new Dumper(exp.outputPath + "/" + runID + ".log", cv.config.getFile()));
				ArrayList<Thread> threads = new ArrayList<Thread>();
				ArrayList<CVThread> cvthreads = new ArrayList<CVThread>();

				for (RelationType type : cv.schema.DefinedRelationTypes) {
					if (!type.type.equals("none")) {
						CVThread cvthread = new CVThread(type, cv, exp);
						Thread thread = new Thread(cvthread, type.type);
						thread.start();
						threads.add(thread);
						cvthreads.add(cvthread);
					
					}
					
				}
				for (Thread thr : threads) {
					thr.join();
					TreeMap<String, Double[]> printMap = cvthreads.get(threads.indexOf(thr)).getValue();
					PrintStuff.printRecap(printMap, "for class", cv.getDumper());
					finalPrintMap.putAll(printMap);
				}
			} else {
				throw (new UnexpectedDatasetFormat("Only Cross-Validation PER CLASS works with Optimizer currently."));

			}
			PrintStuff.printRecap(finalPrintMap, "for all classes", cv.getDumper());

		} else {
			throw (new UnexpectedDatasetFormat("Only Cross-Validation works with Optimizer currently."));
		}

	}
}
