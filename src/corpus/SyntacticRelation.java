package corpus;

import java.io.Serializable;
import java.util.List;

import exceptions.UnexpectedDatasetFormat;

public class SyntacticRelation implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2667649444134861872L;
	public SyntacticRelationType type;
	public SyntacticRelationArgument arg1;
	public SyntacticRelationArgument arg2;
	public ID rid;
	public String layer;
	public String arg1role;
	public String arg2role;

	public SyntacticRelation(SyntacticRelationType synType, SyntacticRelationArgument synArg1, SyntacticRelationArgument synArg2, ID rID){
		this(synType,"argument",synArg1,"argument", synArg2,rID,"Syntax");
	}
	public SyntacticRelation(SyntacticRelationType synType, SyntacticRelationArgument synArg1, SyntacticRelationArgument synArg2, ID rID, String synLayer){
		this(synType, "argument",synArg1,"argument", synArg2, rID, synLayer);
	}
	public SyntacticRelation(SyntacticRelationType synType, String role1,SyntacticRelationArgument synArg1,String role2, SyntacticRelationArgument synArg2, ID rID){
		this(synType, role1, synArg1, role2, synArg2,rID, "Syntax");
	}

	public SyntacticRelation(SyntacticRelationType synType, String role1,SyntacticRelationArgument synArg1,String role2, SyntacticRelationArgument synArg2, ID rID, String synLayer){
		type = synType;
		arg1 = synArg1;
		arg1role = role1;
		arg2role = role2;
		arg2 = synArg2;
		rid = rID;		
		layer = synLayer;
	}
	public String getRole(SyntacticRelationArgument arg){
		if (arg.equals(arg1)) return arg1role;
		if (arg.equals(arg2)) return arg2role;
		throw new UnexpectedDatasetFormat("Asking role of inavlid argument.");
	}
	public Boolean isDuplicate(SyntacticRelation r){
		Boolean dup = false;
		if (r.arg1.equals(this.arg1) && (r.arg2.equals(this.arg2)) && (r.type.type.equals(this.type.type))){
			dup = true;
		}
		return dup;
	}
	
	public Boolean isDuplicate(List<SyntacticRelation> rels){
		Boolean dup = false;
		for (SyntacticRelation r:rels){
			dup = isDuplicate(r);
		}
		return dup;
	}

}
