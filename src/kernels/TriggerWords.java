package kernels;

import java.util.ArrayList;

public class TriggerWords extends ArrayList<String> {
	protected ArrayList<String> classWords = new ArrayList<>();
	private static final long serialVersionUID = 1L;
	
	public Boolean containsStem(String word){
		for (String stem:this) {
			if (word.startsWith(stem)) return true;
		}
		return false;
		
	}



}
