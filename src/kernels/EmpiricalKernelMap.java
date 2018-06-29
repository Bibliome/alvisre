package kernels;

import java.util.ArrayList;

import representation.Candidate;


public class EmpiricalKernelMap {
	/** Because we cannot use the GlobalAlignment directly, as it is not a PSD kernel, we use this Empirical Kernel Map
	 * ref. "A kernel approach for learning from almost orthogonal patterns" by B. Scholkopf et al 2002
	 * The way it works is instead of using the similarity function, we express its data point as a vector of its 
	 * distances (using the similarity function) to each of the rest of the points. This new matrix is PSD and is 
	 * a kernel. Since it's an integer vector we can then apply simple linear SVN algorithms on it. Isn't this swell?
	 * To create it you io.input the Candidates and the similarity function object, it calculates the new matrix, that it can return as a whole, part or even 
	 * a single line
	 */


	public ArrayList<Candidate> candidates = new ArrayList<Candidate>();
	public SimilarityFunction fauxkernel;

	public EmpiricalKernelMap(ArrayList<Candidate> candidates, SimilarityFunction fauxkernel){
		this.candidates = candidates;
		this.fauxkernel = fauxkernel;

	}

	public ArrayList<Double> getVector(Candidate a){
		ArrayList<Double> map  = new ArrayList<Double>();
		for (Candidate b: candidates){
			map.add(fauxkernel.calculateScore(a, b, true));
		}
		return map;
	}
}
