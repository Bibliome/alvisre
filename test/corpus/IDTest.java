/**
 * 
 */
package corpus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import corpus.ID.Type;




/**
 * @author dialecti
 *
 */
@SuppressWarnings("unused")
public class IDTest {

	/**
	 * Test method for {@link corpus.ID#getMixID()}.
	 */
	@Test
	public void testGetMixID() {
		ID gata = new ID (new Integer(23), Type.textbound);
		assertEquals("T23", gata.getMixID());
	}

	/**
	 * Test method for {@link corpus.ID#setMixID(java.lang.String)}.
	 */
	@Test
	public void testSetMixID() {
		ID arnia = new ID("W155");
		assertEquals(Type.textbound, arnia.type);
		assertEquals(new Integer(155), arnia.id);
	}
//	
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDStringNull(){	
//		@SuppressWarnings("unused")
//		ID kotes = new ID (null);
//		
//	}
//
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDNoNumberPart(){	
//		@SuppressWarnings("unused")
//		ID kotes = new ID ("W");
//		
//	}
//	
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDBothNull(){	
//		@SuppressWarnings("unused")
//		ID kotes = new ID (null,null);
//		
//	}
//	
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDStringEmpty(){	
//		@SuppressWarnings("unused")
//		ID kotes = new ID ("");
//		
//	}
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDOnlyNumberNull(){	
//		@SuppressWarnings("unused")
//		ID kotes = new ID(null, Type.textbound);
//		
//	}
//	
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDOnlyTypeNull(){	
//		@SuppressWarnings("unused")
//		ID kotes = new ID (new Integer(23),null);
//		
//	}
//	
//	@Test(expected=UnexpectedDatasetFormat.class)
//	public void testSetMixIDInvalidStartsWith(){		
//		@SuppressWarnings("unused")
//		ID kotes = new ID ("F123");
//	}
//
//




	
	
	
}
