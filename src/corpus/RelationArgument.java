package corpus;

import java.io.Serializable;

public class RelationArgument implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public NamedEntity argument;
	public String role;

//	public RelationArgument(NamedEntity t){
//		this(t,null);
//	}
//	
	public RelationArgument(NamedEntity t, String n){
		argument = t;
		role = n;
	}
}

