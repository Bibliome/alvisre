package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Word implements Serializable {
	private static final long serialVersionUID = 1L;

	public String text;
	public Integer start,end;
	public ArrayList<Tag> tags = new ArrayList<Tag>();
	public ID wid;

	public Word(String wordText, Integer wordStart, Integer wordEnd, List<Tag> wordTags, ID wordID) {
		text = wordText;
		start = wordStart;
		end = wordEnd;
		tags = new ArrayList<Tag>( wordTags );
		wid = wordID;
	}
	
	public Word(String wordText, Integer wordStart, Integer wordEnd, ID wordID){
		this(wordText,wordStart,wordEnd,new ArrayList<Tag>(),wordID);

	}
	public void addTag(Tag extraTag){
		tags.add(extraTag);
	}

	
    public List<NamedEntity> findTerm (Sentence s){
/* If the word belongs to a term, return the term;
 * otherwise, return null.
 */
    	List<NamedEntity> namedEntities = s.namedEntities;
    	List<NamedEntity> result = new ArrayList<NamedEntity>();
    	for (NamedEntity namedEntity:namedEntities){
    		for (int i=0; i< namedEntity.getStart().size(); i++){
    			if (namedEntity.getStart(i) <= this.start && namedEntity.getEnd(i) >= this.end){
    				if (namedEntity.findWords(s).contains(this)){
    					result.add(namedEntity);
    				}
    			}
    		}
    	}
    	
    	return result;
    }
    
    public SyntacticRelationArgument asSyntacticRelationArgument () {
    	SyntacticRelationArgument s = new SyntacticRelationArgument(this);
    	return s;
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
    
    public void print(){
    	System.out.println("ID:"+ wid+" start: "+start+" end: "+end+" text: "+text+" (tags not listed)");
    }
}
