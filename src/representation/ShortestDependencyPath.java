package representation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import corpus.Document;
import corpus.ID;
import corpus.NamedEntity;
import corpus.Sentence;
import corpus.SyntacticRelationArgument;
import corpus.Word;
import experiments.Experiment;

public class ShortestDependencyPath extends Transform {
	public ShortestDependencyPath(Sentence s, Document d) {
		super(s,d);
	}


	public List<ID> findPathBetweenTerms(Experiment exp,Sentence s, NamedEntity t1, NamedEntity t2, Boolean verbose) throws IOException {
		/*
		 */
		/*
		 * The boolean parameter indicates whether syntactic relations may exist
		 * between namedEntities (true) or if they only exist between words (false).
		 * 
		 * You don't need to call this it is called by
		 * findAllConnectedCandidates
		 */
//		List<String> printed = new ArrayList<>();
		List<Word> w1 = new ArrayList<Word>();
		List<Word> w2 = new ArrayList<Word>();
		w1.addAll(t1.findWords(s));
		w2.addAll(t2.findWords(s));
		List<ID> ids = new ArrayList<ID>();
		List<SyntacticRelationArgument> start = new ArrayList<SyntacticRelationArgument>();
		List<SyntacticRelationArgument> end = new ArrayList<SyntacticRelationArgument>();
		for (Word w : w1) {
			SyntacticRelationArgument arg = s.asSyntacticRelationArgument(w);
			if (arg != null) {
			start.add(arg);
			}
		}
		for (Word w : w2) {
			SyntacticRelationArgument arg = s.asSyntacticRelationArgument(w);
			if (arg != null) {
			end.add(arg);
			}
		}
//		if (b) {
//			start.add(t1.asSyntacticRelationArgument());
//			end.add(t2.asSyntacticRelationArgument());
//		}
		/*
		 * Start includes or the possible words + term that could represent the
		 * first argument (term) End is the same for the 2nd one
		 */
		Double wordpathCost = exp.getConf().getDoubleValue("wordpathCost");
		ids = ShortestPath(s, start, end, wordpathCost);
//		if (s.inRelation(t1, t2)){
//			positiveCoupleSize++;
//		}
//		if (ids == null){
//			String att = "";
//			if(t1.tid.id < t2.tid.id) att = t1.tid.id+":"+t2.tid.id;
//			else att = t2.tid.id+":"+t1.tid.id;
//			if (!printed.contains(att)){
//				if (verbose) exp.getDumper().print("Attention! No path found in sentence "+s.id+" between "+t1.tid.getMixID()+" and "+t2.tid.getMixID()+".");
//				if (s.inRelation(t1, t2)){
//					if (verbose)exp.getDumper().println(" POSITIVE");
//					positiveDisconnectedCoupleSize++;
//				}
//				else {
//					if (verbose)exp.getDumper().println(" NEGATIVE");
//				}
//				printed.add(att);
//			}
//			System.out.println("Null Path");
//		}
		return ids;
	}

	private List<ID> ShortestPath(Sentence s, List<SyntacticRelationArgument> start, List<SyntacticRelationArgument> end, Double wordpathCost) {
		/*
		 * 
		 * You don't need to call this, it is called by findPathBetweenTerms
		 */
		List<ID> list = new ArrayList<ID>();
		HashMap<Integer,List<ID>> lists2length = new HashMap<Integer,List<ID>>();
		for (SyntacticRelationArgument arg1 : start) {
			for (SyntacticRelationArgument arg2 : end) {
				Graph G = new Graph(s, false);
				Dijkstra djk = new Dijkstra(G);
				list = djk.getPath(arg1, arg2, wordpathCost);
				if (list.size() > 0) lists2length.put(list.size(), list);
//				System.out.println("Debug83");
			}
		}
		if (lists2length.size() == 0) return list;
		Integer minlength = Collections.min(lists2length.keySet());
		list = lists2length.get(minlength);
		return list;
	}



	




	

}
