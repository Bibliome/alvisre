package kernels;

import representation.Candidate;
/*
 * Co-occurrence kernel 
 */
public class DummyKernel extends SimilarityFunction {

	public DummyKernel(SimilarityFunctionConfiguration conf) {
		this.configuration = conf;
	}

	@Override
	public Double calculateScore(Candidate a, Candidate b) {
		return new Double(1);
	}

	@Override
	public Double calculateScore(Candidate a, Candidate b, boolean c) {
		return new Double(1);
	}

}
