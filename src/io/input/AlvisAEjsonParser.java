package io.input;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import corpus.NEtype;
import corpus.RelationType;
import corpus.Schema;

public class AlvisAEjsonParser {


	public static Schema parseJSON(String filename) throws IOException{
		FileReader fr  = new FileReader(filename);
		JsonObject js = JsonObject.readFrom(fr);
//		HashMap<String, NEtype> netypes = new HashMap<String, NEtype>();
//		ArrayList<RelationType> reltypes = new ArrayList<RelationType>();
		List<String> relNames = js.names();
		Schema schema = new Schema();
		for (String name:relNames) {
			if (!name.equals("Condition")){
				JsonObject rel =  (JsonObject) js.get(name);
				JsonArray args = (JsonArray) rel.get("relationDef");
				JsonObject arg1 = (JsonObject) args.get(0);
				JsonObject arg2 = (JsonObject) args.get(1);
				String role1 = arg1.names().get(0);
				String role2 = arg2.names().get(0);
				JsonArray types1 = (JsonArray) arg1.get(role1);
				JsonArray types2 = (JsonArray) arg2.get(role2);
				ArrayList<NEtype> arg1types = new ArrayList<NEtype>();
				ArrayList<NEtype> arg2types = new ArrayList<NEtype>();
				for (JsonValue val:types1){
					String value = val.asString();
//					if (netypes.containsKey(value)) {
//						arg1types.add(netypes.get(value));
//					}
					if (schema.getNETypeByName(value) != null){
						arg1types.add(schema.getNETypeByName(value));
					}
					else {
						NEtype ne = new NEtype(value);
//						netypes.put(value, ne);
						schema.addType(ne);
						arg1types.add(ne);
					}
				}
				for (JsonValue val:types2){
					String value = val.asString();
					if (schema.getNETypeByName(value) != null){
						arg2types.add(schema.getNETypeByName(value));
					}
					else {
						NEtype ne = new NEtype(value);
						schema.addType(ne);
						arg2types.add(ne);
					}
				}
				RelationType reltype = new RelationType(name, role1, arg1types, role2, arg2types);
//				reltypes.add(reltype);
				schema.addRelationType(reltype);

			}
			else {
				System.out.println("Ignoring Condition");
			}
		}
		return schema;
	}


}
