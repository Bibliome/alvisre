package experiments;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;

import exceptions.UnexpectedDatasetFormat;

public class SpecialExperiment {

	public SpecialExperiment() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) 	throws ConfigurationException, IOException, ParseException, Exception {
			String xmlfilename = args[0];
			File xmlfile = new File(xmlfilename);
			Experiment exp = Preparation.parseConfig(xmlfile);
			if (exp instanceof Predict){
				Predict pr = (Predict) exp;
				Process.runSpecialPrediction(pr);
			}
			else {
				throw (new UnexpectedDatasetFormat("Please use this with Predict scenarios for now"));
			}
	}

}
