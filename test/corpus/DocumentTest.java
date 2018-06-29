/**
 * 
 */
package corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author dialecti
 *
 */
public class DocumentTest {

	/**
	 * Test method for {@link corpus.Document#searchSentenceByID(java.lang.String)}.
	 */
	@Test
	public void testSearchSentenceByID() {
		Document d = new Document("kaka");
		Sentence mitsos = new Sentence("kokoriko",d);
		d.addSentence(mitsos);
		assertEquals(mitsos, d.searchSentenceByID("kokoriko"));
//		assertNull(d.searchSentenceByID(null));
		assertNull(d.searchSentenceByID("kakavia"));
	}

}
