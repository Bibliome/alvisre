package corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class SchemaTest {

	@Test
	public void testGetRelationTypeByName() {
		RelationType rt = new RelationType("kot");
		Schema sc = new Schema();
		sc.addRelationType(rt);
		assertEquals(rt, sc.getRelationTypeByName("kot"));
		assertNull(sc.getRelationTypeByName("kaka"));
		Schema nc = new Schema();
		assertNull(nc.getRelationTypeByName("kaka"));
	}

	@Test
	public void testGetNETypeByName() {
		NEtype nt = new NEtype("kaka");
		Schema sc = new Schema();
		sc.addType(nt);
		assertEquals(nt, sc.getNETypeByName("kaka"));
		assertNull(sc.getNETypeByName("kdaka"));
		Schema nc = new Schema();
		assertNull(nc.getNETypeByName("kada"));
	}
	
	@Test
	public void testRelOnRels(){
		RelationType srt = new RelationType("simpleRel");
		Schema sc = new Schema();
		sc.addRelationType(srt);
		RelationType crt = new RelationType("complexRel");
		sc.addRelationType(crt);
		NEtype nt = new NEtype("entity");
		sc.addType(nt);
		ArrayList<NEtype> neargs = new ArrayList<NEtype>();
		neargs.add(nt);
		ArrayList<RelationType> relargs = new ArrayList<RelationType>();
		relargs.add(srt);
		srt.updateRelation("arg1", neargs, "arg2", neargs);
		crt.updateRelation("relation", relargs, "entity", neargs);
		assertTrue(crt.arg1types.contains(srt));
		assertFalse(crt.arg1types.contains(nt));
		assertTrue(crt.arg2types.contains(nt));
	}

}
