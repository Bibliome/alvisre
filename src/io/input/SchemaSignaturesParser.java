package io.input;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corpus.NEtype;
import corpus.RelationType;
import corpus.Schema;
import exceptions.UnexpectedDatasetFormat;

public class SchemaSignaturesParser {

	public static Schema parseFile(String filename) throws IOException{
		Schema schema = new Schema();
		FileReader fr  = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		String line ="";
		Pattern pattern = Pattern.compile("([a-zA-Z_0-9]+)\\s*\\(\\s*\\[\\s*([a-zA-Z_0-9]+)\\s*\\]\\s*([a-zA-Z_0-9]+)\\s*,\\s*\\[\\s*([a-zA-Z_0-9]+)\\s*\\]\\s*([a-zA-Z_0-9]+)\\s*\\)");
		while ((line = br.readLine()) != null) {
			String relName = "";
			String role1 = "";
			String arg1 = "";
			String role2 = "";
			String arg2 = "";
			Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				relName = matcher.group(1);
				role1 = matcher.group(2);
				arg1 = matcher.group(3);
				role2 = matcher.group(4);
				arg2 = matcher.group(5);
			}
			System.out.println("Debug "+line);
			if (relName.equals("") && role1.equals("") && arg1.equals("") && role2.equals("") && arg2.equals("")){
				continue;
			}
			else if (role1.equals("") || arg1.equals("") || role2.equals("") || arg2.equals("")){
				br.close();
				fr.close();
				throw (new UnexpectedDatasetFormat("Something went wrong with the pattern: relname role1 arg1 role2 arg2: '"+relName+"' '"+role1+"' '"+arg1+"' '"+role2+"' '"+arg2));
			}
			else {
				RelationType relType = schema.getRelationTypeByName(relName);
				if (relType == null) {
					relType = new RelationType(relName);
					relType.setRoles(role1, role2);
					schema.addRelationType(relType);
				}
				NEtype type1 = schema.getNETypeByName(arg1);
				if (type1 == null) {
					type1 = new NEtype(arg1);
					schema.addType(type1);
				}
				NEtype type2 = schema.getNETypeByName(arg2);
				if (type2 == null) {
					type2 = new NEtype(arg2);
					schema.addType(type2);
				}
				relType.addSignature(type1, type2);
			}
		}
		br.close();
		fr.close();
		return schema;
	}

}
