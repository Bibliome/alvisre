package kernels;

import java.util.ArrayList;
import java.util.Collections;

import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

public class DummyClassifier extends AbstractClassifier {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Double predictedClassIndex = 0.0; 

	public DummyClassifier() {
	}

	@Override
	public void buildClassifier(Instances data) throws Exception {
		// TODO Auto-generated method stub

	}

	public void setOptions(String[] options) throws Exception {
		predictedClassIndex =  Double.parseDouble(Utils.getOption('c', options));

	}

	public double classifyInstance(Instance instance) throws Exception {
		if (instance.classIndex() == 0) 	{
			return predictedClassIndex.doubleValue();
		}
		else {
			int noneidx = instance.numClasses()-1;
			boolean set = false;
			int candRelTypeIdx =  0;
			Attribute candRelAttribute = instance.attribute(candRelTypeIdx);
			double candRel = instance.value(candRelAttribute);
			ArrayList<String> candRelTypes = Collections.list(candRelAttribute.enumerateValues());
			String candRelType = candRelTypes.get((int)candRel);
			instance.setClassValue(candRelType); //Co-occurrence is there
			/* the code below handles trigger word activation
			 * 
			 */
			for (int i = 1; i < instance.classIndex(); i++){
				if (instance.value(i) >=1.0) {
					String name = instance.attribute(i).name();
					if (!set && name.equals(candRelType)) {
						instance.setClassValue(name);
						set = true;
					}
				}
				else {
					instance.setClassValue(noneidx);
				}

			}
 			return instance.classValue();
		}

	}
}
