package corpus;

import java.io.Serializable;

public class Relation  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6153918603529836644L;
	public RelationType type;
	public RelationArgument arg1;
	public RelationArgument arg2;
	public ID eid;
	
	public Relation(RelationType rtype, RelationArgument argument1,  RelationArgument argument2, ID eID){
		type = rtype;
		eid = eID;
		if (!rtype.type.equals("none")) {
			if (argument1.role.equals(rtype.arg1role) && argument2.role.equals(rtype.arg2role)){
				arg1 = argument1;
				arg2 = argument2;
			}
			else if (argument1.role.equals(rtype.arg2role) && argument2.role.equals(rtype.arg1role)){
				arg1 = argument2;
				arg2 = argument1;
				System.out.println("Warning: arguments were reversed in "+eID);
			}
			else {
//				throw (new UnexpectedDatasetFormat("Roles not matching in relation definition: "+ eID));
				arg1 = argument1;
				arg2 = argument2;
			}
		}
		else {
			arg1 = argument1;
			arg2 = argument2;
		}
	}
	
/* TODO: For the moment there exists no check to see if the relation we are declaring has the correct arguments for its type*/
	
	public Boolean equals(Relation r2) {
		if (!arg1.equals(r2.arg1)) return false;
		if (!arg2.equals(r2.arg2)) return false;
		if (!type.equals(r2.type)) return false;
		return true;
	}
	
	
	
}
