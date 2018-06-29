package corpus;

import java.io.Serializable;

public class NEtype extends SchemaType implements Serializable{
	private static final long serialVersionUID = 1L;
	public String type;
	
	public NEtype(String netype) {
		type = netype;
		}
	
	public String getName(){
		return type;
	}

}

