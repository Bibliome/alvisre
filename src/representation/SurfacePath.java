package representation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import corpus.Document;
import corpus.ID;
import corpus.NamedEntity;
import corpus.Sentence;
import corpus.Word;
import experiments.Experiment;

/**
 * This representation gives the list of IDs of the words that are found in the sentence between the two candidate arguments.
 * @author dvalsamou
 *
 */
public class SurfacePath extends Transform {


	public SurfacePath(Sentence s, Document d) {
		super(s, d);
	}

	public SurfacePath(Sentence s) {
		super(s);
	}
	
	public List<ID> findPathBetweenTerms(Experiment exp, Sentence s, NamedEntity t1,		NamedEntity t2, Boolean verbose) throws IOException {
//		List<String> printed = new ArrayList<>();
		List<Word> w1 = new ArrayList<Word>();
		List<Word> w2 = new ArrayList<Word>();
		w1.addAll(t1.findWords(s));
		w2.addAll(t2.findWords(s));
		List<ID> ids = new ArrayList<ID>();
		ids = WordPath(s, w1, w2);
//		if (s.inRelation(t1, t2)){
//			positiveCoupleSize++;
//		}
//		if (ids == null){
//			String att = "";
//			if(t1.tid.id < t2.tid.id) att = t1.tid.id+":"+t2.tid.id;
//			else att = t2.tid.id+":"+t1.tid.id;
//			if (!printed.contains(att)){
//				if (verbose) 	exp.getDumper().print("Attention! No path found in sentence "+s.id+" between "+t1.tid.getMixID()+" and "+t2.tid.getMixID()+".");
//				if (s.inRelation(t1, t2)){
//					if (verbose) exp.getDumper().println(" POSITIVE");
//					positiveDisconnectedCoupleSize++;
//				}
//				else {
//					if (verbose) exp.getDumper().println(" NEGATIVE");
//				}
//				printed.add(att);
//			}
//			System.out.println("Null Path");
//		}
		return ids;
	}

	private List<ID> WordPath(Sentence s, List<Word> w1, List<Word> w2) {
		List<ID> ids = new ArrayList<ID>();
		Collections.sort(w1, new WordComparator());
		Collections.sort(w2, new WordComparator());
		Word lastW1 = Collections.max(w1, new WordComparator());
		Word firstW1 = Collections.min(w1, new WordComparator());
		Word firstW2 = Collections.min(w2, new WordComparator());
		Word lastW2 =  Collections.max(w2, new WordComparator());
		if (firstW1.start > lastW2.end) {
			List<Word> tmp = w1;
			w1  =w2;
			w2= tmp;
			lastW1 = Collections.max(w1, new WordComparator());
			firstW2 = Collections.min(w2, new WordComparator());
		}
		ids.addAll(getWordIDs(w1));
		ids.addAll(getWordIDsBetweenPos(s.words, lastW1.end, firstW2.start));
		ids.addAll(getWordIDs(w2));
		return ids;
	}


	
	public class WordComparator implements Comparator<Word> {
	    @Override
	    public int compare(Word o1, Word o2) {
	        return o1.start.compareTo(o2.start);
	    }
	}
	
	public List<ID> getWordIDs (List<Word> words) {
		List<ID> ids = new ArrayList<ID>(words.size());
		for (Word w:words){
			ids.add(w.wid);
		}
		return ids;
	}
	
	public List<ID> getWordIDsBetweenPos (List<Word> words, int pos1, int pos2) {
		if (pos1 > pos2) {
			int tmp = pos1;
			pos1 = pos2;
			pos2 = tmp;
		}
		List<ID> ids = new ArrayList<ID>(words.size());
		Collections.sort(words, new WordComparator());
		for (Word w:words){
			if ((w.start >= pos1)&& (w.end <= pos2) ) ids.add(w.wid);
		}
		return ids;
	}
	

	

}
