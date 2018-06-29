package io.input;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import corpus.Schema;

public class TestAlvisAEjsonParser {

	@Test
	public void test() throws IOException {
		Schema schema = AlvisAEjsonParser.parseJSON("data/arabido/schema_arabido.json");
		assertNotNull(schema.getNETypeByName("Gene"));
		assertNotNull(schema.getRelationTypeByName("Comparison"));
		
	}

}
