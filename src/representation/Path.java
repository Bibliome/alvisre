package representation;

import java.util.ArrayList;
import java.util.List;

import corpus.ID;
import corpus.Relation;
import corpus.RelationArgument;
import corpus.RelationType;
import corpus.Sentence;
import corpus.Word;

/**
 * A 'Candidate' is the actual vector we are going to use for the learning
 * algorithm. In this case it is a PATH. It is a vector representation of a
 * sentence as a path connecting two (candidate) arguments. For one sentence
 * there can be many candidates like that, for every relation that could
 * possibly be expressed in it.
 */
public class Path extends Candidate {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6508927237143587636L;
	protected ArrayList<ID> path;
	public List<ID> getPath() {
		return path;
	}

	public void setPath(List<ID> path) {
		this.path = new ArrayList<ID>(path);
	}

	//	static class ExportAttributes {
	//		static Attribute index = new Attribute("index");
	//		static Attribute arg1 = new Attribute("arg1",(FastVector) null);
	//		static Attribute arg2 = new Attribute("arg2", (FastVector) null);
	//		static Attribute path = new Attribute("path", (FastVector) null);
	//		static Attribute sentence = new Attribute("sentence", (FastVector) null);
	//		static Attribute relation = new Attribute("relation", (FastVector) null);
	//	}

	public Path(String index, RelationArgument arg1, RelationArgument arg2,
			List<ID> path, Sentence sentence, RelationType t) {
		this(index, arg1, arg2, path, null, sentence, t);
	}

	public Path(Path p){
		this(p.index,p.arg1,p.arg2, p.path, p.relation, p.sentence, p.candidateRelationType);
	}

	public Path(String index, RelationArgument a1, RelationArgument a2, List<ID> p, Relation r, Sentence s, RelationType t) {
		super(index, r, s,t);
		arg1 = a1;
		arg2 = a2;
		path = new ArrayList<ID>(p);
	}


	public void findRelation(Boolean isLabeled) {
		if (!isLabeled) {
			relation = null;
		}
		else {
			for (Relation r : sentence.getRelationsHaving(arg1)) {
				if (r.type.directional) {
					if ((r.arg2.argument.equals(arg2.argument)) && r.type.equals(candidateRelationType)) {
						relation = r;
					}
				}
				else {
					if ((r.arg1.argument.equals(arg2.argument) || r.arg2.argument.equals(arg2.argument)) && r.type.equals(candidateRelationType)) {
						relation = r;
					}
				}
			}
			if (relation==null){
				relation = new Relation(new RelationType("none"), arg1, arg2, new ID("R0"));
			}
		}
	}
//	
//	public Boolean equals(Couple other){
//		if (type.equals(other.type)) {
//			if (type.directional) {
//				return (arg1.equals(other.arg1) && arg2.equals(other.arg2));
//			}
//			else {
//				return ((arg1.equals(other.arg1) && arg2.equals(other.arg2)) || (arg2.equals(other.arg1) && arg1.equals(other.arg2)));
//			}
//			
//		}
//		return false;
//		
//		
////		return (arg1.equals(other.arg1) && arg2.equals(other.arg2) && type.equals(other.type));
//	}

	public ArrayList<Word> getWordsOnPath(){
		ArrayList<Word> words = new ArrayList<Word>();
		for (ID id:path){
			String idstring= id.getMixID();
			if (idstring.startsWith("T")){
				Word word = sentence.getWordByID(id.id);
				words.add(word);
			}
		}
		return words;
	}

	@Override
	public int getLength() {
		return path.size();
	}

	//	public Instance toInstance() {
	//		try {
	//			DenseInstance i = new DenseInstance(6);
	//			i.setValue(ExportAttributes.index, this.index);
	//			i.setValue(ExportAttributes.arg1,
	//					StringSerialization.toString(this.arg1));
	//			i.setValue(ExportAttributes.arg2,
	//					StringSerialization.toString(this.arg2));
	//			i.setValue(ExportAttributes.path,
	//					StringSerialization.toString(this.path));
	//			i.setValue(ExportAttributes.sentence,
	//					StringSerialization.toString(this.sentence));
	//			return i;
	//		} catch (IOException e) {
	//			throw new SerializationException(e);
	//		}
	//	}

}
