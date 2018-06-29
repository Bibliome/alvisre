/**
 * 
 */
package corpus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Test;

/**
 * @author dialecti
 *
 */
public class TermTest {

	/**
	 * Test method for {@link corpus.NamedEntity#findWords(corpus.Sentence)}.
	 */
	@Test
	public void testFindWords() {
		//This is a sentence
		//0123 45 6 78901234
		//        <---T1--->
		//           <--T2->
		//W1 = 0-3
		//W2 = 4-5
		//W3 = 6
		//W4 = 7-14
		//T1 = 6-14
		//T2 = 8-14
		Document d = new Document("D1");
		ID wid1 = new ID("W1");
		ID wid2 = new ID("W2");
		ID wid3 = new ID("W3");
		ID wid4 = new ID("W4");
		ID tid1 = new ID("T1");
		Word w1 = new Word("This", 0, 3, wid1);
		Word w2 = new Word("is",4,5,wid2);
		Word w3 = new Word("a", 6,6, wid3);
		Word w4 = new Word("sentence",7,14, wid4);
		NEtype fake = new NEtype("Fake");
		NamedEntity t1 = new NamedEntity(fake,6,14,tid1, "");
		NamedEntity t2 = new NamedEntity(fake,8,14, tid1,"");
		Sentence s = new Sentence("s1", Arrays.asList(w1,w2,w3,w4), d);
		//NamedEntity who SHOULD give results
		assertEquals(Arrays.asList(w3,w4), t1.findWords(s));
		assertNotNull(t1.findWords(null));
		assertEquals(0,t1.findWords(null).size());
		
		//NamedEntity who SHOULDN'T give results
		assertNotNull(t2.findWords(s));
		assertEquals(0,t2.findWords(s).size());
	}


}
