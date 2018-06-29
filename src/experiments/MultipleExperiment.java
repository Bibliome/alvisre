package experiments;


public class MultipleExperiment extends Experiment {

	Integer runs = 1;
	Double percent = 1.0;
	Integer folds = 1;
	Experiment experiment;
	String type = "none";

	public MultipleExperiment(Experiment exp) {
		super(exp);
		experiment = exp;
	}

	/*
	 * used to create a percentage "Predict" for the experiments PZ had recommended for broken paths
	 */
	public MultipleExperiment(Experiment exp, Integer runs, Double percent) {
		super(exp);
		experiment = exp;
		this.runs = runs;
		this.percent = percent;
		this.type = "Predict";
	}
	
	/*
	 * used to create a multi folded cv
	 */
	public MultipleExperiment(Experiment exp, Integer runs, Integer folds){
		super(exp);
		experiment = exp;
		this.runs = runs;
		this.folds = folds;
		this.type = "CrossValidation";
	}

}