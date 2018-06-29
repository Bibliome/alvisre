package kernels;

import java.util.ArrayList;
import java.util.HashMap;

import corpus.Word;

public class TriggerWordTable extends HashMap<String, TriggerWords> {
	private static final long serialVersionUID = 1L;
	
	public TriggerWords getTriggerWords(String relationType) {
		if (this.isEmpty() || (this.get(relationType) == null)) return new TriggerWords();
		return this.get(relationType);
	}
	
	public Boolean isTriggerWord(String word){
		return this.values().contains(word);
	}
	
	/**
	 * Checks if a word is a trigger word and if yes returns the name of the relation (class) it is a trigger word for
	 * @param word
	 * @return
	 */
	public String getRelationType (String word){
		for (String rel : this.keySet()){
			if (getTriggerWords(rel).containsStem(word)) return rel;
			
		}
		return "";
	}
	/**
	 * for a set of words (Word type) it gives a set of words (String) that are active trigger words
	 * @param words
	 * @return
	 */
	public TriggerWords activeTriggerWords (ArrayList<Word> words){
		TriggerWords activeWords = new TriggerWords();
		for (Word w:words){
			if (isTriggerWord(w.text)){
				activeWords.add(w.text);
			}
		}
		return activeWords;
	}
	
	
	
	/**
	 * for a set of words (Word type) it decides whether it includes any trigger words expressing one type of relations
	 * @param relationType
	 * @param words
	 * @return
	 */
	public Boolean containsTriggerWord (String relationType, ArrayList<Word> words) {
		if (relationType.equals("none")) return false;
		for (Word w: words) {
			String text = w.text;
			for (String trigger: getTriggerWords(relationType)){
				if (text.startsWith(trigger)){
					return true;
				}
			}
		}
		return false;
	}
	
	public Double numOfTriggerWordOccurrences(String relationType,ArrayList<Word> words) {
		Double val = 0.0;
		if (relationType.equals("none")) return val;
		for (Word w: words) {
			String text = w.text;
			for (String trigger: getTriggerWords(relationType)){
				if (text.startsWith(trigger)){
					val+=1.0;
				}
			}
		}
		
		
		return val;
	}
	
}
