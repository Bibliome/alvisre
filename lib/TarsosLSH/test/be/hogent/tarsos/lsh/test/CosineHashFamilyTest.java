/*
*      _______                       _        ____ _     _
*     |__   __|                     | |     / ____| |   | |
*        | | __ _ _ __ ___  ___  ___| |    | (___ | |___| |
*        | |/ _` | '__/ __|/ _ \/ __| |     \___ \|  ___  |
*        | | (_| | |  \__ \ (_) \__ \ |____ ____) | |   | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|   |_|
*                                                         
* -----------------------------------------------------------
*
*  TarsosLSH is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info    : http://tarsos.0110.be/tag/TarsosLSH
*  Github  : https://github.com/JorenSix/TarsosLSH
*  Releases: http://tarsos.0110.be/releases/TarsosLSH/
* 
*/

package be.hogent.tarsos.lsh.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import be.hogent.tarsos.lsh.families.CosineHashFamily;

public class CosineHashFamilyTest {
	
	@Test
	public void testCombination(){
		CosineHashFamily chf = new CosineHashFamily(4);
		           //1 2 4 8
		int[] test= {0,0,0,1};
		int result = chf.combine(test);
		assertEquals("Expected 8 when combining 0001 (= 0 + 0 + 0 + 8).",8,result);
		
                        //1 2 4 8
		int[] otherTest= {1,0,0,1};
		int otherResult = chf.combine(otherTest);
		assertEquals("Expected 9 when combining 1001 (= 1 + 0 + 0 + 8).",9,otherResult);
	}
	
	@Test
	public void testScheme(){
		CosineHashFamily chf = new CosineHashFamily(4);
		           //1 2 4 8
		int[] test= {0,0,0,1};
		int result = chf.combine(test);
		assertEquals("Expected 8 when combining 0001 (= 0 + 0 + 0 + 8).",8,result);
		
                        //1 2 4 8
		int[] otherTest= {1,0,0,1};
		int otherResult = chf.combine(otherTest);
		assertEquals("Expected 9 when combining 1001 (= 1 + 0 + 0 + 8).",9,otherResult);
	}
	
	

}
