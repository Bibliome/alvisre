package kernels;

import exceptions.UnexpectedDatasetFormat;

public class PartSimilarity {
	/**
	 * This is the object that handles one single instance of the PartSimilarityTable. It's the similarity (io.input) of one element
	 * to another. It is not what we calculate, that is to say the similarity between different sentences, but the similarity
	 * of word_A and word_B on the level "semantics", for example (semantics would be a layer and we'd search for their tags
	 * in the tables, and the similarity of the tags would be a Simiarity).
	 */
	public String arg1;
	public String arg2;
	public Double sim;
	
	public PartSimilarity(String a1, String a2, Double s){
		if (a1 == null || a2 == null || s == null) throw new UnexpectedDatasetFormat("Error S11: This similarity is invalid!");
		arg1 = a1;
		arg2 = a2;
		sim = s;
	}

}
