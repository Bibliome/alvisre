package kernels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import representation.Candidate;

public abstract class SimilarityFunction {
	/**
	 * A SimilarityFunction object is one that defines a function to calculate a similarity
	 * score
	 * 
	 * 
	 * * SVM interface types: - write a matrix to be used later on - collaborate
	 * with weka - collaborate with libsvm
	 */

	public SimilarityFunctionConfiguration configuration = new SimilarityFunctionConfiguration();
	public List<PartSimilarityTable> partSimilarityTables = new ArrayList<PartSimilarityTable>();


	public SimilarityFunction() {

	}


	public SimilarityFunction( SimilarityFunctionConfiguration conf, List<PartSimilarityTable> tables ) throws IOException {
		this.partSimilarityTables = tables;
		this.configuration = conf;

	}


	public abstract Double calculateScore(Candidate a, Candidate b);

	/**
	 * calculate the score between two candidates, with the possibility of a boolean parameter, used for threads (or other)
	 * @param a Candidate a
	 * @param b Candidate b
	 * @param c Boolean c, threaded or not in most cases
	 * @return
	 */
	public abstract Double calculateScore(Candidate a, Candidate b, boolean c);


}
