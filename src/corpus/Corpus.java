package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Corpus  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3620583095626435506L;
	public ArrayList<Document> documents = new ArrayList<Document>();
//	private HashMap<String, ArrayList<Candidate>> candidates = new HashMap<String, ArrayList<Candidate>>();
//	private HashMap<String, Instances> instanceSets = new HashMap<String, Instances>();
	
	public Corpus() {
	}
	
	public Corpus(Corpus c){
		for (Document d:c.documents){
			documents.add(d);
		}
	}
	public Corpus(ArrayList<Document>d){
		documents = d;
	}
	public void addDocument(Document... docs){
		for (Document d:docs){
			documents.add(d);
		}
	}
	public Document searchDocumentByID(String id){
		for (Document d:documents){
			if (d.id.equals(id)){
				return d;
			}
		}
		return null;
	}

	public void addDocument(List<ArrayList<Document>> asList) {
		// TODO Auto-generated method stub
		
	}
//	
////	public Instances getIsntances(String type){
//		return instanceSets.get(type);
//	}
//	
//	/**
//	 * @param type
//	 * @param instance
//	 */
//	public void addInstances(String type, Instances instance){
//		instanceSets.put(type, instance);
//	}
//	
//	/**
//	 * @param type
//	 * @param instance
//	 */
//	public void setInstance(String type, Instances instance){
//		instanceSets.put(type, instance);
//	}
//	
//	/**
//	 * @param type
//	 * @return
//	 */
//	public ArrayList<Candidate> getCandidates(String type){
//		return candidates.get(type);
//	}
//	/**
//	 * @param type
//	 * @param cands
//	 */
//	public void addCandidates(String type, ArrayList<Candidate> cands){
//		candidates.put(type, cands);
//	}
//	/**
//	 * for this Corpus set <type> candidates, type can be for example "train", "test", "labeled", etc
//	 */
//	/**
//	 * @param type
//	 * @param cands
//	 */
//	public void setCandidates(String type, ArrayList<Candidate> cands){
//		candidates.put(type, cands);
//	}

}
