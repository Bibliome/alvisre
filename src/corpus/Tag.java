package corpus;

import java.io.Serializable;

public class Tag  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String label;
	public String layer;
	public Tag(String tagLabel, String tagLayer) {
		label = tagLabel;
		layer = tagLayer;
	}	
	
	public Tag(String tagLabel){
		this(tagLabel,"Syntax");
	}


}
