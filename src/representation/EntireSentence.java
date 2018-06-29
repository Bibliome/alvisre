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
 * This representation gives a list of the IDS of all words in the order they appear in the sentence. 
 * @author dvalsamou
 *
 */
public class EntireSentence extends Transform {
	/*
	 * Formerly known as Bag of Words
	 */

	public EntireSentence(Sentence s, Document d) {
		super(s, d);
	}

	public EntireSentence(Sentence s) {
		super(s);
	}





	public List<ID> findPathBetweenTerms(Experiment exp, Sentence s, NamedEntity t1,		NamedEntity t2, Boolean verbose) throws IOException {
//		List<String> printed = new ArrayList<>();
		List<Word> w1 = new ArrayList<Word>();
		List<Word> w2 = new ArrayList<Word>();
		w1.addAll(t1.findWords(s));
		w2.addAll(t2.findWords(s));
		List<ID> ids = new ArrayList<ID>();
		ids = SentenceWordIDs(s, w1, w2);
//		if (s.inRelation(t1, t2, )){
//			positiveCoupleSize++;
//		}
//		if (ids == null){
//			String att = "";
//			if(t1.tid.id < t2.tid.id) att = t1.tid.id+":"+t2.tid.id;
//			else att = t2.tid.id+":"+t1.tid.id;
//			if (!printed.contains(att)){
//				if (verbose) 	exp.getDumper().print("Attention! No path found in sentence "+s.id+" between "+t1.tid.getMixID()+" and "+t2.tid.getMixID()+".");
//				if (s.inRelation(t1, t2)){
//					if (verbose) 	exp.getDumper().println(" POSITIVE");
//					positiveDisconnectedCoupleSize++;
//				}
//				else {
//					if (verbose) 	exp.getDumper().println(" NEGATIVE");
//				}
//				printed.add(att);
//			}
//			System.out.println("Null Path");
//		}
		return ids;
	}

	private List<ID> SentenceWordIDs(Sentence s, List<Word> w1, List<Word> w2) {
		List<ID> ids = new ArrayList<ID>();
		//		Collections.sort(w1, new WordComparator());
		//		Collections.sort(w2, new WordComparator());
		ArrayList<Word> allWords = new ArrayList<Word>(sentence.words);
		Collections.sort(allWords, new WordComparator());
		Word lastAll = Collections.max(allWords, new WordComparator());
		Word firstAll = Collections.min(allWords, new WordComparator());
		//		ids.addAll(getWordIDs(w1));
		ids.addAll(getWordIDsBetweenPos(s.words, firstAll.start, lastAll.end));
		//		ids.addAll(getWordIDs(w2));
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
