package kernels;

import java.util.HashMap;

import exceptions.UnexpectedDatasetFormat;

public class ExtraFeatures {
	private HashMap<String, Boolean> booleanFeatures = new HashMap<String, Boolean>();
	private HashMap<String, Double> doubleFeatures= new HashMap<String, Double>();
	private HashMap<String, String> stringFeatures = new HashMap<String, String>();

	public ExtraFeatures() {
		// TODO Auto-generated constructor stub
	}

	public void setFeature(String key, Double value) {
		doubleFeatures.put(key, value);
	}
	
	public void setFeature(String key, Boolean value){
		booleanFeatures.put(key, value);
	}
	
	public void setFeature (String key, String value){
		stringFeatures.put(key, value);
	}
	
	
	public Boolean getBooleanFeature(String key) {
		if (booleanFeatures.containsKey(key)) {
			return booleanFeatures.get(key);
		}
		else {
			throw new UnexpectedDatasetFormat("Requested inexistant extra Boolean feature: "+key);
		}
	}
	
	public Double getDoubleFeature(String key) {
		if (doubleFeatures.containsKey(key)) {
			return doubleFeatures.get(key);
		}
		else {
			throw new UnexpectedDatasetFormat("Requested inexistant extra Double feature: "+key);
		}
	}
	
	public String getStringFeature(String key) {
		if (stringFeatures.containsKey(key)) {
			return stringFeatures.get(key);
		}
		else {
			throw new UnexpectedDatasetFormat("Requested inexistant extra String feature: "+key);
		}
	}

}
