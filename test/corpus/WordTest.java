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
public class WordTest {

	/**
	 * Test method for {@link corpus.Word#findTerm(corpus.Sentence)}.
	 */
	@Test
	public void testFindTerm() {
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
		NamedEntity t1 = new NamedEntity(fake,6,14,tid1,"");
		NamedEntity t2 = new NamedEntity(fake,8,14, tid1,"");
		Document d = new Document("D1");
		Sentence s = new Sentence("s1", Arrays.asList(w1,w2,w3,w4),Arrays.asList(t1,t2), d);
		assertNotNull(w1.findTerm(s));
		assertEquals(0,w1.findTerm(s).size());
		assertEquals(Arrays.asList(t1), w3.findTerm(s));
		assertEquals(Arrays.asList(t1), w4.findTerm(s));
	}

}
