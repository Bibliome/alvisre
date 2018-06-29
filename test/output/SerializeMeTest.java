package output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import corpus.Document;
import corpus.ID;
import corpus.ID.Type;
import corpus.NEtype;
import corpus.NamedEntity;
import corpus.Relation;
import corpus.RelationArgument;
import corpus.RelationType;
import corpus.Sentence;
import corpus.SyntacticRelation;
import corpus.SyntacticRelationArgument;
import corpus.SyntacticRelationType;
import corpus.Tag;
import corpus.Word;
import io.output.SerializeMe;
import representation.Path;
public class SerializeMeTest {
	ID a_wid1 ;
	ID a_wid2 ;
	ID a_wid3 ;
	ID a_wid4 ;
	ID a_tid1 ;
	Word a_w1 ;
	Word a_w2 ;
	Word a_w3 ;
	Word a_w4 ;
	NEtype fake ;
	NamedEntity a_t1 ;
	NamedEntity a_t2 ;
	Sentence a_s ;
	SyntacticRelationArgument a_sr1_a1 ;
	SyntacticRelationArgument a_sr1_a2 ;
	SyntacticRelationType a_sr1_type ;
	SyntacticRelation a_sr1 ;
	SyntacticRelationType a_sr2_type ;
	SyntacticRelationArgument a_sr2_a2 ;
	SyntacticRelation a_sr2 ;
	RelationArgument a_a1 ;
	RelationArgument a_a2 ;
	List<ID> a_path ;
	Path a_p ;
	Relation a_r;
	RelationType rtype;
	Tag noun = new Tag("N") ;
	Tag verb = new Tag("V");
	SerializeMe spath = new SerializeMe("/tmp/");
	Document d ;

	
	@Test
	public void testWritePath() throws IOException {
		setup();
//		spath.writePath(a_p);
	}

	@Test
	public void testReadPath() throws ClassNotFoundException, IOException {
		setup();
//		spath.writePath(a_p);
		@SuppressWarnings("unused")
		Path path = spath.readPath("1");
//		assertEquals(path.relation.type.type, a_p.relation.type.type);
	}
	public void setup() {
		d = new Document("D1");
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
		a_t1 = new NamedEntity(fake, 0, 3, a_tid1, "");
		a_t2 = new NamedEntity(fake, 8, 10, a_tid1, "");
		a_w1.addTag(noun);
		a_w2.addTag(verb);
		a_w4.addTag(noun);
		a_s = new Sentence("s1", Arrays.asList(a_w1, a_w2, a_w3, a_w4), d);
		a_s.namedEntities.addAll(Arrays.asList(a_t1,a_t2));
		a_sr1_a1 = new SyntacticRelationArgument(a_w4); // cat
		a_sr1_a2 = new SyntacticRelationArgument(a_w2); // has
		a_sr1_type = new SyntacticRelationType("obj:V-N");
		a_sr1 = new SyntacticRelation(a_sr1_type, a_sr1_a2, a_sr1_a1, new ID(1, Type.relation));
		a_sr2_type = new SyntacticRelationType("subj:N-V");
		a_sr2_a2 = new SyntacticRelationArgument(a_w1); // Jack
		a_sr2 = new SyntacticRelation(a_sr2_type, a_sr2_a2, a_sr1_a2, new ID(2, Type.relation));
		a_s.syntacticRelations.addAll(Arrays.asList(a_sr1, a_sr2));
		a_a1 = new RelationArgument(a_t1, "arg");
		a_a2 = new RelationArgument(a_t2, "arg");
		a_path = Arrays.asList(a_w1.wid, a_sr1.rid, a_w2.wid, a_sr2.rid, a_w4.wid);
		rtype = new RelationType("typissimo");
		a_r = new Relation(rtype, a_a1, a_a2, new ID("R15"));
	
	}
	
	@Test
	public void testReadPaths() throws ClassNotFoundException, IOException{
		SerializeMe serialLLL = new SerializeMe("/home/dvalsamou/workspace/re-kernels/data/LLL/zorana/manual/io.output/labeled/");
		ArrayList<Path> paths = serialLLL.readPaths();
		for (Path path:paths){
			if (path.relation != null){
				System.out.println("zito!");
			}
		}
		System.out.println("kaka");
		
	}

}
