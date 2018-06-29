package io.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import corpus.NEtype;
import corpus.RelationType;
import corpus.Schema;

public class SeeDevSchemaSignaturesParser extends SchemaSignaturesParser {

	public static Schema parseDirectory(String dirname) throws IOException{
		Schema schema = new Schema();
		File dir = new File(dirname);
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".csv");
			}
		});
		for (File file: files) {
			parseFile(file.getAbsolutePath(), schema);
		}
		return schema;
	}

	public static void parseFile(String filename, Schema schema) throws IOException{
		FileReader fr  = new FileReader(filename);
		System.out.println("Parsing file "+filename);
		BufferedReader br = new BufferedReader(fr);
		String line ="";
		String relName = "";
		ArrayList<String> lines = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			lines.add(line);
		}

		/*
		 * if lines 5,6,7 are short it means there's that stupid carriage return in there, so we should merge them
		 */

		ArrayList<String> newlines = new ArrayList<String>();
		String newline = "";
		ArrayList<String> testArray = new ArrayList<String>(Arrays.asList(lines.get(4).split(",")));
		if (testArray.size() < 10) {
			newline = lines.get(4)+lines.get(5)+lines.get(6);
			newlines.addAll(lines.subList(0, 4));
			newlines.add(newline);
			newlines.addAll(lines.subList(7, lines.size()));
//			System.out.println("Debug reparation 5th before: "+lines.get(4));
			lines = newlines;
//			System.out.println("Debug reparation 5th after: "+lines.get(4));
		}
		/* 4th and 5th are "special lines" 
		 * Fourth line contains binary name and argument roles
		 * Fifth line contains argument names
		 * 6th - end are signatures*/
		String fourth = lines.get(3);
		String fifth = lines.get(4);
		ArrayList<String> array4 = new ArrayList<String>(Arrays.asList(fourth.split(",", -1)));
		ArrayList<String> array5 = new ArrayList<String>(Arrays.asList(fifth.split(",", -1)));
		relName = array4.get(0);
		while (array4.size() < 19) {
			array4.add("");
		}
		while (array5.size() < 19) {
			array5.add("");
		}
		ArrayList<String>  rolesRow = new ArrayList<String>(array4.subList(3, 19));
		ArrayList<String> argNameRow = new ArrayList<String>(array5.subList(3, 19));
		for (String s:argNameRow) {
			NEtype ne = schema.getNETypeByName(s);
			if (ne == null) {
				ne = new NEtype(s);
				schema.addType(ne);
			}
		}
		for (int i = 5; i < lines.size(); i++){
			String thisLine = lines.get(i);
			ArrayList<String> array = new ArrayList<String>(Arrays.asList(thisLine.split(",", -1)));
			String role1= array.get(0);
			role1 = role1.replace(" ", "");
			String arg1 = array.get(2);
			String role2 = "";
			String arg2 = "";
			for (int v = 3; v < 19; v++){
				role2 = rolesRow.get(v-3);
				role2 = role2.replace(" ", "");
				arg2 = argNameRow.get(v-3);
				Boolean isTrue = array.get(v).length() > 0;
				if (isTrue) {
//					System.out.println("Debug "+line);
					if (role1.equals("") || arg1.equals("") || role2.equals("") || arg2.equals("")){
						//					br.close();
						//					fr.close();
						System.out.println("Debug: impossible relation (2): '"+relName+"' '"+arg1+"' '"+arg2);
						//					throw (new UnexpectedDatasetFormat("Something went wrong with reading the file "+filename+": relname role1 arg1 role2 arg2: '"+relName+"' '"+role1+"' '"+arg1+"' '"+role2+"' '"+arg2));
					}
					else {
						RelationType relType = schema.getRelationTypeByName(relName);
						if (relType == null) {
							relType = new RelationType(relName);
							relType.setRoles(role1, role2);
							schema.addRelationType(relType);
						}
						NEtype type1 = schema.getNETypeByName(arg1);
						NEtype type2 = schema.getNETypeByName(arg2);
						
						relType.addSignature(role1, type1, role2,type2);
					}
				}
				else {
//					System.out.println("Debug: impossible relation: '"+relName+"' '"+arg1+"' '"+arg2);
				}
			}
		}
		br.close();
		fr.close();
	}

}
