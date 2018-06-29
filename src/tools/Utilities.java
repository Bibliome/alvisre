package tools;

import java.util.ArrayList;


public class Utilities {

	public Utilities() {
	}

	public static  Double dotProduct(ArrayList<Double> v1, ArrayList<Double> v2) throws Exception {
		Double score = 0.0;
		if (v1 == null || v2 == null) {
			throw (new Exception("null vectors!"));
		}
		if (v1.size()!= v2.size())
			try {
				throw new Exception("Vectors have different sizes!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		for (int i = 0; i< v1.size(); i++){
			score += v1.get(i) * v2.get(i);
		}
		return score;
	}
	
	
	public static Double euclideanDistance(ArrayList<Double> v1, ArrayList<Double> v2) throws Exception {
		if (v1 == null || v2 == null) {
			throw (new Exception("null vectors!"));
		}
		if (v1.size() != v2.size()) {
			throw (new Exception("Vector sizes do not much!"));
		}
		double score = 0.0;
		for (int i = 0, n = v1.size(); i < n; i++) {
			score += Math.pow(v1.get(i) - v2.get(i), 2);
		}
		return Math.sqrt(score);
	}

}
