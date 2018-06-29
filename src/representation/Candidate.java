package representation;

import java.io.Serializable;

import corpus.Document;
import corpus.Relation;
import corpus.RelationArgument;
import corpus.RelationType;
import corpus.Sentence;

/**
 * A 'Candidate' is the actual vector we are going to use for the learning algorithm. 
 * It is a vector representation of a sentence as a path connecting two (candidate) arguments. 
 * For one sentence there can be many candidates like that, for every relation that could possibly be expressed in it.  
 */
public abstract class Candidate implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2644710727056992759L;
	public Relation relation; //associated relation
	public String index;
	public Sentence sentence;
	public Document document;
	public RelationType candidateRelationType;
	public RelationArgument arg1;
	public RelationArgument arg2;

	
	public Candidate(String i, Relation r, Sentence s, RelationType t){
		this(i,r,s,s.document,t);
	}
	
	public Candidate(Candidate a){
		this(a.index,a.relation,a.sentence,a.document,a.candidateRelationType);
	}
	public Candidate(String i, Relation r, Sentence s, Document d){
		this (i,r,s,d,null);
	}
	
	public Candidate(String i, Relation r, Sentence s, Document d, RelationType t){
		index = i;
		relation = r;
		sentence = s;
		document = d;
		candidateRelationType = t;
	}
	
	public abstract void findRelation(Boolean isLabeled);
	
	public abstract int getLength();
}
