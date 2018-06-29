package experiments;

import java.util.HashMap;

public class Statistics {
	HashMap<String, String> stringStats = new HashMap<String, String>();
	HashMap<String, Double> doubleStats = new HashMap<String, Double>();
	HashMap<String, Integer> integerStats = new HashMap<String, Integer>();

	public Statistics() {
		stringStats = new HashMap<String, String>();
		 doubleStats = new HashMap<String, Double>();
	}
	
	public void addStat(String name, String value){
		stringStats.put(name, value);
	}
	
	public void addStat(String name, Double value){
		doubleStats.put(name, value);
	}
	
	public void addStat(String name, Integer value){
		integerStats.put(name, value);
	}
	
	public Double getDoubleStat(String name){
		return doubleStats.get(name);
	}
	
	public String getStringStat(String name){
		return stringStats.get(name);
	}
	
	public Integer getIntegerstat(String name){
		return integerStats.get(name);
	}
	
	
	public HashMap<String, String>  getStringStats (){
		return stringStats;
	}

	public HashMap<String, Double>  getDoubleStats (){
		return doubleStats;
	}
	
	public HashMap<String, Integer>  getIntegerStats (){
		return integerStats;
	}
}
