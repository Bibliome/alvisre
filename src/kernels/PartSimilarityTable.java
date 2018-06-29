package kernels;

import java.util.ArrayList;
import java.util.List;

public class PartSimilarityTable {
	/** 
	 * This is the class tha handles the io.input tables of partSimilarities between the things we need to compare. 
	 * Think of semantic classes, ontology-based distances or whatever. 
	 */
	List<PartSimilarity> partSimilarities = new ArrayList<PartSimilarity>();
	String layer;
	public PartSimilarityTable(String l) {
		layer = l;
	}
	
	public void addEntry(PartSimilarity sim) {
		partSimilarities.add(sim);
	}
	
	public Double getSimilarity(String a1, String a2){
		for(PartSimilarity s:partSimilarities) {
			if (s.arg1.equals(a1) && s.arg2.equals(a2)) return s.sim;
			if (s.arg1.equals(a2) && s.arg2.equals(a1)) return s.sim;
		}
		return null;
	}
	
	
}
