package kernels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import semantictools.WordToSemClassTable;


public class SimilarityFunctionConfiguration {

	HashMap<String, String> stringConf = new HashMap<String, String>();
	HashMap<String, Double> doubleConf = new HashMap<String, Double>();
	HashMap<String, Boolean> booleanConf = new HashMap<String, Boolean>();
	List<PartSimilarityTable> partSimilarityTables = new ArrayList<PartSimilarityTable>();
	WordToSemClassTable wordToSemClassTable = new WordToSemClassTable();
	public TriggerWordTable triggerWordTable = new TriggerWordTable();
	
	
	public SimilarityFunctionConfiguration(HashMap<String, String> str, HashMap<String, Double> db, List<PartSimilarityTable> st, WordToSemClassTable wordToSemClassTable, TriggerWordTable triggerWordTable) {
		stringConf = str;
		doubleConf = db;
		partSimilarityTables = st;
		this.wordToSemClassTable = wordToSemClassTable;
		this.triggerWordTable = triggerWordTable;
		
	}
	public SimilarityFunctionConfiguration(HashMap<String, String> str, HashMap<String, Double> db, List<PartSimilarityTable> st, WordToSemClassTable wordToSemClassTable) {
		this(str,db,st,wordToSemClassTable,null);
	}
	public SimilarityFunctionConfiguration(HashMap<String, String> str, HashMap<String, Double> db, List<PartSimilarityTable> st, TriggerWordTable triggerWordTable) {
		this(str,db,st,null,triggerWordTable);
	}
	public SimilarityFunctionConfiguration(HashMap<String, String> str, HashMap<String, Double> db, List<PartSimilarityTable> st) {
		this(str,db,st,null, null);
	}
	public SimilarityFunctionConfiguration(){
		
	}
	
	public Double getDoubleValue(String key) {
		return doubleConf.get(key);
	}
	
	public String getStringValue(String key){
		return stringConf.get(key);
	}
	
	public Boolean getBooleanValue(String key){
		return booleanConf.get(key);
	}
	
	public void SetConfiguration(HashMap<String, String> str, HashMap<String, Double> db,  HashMap<String, Boolean> bl, List<PartSimilarityTable> st) {
		stringConf = str;
		doubleConf = db;
		booleanConf = bl;
		partSimilarityTables = st;
	}
	public void SetConfiguration(HashMap<String, String> stringConf2,
			HashMap<String, Double> doubleConf2,
			HashMap<String, Boolean> booleanConf2,
			List<PartSimilarityTable> simTables, WordToSemClassTable semanticTable2, TriggerWordTable triggerWordTable2) {
		stringConf = stringConf2;
		doubleConf = doubleConf2;
		booleanConf = booleanConf2;
		partSimilarityTables = simTables;
		wordToSemClassTable = semanticTable2;
		triggerWordTable = triggerWordTable2;

	}


}
