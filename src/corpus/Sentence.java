package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import representation.Candidate;

/** 
 * A 'sentence' is the representation of one (training/dev/test) example. 
 * It contains:
 * - words, which are entities that have text, IDs and tags and start-end char counters
 * - namedEntities, which are special words or combination of words, with a special tag (NEtype or Named Entity type)
 * - syntactic links between words
 * - relations between namedEntities (that we are looking to find)
 * - groups potentially (co-references, anaphora). (not implemented)
 * Taking all that data we are trying to find Candidates that we will attach to it. Candidates are all the possible 
 * relations that could be expressed in this sentence. They are actually connected (via syntactic paths) couples of namedEntities that could
 * be relation arguments. They will be fed to the learning algorithm, along with their possible labels (Relations). 
 */
public class Sentence implements Serializable {
	private static final long serialVersionUID = 1L;

	public ArrayList<Word> words = new ArrayList<Word>();
	public ArrayList<NamedEntity> namedEntities = new ArrayList<NamedEntity>();
	public ArrayList<SyntacticRelation> syntacticRelations = new ArrayList<SyntacticRelation>();
	public ArrayList<Relation> relations = new ArrayList<Relation>();
	public ArrayList<Candidate> candidates = new ArrayList<Candidate>();
	public String text = "";
	public String id;
	public int start;
	public int end;
	public Document document;

	public Sentence (String id, Document d){
		this(id,new ArrayList<Word>(), d);
	}
	public Sentence (String id, String t, Document d){
		this(id,d);
		text = t;
	}
	public Sentence (String id, List<Word> w, Document d){
		this(id,w,new ArrayList<NamedEntity>(),d);
	}
	public Sentence(String id, List<Word> w, List<NamedEntity> t, Document d){
		this(id,w,t,new ArrayList<SyntacticRelation>(),d);
	}
	public Sentence(String id, List<Word> w, List<NamedEntity> t, List<SyntacticRelation> sr, Document d){
		this(id,w,t,sr, new ArrayList<Relation>(),d);
	}
	public Sentence(String id, List<Word> w, List<NamedEntity> t, List<SyntacticRelation> sr, List<Relation> r,  Document d){
		this(id,w,t,sr,r,"", d);
	}
	public Sentence(String id, List<Word> w, List<NamedEntity> t, List<SyntacticRelation> sr, List<Relation> r, String txt,  Document d){
		this.id 				= id;
		this.words 				= new ArrayList<Word>(w);
		this.namedEntities 				= new ArrayList<NamedEntity>(t);
		this.syntacticRelations = new ArrayList<SyntacticRelation>( sr );
		this.relations 			= new ArrayList<Relation>( r );
		this.text 				= txt;
		this.document =d ;
	}

	public List<SyntacticRelationArgument> getSyntacticRelationArguments(){
		List<SyntacticRelationArgument> args = new ArrayList<SyntacticRelationArgument>();
		for (SyntacticRelation r:syntacticRelations){
			if (!args.contains(r.arg1)){
				args.add(r.arg1);
			}
			if (!args.contains(r.arg2)){
				args.add(r.arg2);
			}
		}
		return args;
	}
	public void setCandidates(List<Candidate> c){
		candidates = new ArrayList<Candidate>(c);

	}
	public List<SyntacticRelation> getSyntacticRelationsHaving(SyntacticRelationArgument arg){
		List<SyntacticRelation> having = new ArrayList<SyntacticRelation>();
		for (SyntacticRelation r:syntacticRelations){
			if (r.arg1.equals(arg) || r.arg2.equals(arg)){
				having.add(r);
			}
		}
		return having;
	}

	public List<Relation> getRelationsHaving(RelationArgument arg){
		List<Relation> having = new ArrayList<Relation>();
		for (Relation r:relations){
			if (r.arg1.argument.equals(arg.argument) || r.arg2.argument.equals(arg.argument)){
				having.add(r);
			}
		}
		return having;
	}

	public void setBoundaries(int start, int end){
		this.start = start;
		this.end = end;
	}

	public List<NamedEntity> getTermsByType(NEtype type) {
		List<NamedEntity> typeterms = new ArrayList<NamedEntity>();
		for (NamedEntity namedEntity:namedEntities){
			if (namedEntity.type.equals(type)) {
				typeterms.add(namedEntity);
			}
		}
		return typeterms;
	}

	public NamedEntity getTermByID (Integer id) {
		for (NamedEntity t:namedEntities){
			if (t.tid.id.equals(id)){
				return t;
			}
		}
		return null;
	}

	public Word getWordByID (Integer id){
		for (Word w:words){
			if (w.wid.id.equals(id)){
				return w;
			}

		}
		return null;
	}

	public SyntacticRelation getSyntacticRelationByID (Integer id){
		for (SyntacticRelation sr:syntacticRelations){
			if (sr.rid.id.equals(id)) {
				return sr;
			}
		}
		return null;
	}

	public SyntacticRelationArgument asSyntacticRelationArgument(NamedEntity t){
		for (SyntacticRelationArgument  sr:getSyntacticRelationArguments()) {
			if(t != null){
				if(t.equals(sr.argt)) return sr;
			}
		}
		return null;
	}

	public SyntacticRelationArgument asSyntacticRelationArgument(Word w){
		for (SyntacticRelationArgument  sr:getSyntacticRelationArguments()) {
			if(w != null){
				if(w.equals(sr.argw)) return sr;
			}
		}
		return null;
	}


	public SyntacticRelationArgument getSyntacticRelationArgument(Word w){
		for (SyntacticRelationArgument arg: getSyntacticRelationArguments()) {
			if (w.equals(arg.argw)){
				return arg;
			}
		}
		return null;
	}

	public SyntacticRelationArgument getSyntacticRelationArgument(NamedEntity t){
		for (SyntacticRelationArgument arg: this.getSyntacticRelationArguments()) {
			if (t.equals(arg.argt)){
				return arg;
			}
		}
		return null;
	}

	public void addWords(ArrayList<Word> newWords){
		for (Word w:newWords){
			if (!words.contains(w)){
				words.add(w);
			}
		}
	}

	/**
	 * This method tells us if this term (t) is a possible relation argument of any type of possible relation.
	 * @param t the term 
	 * @return null if it isn't, the argument if it is
	 */
	public RelationArgument getRelationArgument(NamedEntity t, String role){
		for (RelationArgument arg: this.getRelationArguments()) {
			if (t.equals(arg.argument) && (arg.role.equals(role))) {
				return arg;
			}
		}
		return new RelationArgument(t, role);
	}
	/**
	 * This method gives us all the possible relation arguments one could find in this sentence, regardless of type of relation.
	 * @return 
	 */
	public List<RelationArgument> getRelationArguments(){
		List<RelationArgument> args = new ArrayList<RelationArgument>();
		for (Relation r:relations){
			if (!args.contains(r.arg1)){
				args.add(r.arg1);
			}
			if (!args.contains(r.arg2)){
				args.add(r.arg2);
			}
		}
		return args;
	}

	public ArrayList<Word> searchWordByStartEnd(Integer start, Integer end){
		ArrayList<Word> result = new ArrayList<Word>();
		for (Word w:words){
			if (isWithin(start, end, w.start, w.end)){
				result.add(w);
			}
		}
		return result;
	}

	public Boolean isWithin(int sentstart, int sentend, int start, int end){
		if ((start >= sentstart)&& (end <= sentend)) return true;
		return false;

	}

	/**
	 * This method should update a sentences relations according to the prediction...
	 * @param candidates
	 */
	public void updateRelations(ArrayList<Candidate> candidates){
		relations = new ArrayList<>();
		for (Candidate c: candidates){
			if (c.sentence.equals(this)){
				if (c.relation != null){
					//					if (!hasMirror(c, relations)) {//is this a good idea? the order does count.
					relations.add(c.relation);
					//					}
				}

			}
		}
	}

	public void maskTerms(){
		for (NamedEntity t:namedEntities){
			for (Word w:t.findWords(this)){
				w.text = t.type.type;
				for (int i = 0; i< Math.min(2, w.tags.size()); i++){
					if (w.tags.get(i).layer.startsWith("surface") || w.tags.get(i).layer.startsWith("canonical"))	w.tags.get(i).label = w.text;
				}
			}
		}
	}
	public ArrayList<NamedEntity> getTermsForWord(Word argw) {
		ArrayList<NamedEntity> result = new ArrayList<>();
		for (NamedEntity t:namedEntities){
			if(t.findWords(this).contains(argw)) result.add(t);
		}
		return result;
	}

	public boolean inRelation(NamedEntity arg1, NamedEntity arg2, RelationType type){
		for (Relation r: relations){
			if (r.type.equals(type)) {
				if (arg1.equals(r.arg1.argument) && arg2.equals(r.arg2.argument)) {
					return true;
				}
			}
		}
		return false;

	}

	public boolean hasTermsOfType(String type){
		for (NamedEntity t:namedEntities){
			if (t.type.type.equals(type)) return true;
		}
		return false;
	}

	public void replaceByAnte(NamedEntity anaphor, NamedEntity ante){
		for (@SuppressWarnings("unused") SyntacticRelation relation:syntacticRelations){

		}

	}
	public boolean equals(Sentence other) {
		if (document.id.equals(other.document.id)&& id.equals(other.id)) return true; 
		return false;
	}

}