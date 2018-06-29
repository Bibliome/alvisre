package corpus;

import java.io.Serializable;

public class SyntacticRelationType implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8832792501777554029L;
	public String type;
	public Integer weight;
	public SyntacticRelationType(String rtype) {
		this(rtype,new Integer(1));
	}
	public SyntacticRelationType(String rtype, Integer w) {
		type = rtype;
		weight = w;
	}


}
