package representation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import corpus.NamedEntity;
import corpus.Sentence;
import corpus.SyntacticRelation;
import corpus.SyntacticRelationArgument;
import corpus.Word;

/*************************************************************************
 * 
 * UNDirected graph of syntactic relation arguments (words or namedEntities), where the
 * edge weights are the syntactic relation types desired weight.
 * 
 *************************************************************************/

public class Graph {

	private int V; // number of vertices
	private int E; // number of edges
	private List<SyntacticRelationArgument> nodes = new ArrayList<SyntacticRelationArgument>();
	private List<SyntacticRelation> edges = new ArrayList<SyntacticRelation>();
	private HashMap<SyntacticRelationArgument, List<SyntacticRelationArgument>> neighboursOf = new HashMap<SyntacticRelationArgument, List<SyntacticRelationArgument>>();
	public Sentence s;

	/*******************************************************************
	 * Read in a graph from a file, bare bones error checking. V E node: id x y
	 * edge: from to
	 *******************************************************************/

	public Graph(Sentence s, boolean synterms) {
		this.s = s;
		createNodes(s, synterms);
		V = nodes.size();
		createNeighboursAndEdges(s);
		E = edges.size();
//		Integer kaka = neighboCursOf.size();
//		Set<Entry<SyntacticRelationArgument, List<SyntacticRelationArgument>>> nana = neighboursOf.entrySet();
//		Integer koko = nana.size();
//		if (!kaka.equals(koko)) System.out.println("kaka!!!!!!!!!");
//		Set<SyntacticRelationArgument> kiset = neighboursOf.keySet();
//		Integer keke = kiset.size();
//		System.out.println(keke);
//		SyntacticRelationArgument kotsos = null;
//		for (SyntacticRelationArgument arg:kiset){
//			if(arg.argw.wid.id.equals(22)){
//				kotsos = arg;
//			}
//		}
//System.out.println("done");
//List<SyntacticRelationArgument> neis = neighboursOf.get(kotsos);
//System.out.println("neis"+neis.size());
	}

	public int V() {
		return V;
	}

	public int E() {
		return E;
	}

	public double distance(SyntacticRelationArgument arg1, SyntacticRelationArgument arg2, Double wordpathCost) {
		//support for self-loops
		if (arg1.equals(arg2)) return 1;
		//support for self-loops
		SyntacticRelation sr = getEdge(arg1, arg2);
		if (sr.type.type.toLowerCase().equals("wordpath")) {
			return wordpathCost; 
		}
		// TODO: if we want weights, that's where they come in
		return 1;
	}

	public List<SyntacticRelationArgument> getNodes() {
		return nodes;
	}

	public List<SyntacticRelation> getEdges() {
		return edges;
	}

	public List<SyntacticRelationArgument> getNeighbours(SyntacticRelationArgument node) {
		return neighboursOf.get(node);
	}
	
	public SyntacticRelation getEdge(SyntacticRelationArgument arg1, SyntacticRelationArgument arg2){
		for (SyntacticRelation edge:edges){
			if ((edge.arg1.equals(arg1) && edge.arg2.equals(arg2) ) ||(edge.arg1.equals(arg2) && edge.arg2.equals(arg1) ))  return edge;
		}
		return null;
	}

	private void createNeighboursAndEdges(Sentence s) {
		for (SyntacticRelation relation : s.syntacticRelations) {
			if (!relation.isDuplicate(edges)){
				SyntacticRelationArgument arg1 = relation.arg1;
				SyntacticRelationArgument arg2 = relation.arg2;
				List<SyntacticRelationArgument> neighsOFarg1 = (neighboursOf.get(arg1) == null) ? new ArrayList<SyntacticRelationArgument>() : neighboursOf.get(arg1);
				List<SyntacticRelationArgument> neighsOFarg2 = (neighboursOf.get(arg2) == null) ? new ArrayList<SyntacticRelationArgument>() : neighboursOf.get(arg2);
				if (!neighsOFarg1.contains(arg2)) {
					neighsOFarg1.add(arg2);
				}
				if (!neighsOFarg2.contains(arg1)) {
					neighsOFarg2.add(arg1);
				}
				//adding self-loops
				if (!neighsOFarg1.contains(arg1)) {
					neighsOFarg1.add(arg1);
				}
				if (!neighsOFarg2.contains(arg2)) {
					neighsOFarg2.add(arg2);
				}
				//self-loops
				neighboursOf.put(arg1, neighsOFarg1);
				neighboursOf.put(arg2, neighsOFarg2);
				edges.add(relation);
			}
			else {
				if (!relation.type.type.equals("Anaphora"))	System.err.println("Duplicate in sentence "+s.id+": R"+relation.rid.id);
			}
		}
	}
	
	public List<SyntacticRelation> getSyntacticRelationsHaving(SyntacticRelationArgument arg){
		List<SyntacticRelation> having = new ArrayList<SyntacticRelation>();
		for (SyntacticRelation r:edges){
			if (r.arg1.equals(arg) || r.arg2.equals(arg)){
				having.add(r);
			}
		}
		return having;
	}

	private void createNodes(Sentence s, Boolean synterms) {
		List<Word> words = s.words;
		if (synterms) {
			List<NamedEntity> namedEntities = s.namedEntities;
			for (NamedEntity t : namedEntities) {
				if (s.asSyntacticRelationArgument(t) != null) {
					nodes.add(s.asSyntacticRelationArgument(t));
				}
			}
		}
		for (Word w : words) {
			if (s.asSyntacticRelationArgument(w) != null) {
				nodes.add(s.asSyntacticRelationArgument(w));
			}
		}
	}

}
