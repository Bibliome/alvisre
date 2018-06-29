package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NamedEntity  implements Serializable {
	private static final long serialVersionUID = 1L;

	public NEtype type;
	private ArrayList<Integer> start = new ArrayList<Integer>();
	private ArrayList<Integer> end = new ArrayList<Integer>();
	public ID tid;
	public ArrayList<Word> words = new ArrayList<Word>();
	public ArrayList<Tag> tags = new ArrayList<Tag>();
	public String forOutput = "";


	public NamedEntity(NEtype termType, Integer termStart, Integer termEnd, ID tID, String line){
		type = termType;
		start.add(termStart);
		end.add(termEnd);
		tid = tID;
		forOutput = line.split("\\|")[0];
	}
	public void addWords(Word...words){
		for (Word w:words){
			this.words.add(w);
		}
	}

	public void addStartEnd(Integer start, Integer end){
		this.start.add(start);
		this.end.add(end);
	}

	public List<Word> findWords(Sentence s){
		if (s != null) {
			for (Word w : s.words) {
				for (int i = 0; i< start.size(); i++){
					if (w.start >= start.get(i) && w.end <= end.get(i)) {
						if (!words.contains(w)) {
							words.add(w);
						}
					}
					if (w.start <= start.get(i) && w.end >= end.get(i)) {
						if (!words.contains(w)) {
							words.add(w);
						}
					}				
				}
			}
			return words;
		}
		return new ArrayList<Word>();
	}

	public SyntacticRelationArgument asSyntacticRelationArgument () {
		SyntacticRelationArgument s = new SyntacticRelationArgument(this);
		return s;
	}
	public void addTag(Tag extraTag){
		tags.add(extraTag);
	}

	public ArrayList<Integer> getStart(){
		return start;
	}
	public Integer getStart(int i){
		return start.get(i);
	}

	public Integer getEnd (int i){
		return end.get(i);
	}
	public ArrayList<Integer> getEnd(){
		return end;
	}
	public String getTagValue(String layer){
		String label = null;
		for (Tag t:tags){
			if (t.layer.equals(layer)){
				label = t.label;
			}
		}
		return label;
	}

}
