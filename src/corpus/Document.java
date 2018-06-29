package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Document  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 922318810042270130L;
	public List<Sentence> sentences = new ArrayList<Sentence>();
	public List<Section> sections = new ArrayList<Section>();
	public List<Paragraph> paragraphs = new ArrayList<Paragraph>();
	public String id;
	public String text ="";
	
	public Document (Document d){
		for (Sentence s: d.sentences){
			addSentence(s);
		}
		id = d.id;
		text = d.text;
	}
	public Document(String id){
		this.id = id;
	}
	
	public Document (String id, String t){
		this.id = id;
		text =t;
	}
	
	public void addSentence (Sentence... sents){
		for (Sentence s:sents){
			sentences.add(s);
		}
	}
	
	public void addParagraph (Paragraph... parags){
		for (Paragraph p:parags){
			paragraphs.add(p);
		}
	}
	
	public void addSection (Section... secs){
		for (Section s:secs){
			sections.add(s);
		}
	}
	
	public Sentence searchSentenceByID(ID sid){
		for (Sentence s:sentences){
			if (s.id.equals(sid.getMixID())){
				return s;
			}
		}
		return null;
	}
	public Sentence searchSentenceByID(String sid){
		for (Sentence s:sentences){
			if (s.id.equals(sid)){
				return s;
			}
		}
		return null;
	}
	
	
	
	public ArrayList<Sentence> searchSentenceByTermId(ID tid){
		ArrayList<Sentence> result = new ArrayList<>();
		for (Sentence s:sentences){
			if (s.getTermByID(tid.id)!= null) {
				result.add(s);
			}
		}
		return result;
	}
	
	public ArrayList<Word> searchWordByStartEnd(Integer start, Integer end){
		ArrayList<Word> result = new ArrayList<Word>();
		for (Sentence s:sentences){
			if (isWithin(start, end, s.start, s.end)){
				result.addAll(s.searchWordByStartEnd(start, end));
			}
		}
		return result;
		
	}
	public Boolean isWithin(int sentstart, int sentend, int start, int end){
		if ((start >= sentstart)&& (end <= sentend)) return true;
		return false;

	}
	public ArrayList<NamedEntity> getTerms (){
		ArrayList<NamedEntity> namedEntities = new ArrayList<NamedEntity>();
		for (Sentence s : sentences){
			for (NamedEntity t: s.namedEntities) {
				if (!namedEntities.contains(t)) {
					namedEntities.add(t);
				}
			}
		}
		return namedEntities;
	}

}
