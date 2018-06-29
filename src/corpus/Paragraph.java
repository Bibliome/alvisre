package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Paragraph  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5371111675520670980L;
	public List<Sentence> sentences = new ArrayList<Sentence>();

	public Paragraph() {
	}
	
	public Paragraph(List<Sentence> s) {
		sentences = s;
	}
	public void addSentence (Sentence... sentences){
		for (Sentence s:sentences){
			this.sentences.add(s);
		}
	}
	

}
