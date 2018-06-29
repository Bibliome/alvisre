package io.input;

import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import corpus.RelationSignature;
import corpus.RelationType;
import corpus.Schema;

public class SchemaSignaturesParserTest {

	@Test
	public void test() throws IOException {
		String filename = "data/arabido/signatures.txt";
		Schema sch = SchemaSignaturesParser.parseFile(filename);
		FileReader fr  = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		Integer lineCount = countLines(filename);
		String line = "";
		Integer rand = 1 + (int)(Math.random()*lineCount);
		for (int i = 0; i < rand; i++) {
			line = br.readLine();
		}
		System.out.println("Just read random line from file: '"+line+"'");
		System.out.println("Parsed by test unit as:");
		br.close();
		fr.close();
		String relName = "";
		String role1 = "";
		String arg1 = "";
		String role2 = "";
		String arg2 = "";
		Pattern pattern = Pattern.compile("([a-zA-Z_]+)\\s*\\(\\s*\\[\\s*([a-zA-Z_]+)\\s*\\]\\s*([a-zA-Z_]+)\\s*,\\s*\\[\\s*([a-zA-Z_]+)\\s*\\]\\s*([a-zA-Z_]+)\\s*\\)");
		Matcher matcher = pattern.matcher(line);
		while (matcher.find()) {
			relName = matcher.group(1);
			role1 = matcher.group(2);
			arg1 = matcher.group(3);
			role2 = matcher.group(4);
			arg2 = matcher.group(5);
			System.out.print("RelName: " + relName);
			System.out.print(" Role1: " + role1);
			System.out.print(" Arg1: " + arg1);
			System.out.print(" Role2: " + role2);
			System.out.println(" Arg2: " + arg2);
		}
		System.out.println("Checking to see if the schema reader got it right");
		RelationType relType = sch.getRelationTypeByName(relName);
		Boolean found = false;

		for (RelationSignature sig: relType.relationSignatures){
			if (sig.arg1.role.equals(role1) && sig.arg1.arg.getName().equals(arg1) && sig.arg2.role.equals(role2) && sig.arg2.arg.getName().equals(arg2)){
				found = true;
				System.out.println("Found!");
			}
		}
		assertTrue(found);
	}

	static int countLines(String filename) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}
}
