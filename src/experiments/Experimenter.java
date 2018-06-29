package experiments;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;

import exceptions.UnexpectedDatasetFormat;

public class Experimenter {
	public static void main(String[] args) throws ConfigurationException, IOException, ParseException, Exception {
		String xmlfilename = args[0];
		File xmlfile = new File(xmlfilename);
		Experiment exp = Preparation.parseConfig(xmlfile);
		if (exp.perClass) {
			System.out.println("================\nProcessing each relation type seperately\n===============");
		}
		else {
			System.out.println("================\nProcessing all relation types together\n===============");
		}
		if (exp instanceof CrossValidation){
			CrossValidation cv = (CrossValidation)exp;
//			if (cv.perClass) {
//				Process.runCrossValidationPerClass(cv);
//			}
//			else {
				Process.runCrossValidation(cv);
//			}

		}
		else if (exp instanceof Predict){
			Predict pr = (Predict) exp;
			if (pr.perClass) {
				Process.runPredictionPerClass(pr);
			}
			else {
				Process.runPrediction(pr);
			}
		}
		else if (exp instanceof MultipleExperiment){
			MultipleExperiment multi = (MultipleExperiment) exp;
			if ( 	multi.type.equalsIgnoreCase("predict")) {
				//				Process.runPrediction(multi);
				throw (new UnexpectedDatasetFormat("Multi-Predict is disabled"));
			}
			else if (multi.type.equalsIgnoreCase("crossvalidation")){
				Process.runCrossValidationByScript(multi);

			}
			else {
				throw (new UnexpectedDatasetFormat("This shouldn't have happened. I promise. Multi-experiment definition error"));
			}

		}
		else if (exp instanceof NearestNeighbours) {
			NearestNeighbours nn = (NearestNeighbours) exp;
			Process.runNeighbourSearchWithClassFilters(nn);
		}

		else if (exp instanceof ExtractCandidates) {
			ExtractCandidates ex = (ExtractCandidates) exp;
			Process.extractCandidates(ex);
		}
	}


}
