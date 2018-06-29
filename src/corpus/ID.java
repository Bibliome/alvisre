package corpus;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exceptions.UnexpectedDatasetFormat;


public class ID  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Integer id;
	public Type type;
	public enum Type implements Serializable {
		textbound, relation
	}


	public ID(Integer idNum, Type idType){
		if (idNum == null) {
			throw new UnexpectedDatasetFormat("Error ID20: There was no goddamn numeric part given for this freaking ID"); 
		}
		if (idType == null) {
			throw new UnexpectedDatasetFormat("Error ID20: There was no goddamn alpha part given for this freaking ID"); 
		}
		
		id = idNum;
		type = idType;
		
	}

	public ID(String mixID){
		this.setMixID(mixID);
	}

	public String getMixID(){
		String mixID = null;
		switch (type){
		case textbound:
			mixID = "T"+id.toString();
			break;
		case relation:
			mixID = "R"+id.toString();
			break;
		}
		return mixID;
	}

	public void setMixID(String mixID){
		if (mixID != null){

			if (mixID.length() > 0 && mixID.charAt(0) == 'T'){
				type = Type.textbound;
			}
			else if (mixID.length() > 0 && mixID.charAt(0) == 'E'){
				type = Type.relation;
			}
			else if (mixID.length() > 0 && mixID.charAt(0) == 'R'){
				type = Type.relation;
			}
			else if (mixID.length() > 0 && mixID.charAt(0) == 'W'){
				type = Type.textbound;
			}
			else {
				throw new UnexpectedDatasetFormat("Error ID70: Invalid mixID given. You fool!");
			}
			Pattern p = Pattern.compile("[WTRE]([0-9]+)");
			Matcher m = p.matcher(mixID);
			if (m.matches()){
				id = Integer.parseInt(m.group(1));	
			}
			else {
				throw new UnexpectedDatasetFormat("Error ID69: There was no number in that ID, jerk!");

			}
		}
		else {
			throw new UnexpectedDatasetFormat("Error ID71: Why am I given a null mixID, putain?");
		}
		
	}


}


