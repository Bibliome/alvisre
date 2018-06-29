package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import kernels.EmpiricalKernelMap;
import representation.Candidate;
import tools.ProgressBar;
import tools.Utilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.LinearNNSearch;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

public class NeighbourSearch {

//	private static final Instance filteredTarget = null;

	public NeighbourSearch() {
	}

	
//	public static Instances runNNsearchForInstance(Instances instances, Instance target, int k) throws Exception{
//		LinearNNSearch knn = new LinearNNSearch(instances);
//		knn.setSkipIdentical(true);
//		Instances nearestInstances= knn.kNearestNeighbours(target, k);
//		return nearestInstances;
//	}
	
	public static HashMap<Candidate, ArrayList<Candidate>>  runNNsearchForCandidatesUsingKernel(ArrayList<Candidate> allCandidates, ArrayList<Candidate> targetCandidates, int k, EmpiricalKernelMap ekm)  throws Exception{
		HashMap<Candidate, ArrayList<Candidate>> targetsToNeighbours = new HashMap<>();
		for (Candidate target:targetCandidates){
			ArrayList<Double> vector = ekm.getVector(target);
			ArrayList<NeighbourAndDistance> neighboursAndDistances = new ArrayList<NeighbourAndDistance>();
			for (int i = 0; i < vector.size(); i++) {
				NeighbourAndDistance nNd = new NeighbourAndDistance(allCandidates.get(i), vector.get(i));
				neighboursAndDistances.add(nNd);
			}
			Collections.sort(neighboursAndDistances, new NeighbourComparator());
			ArrayList<Candidate> theseNeighbours = new ArrayList<Candidate>();
			for (int i = 0; i<k; i++) {
				theseNeighbours.add(neighboursAndDistances.get(i).neighbour);
				if  ((neighboursAndDistances.get(i).distance.equals(neighboursAndDistances.get(i+1).distance))&& (i == k-1) ) {
					k++;
				}
			}
			targetsToNeighbours.put(target, theseNeighbours);
		}
		return targetsToNeighbours;
	}
	
	public static HashMap<Candidate, ArrayList<Candidate>>  runNNsearchForCandidatesUsingEuclideanDistance(ArrayList<Candidate> allCandidates, ArrayList<Candidate> targetCandidates, int k, EmpiricalKernelMap ekm)  throws Exception{
		HashMap<Candidate, ArrayList<Candidate>> targetsToNeighbours = new HashMap<>();
		HashMap<String, Double> idxidxDistance = new HashMap<String, Double>();
		System.out.println("Finding nearest neighbours");
		ProgressBar bar = new ProgressBar(targetCandidates.size());
		for (Candidate target:targetCandidates){
			bar.updatePlusPlus();
			ArrayList<Double> vector1 = ekm.getVector(target);
			ArrayList<NeighbourAndDistance> neighboursAndDistances = new ArrayList<NeighbourAndDistance>();
			for (Candidate cand:allCandidates){
				String idxid = "";
				Double distance = 0.0;
				if (cand.index.compareTo(target.index) < 0) {
					idxid = cand.index+target.index;
				}
				else idxid = target.index+cand.index;
				if (idxidxDistance.containsKey(idxid)) {
					distance = idxidxDistance.get(idxid);
				}
				else {
					ArrayList<Double> vector2 = ekm.getVector(cand);
					distance = Utilities.euclideanDistance(vector1, vector2);
					idxidxDistance.put(idxid, distance);
				}
				NeighbourAndDistance nNd = new NeighbourAndDistance(cand, distance);
				neighboursAndDistances.add(nNd);
			}
			
			Collections.sort(neighboursAndDistances, new NeighbourComparator());
			ArrayList<Candidate> theseNeighbours = new ArrayList<Candidate>();
			int k1 = k;
			for (int i = 0; i<k1; i++) {
				theseNeighbours.add(neighboursAndDistances.get(i).neighbour);
				if  ((neighboursAndDistances.get(i).distance.equals(neighboursAndDistances.get(i+1).distance))&& (i == k-1) ) {
					k1++;
				}
			}
			targetsToNeighbours.put(target, theseNeighbours);
		}
		return targetsToNeighbours;
	}
	
	
	
	
	


	public static HashMap<Instance,Instances> runNNsearchForInstances(Instances allInstances, Instances targets, int k) throws Exception{
		HashMap<Instance,Instances> targetsToNeighbours  = new HashMap<Instance, Instances>();
		Remove rm = new Remove();
		rm.setAttributeIndices("1");
		rm.setInputFormat(allInstances);
		Instances tmp = Filter.useFilter(allInstances, rm);

		ArrayList<Instance> filteredTargets = new ArrayList<Instance>();
		Instances filteredAllInstances = new Instances(tmp, allInstances.numInstances());
		 HashMap<Instance, Instance> filteredToOriginalTargets = getFilteredToOriginal(targets);
		 filteredTargets.addAll(filteredToOriginalTargets.keySet());
		 HashMap<Instance, Instance> filteredToOriginalAllInstances = getFilteredToOriginal(allInstances);
		 filteredAllInstances.addAll(filteredToOriginalAllInstances.keySet());
		 HashMap<String, Instance> filteredStringToOriginalInstances = getFilteredStringToOriginal(allInstances);
		LinearNNSearch knn = new LinearNNSearch(filteredAllInstances);
		knn.setSkipIdentical(true);
		for (Instance filteredTarget:filteredTargets){ 
			Instances nearestInstances= knn.kNearestNeighbours(filteredTarget, k);
			Instances tmp2 =  unFilterIndex(allInstances,nearestInstances, filteredStringToOriginalInstances);
			Instance tmp1 = filteredToOriginalTargets.get(filteredTarget);
			targetsToNeighbours.put(tmp1, tmp2);
		}
		return targetsToNeighbours;
	}
	
	
	

	public static Instances runNNsearchForInstancesPerClass(Instances allInstances, Instance target, int k) throws Exception{
		Remove rm = new Remove();
		rm.setAttributeIndices("1");
		rm.setInputFormat(allInstances);
		Instances tmp = Filter.useFilter(allInstances, rm);
		Instances filteredAllInstances = new Instances(tmp, allInstances.numInstances());
		rm.input(target);
		Instance filteredTarget = rm.output();
		HashMap<Instance, Instance> filteredToOriginalAllInstances = getFilteredToOriginal(allInstances);
		filteredAllInstances.addAll(filteredToOriginalAllInstances.keySet());
		HashMap<String, Instance> filteredStringToOriginalInstances = getFilteredStringToOriginal(allInstances);
		LinearNNSearch knn = new LinearNNSearch(filteredAllInstances);
		knn.setSkipIdentical(true);
		Instances nearestInstances= knn.kNearestNeighbours(filteredTarget, k);
		Instances tmp2 =  unFilterIndex(allInstances,nearestInstances, filteredStringToOriginalInstances);
		return tmp2;
	}
	
//	private static Instances filterByClass(Instances instances, String classValue) {
//		
//		Instances filteredInstances = new Instances(instances, instances.size());
//		for (Instance instance:instances) {
//			String thisClassValue = instance.stringValue(instance.classIndex());
//			if  (thisClassValue.equals(classValue) ||  thisClassValue.equals("none")){
//				filteredInstances.add(instance);
//			}
//		}
//		return filteredInstances;
//	}
	
	private static Instances unFilterIndex(Instances originalSet, Instances filteredNeighbourSet, HashMap<String, Instance> filteredStringToOriginalInstances) throws Exception {
		Instances unfilteredNeighbourSet = new Instances(originalSet, filteredNeighbourSet.numInstances());
		for (Instance filteredNeighbour:filteredNeighbourSet) {
			Instance result = filteredStringToOriginalInstances.get(hashInstance(filteredNeighbour));
			if (result != null) {
				unfilteredNeighbourSet.add(result);
			}
			else  {
				System.out.println("this was null;");
			}
			
			
		}
		return unfilteredNeighbourSet;
		
	}

	private static HashMap<Instance, Instance> getFilteredToOriginal(Instances originalInstances) throws Exception{
		Remove rm = new Remove();
		rm.setAttributeIndices("1");
		rm.setInputFormat(originalInstances);

		HashMap<Instance, Instance> filteredToOriginal = new HashMap<Instance, Instance>();
		for (Instance original:originalInstances){
			rm.input(original);
			filteredToOriginal.put( rm.output(), original);
		}
		return filteredToOriginal;
		
	}
	

	
	private static HashMap<String, Instance> getFilteredStringToOriginal(Instances originalInstances) throws Exception{
		HashMap<String, Instance> filteredStringToOriginal = new HashMap<String, Instance>();
		Remove rm = new Remove();
		rm.setAttributeIndices("1");
		rm.setInputFormat(originalInstances);
		for (Instance original:originalInstances){
			rm.input(original);
			filteredStringToOriginal.put( hashInstance(rm.output()), original);
		}
		return filteredStringToOriginal;
	}

	/**
	 * this is very dirty and I admit my shame. 
	 * @param instance
	 * @return
	 */
	private static String hashInstance(Instance instance){
		String hash ="";
		for (int i=0; i <instance.numAttributes() ; i++){
			hash+=instance.toString(i);
		}
		return hash;
	}

	

}

class NeighbourAndDistance {
	Candidate neighbour = null;
	Double distance = 0.0;
	public NeighbourAndDistance(Candidate cand, Double dist){
		neighbour = cand;
		distance = dist;
	}
	
}

class NeighbourComparator implements Comparator<NeighbourAndDistance> {

	@Override
	public int compare(NeighbourAndDistance o1, NeighbourAndDistance o2) {
		return o1.distance.compareTo(o2.distance);
	}
	
}
