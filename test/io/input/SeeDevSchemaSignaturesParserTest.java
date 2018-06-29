package io.input;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import corpus.RelationSignature;
import corpus.RelationType;
import corpus.Schema;
import tools.PrintStuff;

public class SeeDevSchemaSignaturesParserTest {

	@Test
	public void testParseDirectory() throws IOException {
		String dirname = "/home/dvalsamou/workspace/re-kernels/data/SeeDev/Signatures/";
		Schema schema = SeeDevSchemaSignaturesParser.parseDirectory(dirname);
		String relName = "Binds_To";
		String role1 = "Functional_Molecule";
		String role2 = "Molecule";
		String arg1 = "RNA";
		String arg2 = "Gene";
		RelationType relType = schema.getRelationTypeByName(relName);
		Boolean found = false;
		System.out.println("----");
		PrintStuff.printSchema(schema, new ArrayList<String>());
		System.out.println("---");
		for (RelationSignature sig: relType.relationSignatures){
			if (sig.arg1.role.equals(role1) && sig.arg1.arg.getName().equals(arg1) && sig.arg2.role.equals(role2) && sig.arg2.arg.getName().equals(arg2)){
				found = true;
				System.out.println("Found!");
			}
		}
		assertTrue(found);
	}

	@Test
	public void testParseFileStringSchema() throws IOException {
		String filename = "/home/dvalsamou/workspace/re-kernels/data/SeeDev/Signatures/BIONLP-ST 2016 - SeeDev Task - signature of relation arguments - Binding.csv";
		Schema schema = new Schema();
		String relName = "Binds_To";
		String role1 = "Functional_Molecule";
		String role2 = "Molecule";
		String arg1 = "RNA";
		String arg2 = "Gene";
		SeeDevSchemaSignaturesParser.parseFile(filename, schema);
		RelationType relType = schema.getRelationTypeByName(relName);
		Boolean found = false;

		for (RelationSignature sig: relType.relationSignatures){
			if (sig.arg1.role.equals(role1) && sig.arg1.arg.getName().equals(arg1) && sig.arg2.role.equals(role2) && sig.arg2.arg.getName().equals(arg2)){
				found = true;
				System.out.println("Found!");
			}
		}
		assertTrue(found);

	}

}
