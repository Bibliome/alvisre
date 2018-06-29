package representation;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.ParseException;

import corpus.Document;
import corpus.ID;
import corpus.ID.Type;
import corpus.NEtype;
import corpus.NamedEntity;
import corpus.RelationArgument;
import corpus.RelationType;
import corpus.Schema;
import corpus.Sentence;
import corpus.SyntacticRelation;
import corpus.SyntacticRelationArgument;
import corpus.SyntacticRelationType;
import corpus.Tag;
import corpus.Word;
/**
 * reference: A Shortest Path Dependency Kernel for Relation Extraction by Bunescu and Mooneys
 * @author dvalsamou
 *
 */
public class ShortestPathfromDependencyGraphTest {
	ID a_wid1;
	ID a_wid2;
	ID a_wid3;
	ID a_wid4;
	ID a_tid1;
	Word a_w1;
	Word a_w2;
	Word a_w3;
	Word a_w4;
	NEtype fake;
	NamedEntity a_t1;
	NamedEntity a_t2;
	Sentence a_s;
	SyntacticRelationArgument a_sr1_a1;
	SyntacticRelationArgument a_sr1_a2;
	SyntacticRelationType a_sr1_type;
	SyntacticRelation a_sr1;
	SyntacticRelationType a_sr2_type;
	SyntacticRelationArgument a_sr2_a2;
	SyntacticRelation a_sr2;
	RelationArgument a_a1;
	RelationArgument a_a2;
	List<ID> a_path;
	Path a_p;
	Tag noun = new Tag("N");
	Tag verb = new Tag("V");
	Schema schema;
	Document d = new Document("fake");

//	@Test
//	public void testfindAllConnectedCandidates() throws ParseException {
//		setup();
//		ShortestDependencyPath sp = new ShortestDependencyPath(a_s,d);
//		List<ID> list = sp.findPathBetweenTerms(a_s, a_t1, a_t2, false);
//		assertFalse(list.isEmpty());
//		PrintStuff.printSentenceAndPathText(a_s, list);
//	}
//
//	
	
//	@Test
//	public void testFindPossibleCandidates() throws ParseException{
//		setup();
//		ShortestDependencyPath sp = new ShortestDependencyPath(a_s,d);
//		List<representation.ShortestDependencyPath.Couple> cp  = sp.findPossibleCandidates(a_s,schema.DefinedTypes,schema.DefinedRelationTypes);
//		assertFalse(cp.isEmpty());
//		assertTrue(cp.get(0).arg1.equals(a_t1) || cp.get(0).arg2.equals(a_t1) );
//		assertTrue(cp.get(0).arg1.equals(a_t2) || cp.get(0).arg2.equals(a_t2) );
//
//	}
	
//	@Test
//	public void testCalculateTransformation() throws ParseException {
//		setup();
//		ShortestDependencyPath sp = new ShortestDependencyPath(a_s,d);
//
//		assertNotNull(schema);
//		List<Candidate> cands = sp.CalculateTransformation(schema);
//		assertFalse(cands.isEmpty());
//		PrintStuff.printSentenceAndCandidatesText(cands);
//	}
	


	public void setup() throws ParseException {
		
		a_wid1 = new ID("W1");
		a_wid2 = new ID("W2");
		a_wid3 = new ID("W3");
		a_wid4 = new ID("W4");
		a_tid1 = new ID("T1");
		a_w1 = new Word("Jack", 0, 3, a_wid1);
		a_w2 = new Word("has", 4, 6, a_wid2);
		a_w3 = new Word("a", 7, 7, a_wid3);
		a_w4 = new Word("cat", 8, 10, a_wid4);
		fake = new NEtype("Fake");
		schema = new Schema();
		schema.addType(fake);
		schema.addRelationType(new RelationType("fakerelation", null, Arrays.asList(fake),  null, Arrays.asList(fake)));
		a_t1 = new NamedEntity(fake, 0, 3, a_tid1, "");
		a_t2 = new NamedEntity(fake, 8, 10, a_tid1, "");
		a_w1.addTag(noun);
		a_w2.addTag(verb);
		a_w4.addTag(noun);
		a_s = new Sentence("s1", Arrays.asList(a_w1, a_w2, a_w3, a_w4),d);
		a_s.namedEntities.addAll(Arrays.asList(a_t1, a_t2));
		a_sr1_a1 = new SyntacticRelationArgument(a_w4); // cat
		a_sr1_a2 = new SyntacticRelationArgument(a_w2); // has
		a_sr1_type = new SyntacticRelationType("obj:V-N");
		a_sr1 = new SyntacticRelation(a_sr1_type, a_sr1_a2, a_sr1_a1, new ID(1,
				Type.relation));
		a_sr2_type = new SyntacticRelationType("subj:N-V");
		a_sr2_a2 = new SyntacticRelationArgument(a_w1); // Jack
		a_sr2 = new SyntacticRelation(a_sr2_type, a_sr2_a2, a_sr1_a2, new ID(2,
				Type.relation));
		a_s.syntacticRelations.addAll(Arrays.asList(a_sr1, a_sr2));
		a_a1 = new RelationArgument(a_t1, "arg");
		a_a2 = new RelationArgument(a_t2, "arg");
		a_path = Arrays.asList(a_w1.wid, a_sr1.rid, a_w2.wid, a_sr2.rid,
				a_w4.wid);

	}

}
