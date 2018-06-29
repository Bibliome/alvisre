package corpus;

import java.io.Serializable;

public class SyntacticRelationArgument implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9082187523601853898L;
	public Word argw;
	public NamedEntity argt;

	public SyntacticRelationArgument(Word w) {
		argw = w;
	}
	public SyntacticRelationArgument(NamedEntity t) {
		argt = t;
	}
	
	
}
