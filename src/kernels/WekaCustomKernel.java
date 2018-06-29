package kernels;

import exceptions.UnexpectedDatasetFormat;
import weka.classifiers.functions.supportVector.Kernel;
import weka.core.Instance;

public class WekaCustomKernel extends Kernel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7335228015648023943L;

	public WekaCustomKernel() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String globalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double eval(int id1, int id2, Instance inst1) throws Exception {
		Instance inst2 = null;
		if (id1 != -1){
			inst1 = m_data.get(id1);
		}
		else {
			if (inst1 == null){
				throw (new UnexpectedDatasetFormat("Problem with instance 1!"));
			}
		}
		if (id2 < 0){
			throw (new UnexpectedDatasetFormat("Problem with instance 2!"));
		}
		else {
			inst2 = m_data.get(id2);	
		}
		if (inst1.attribute(0).name()!="id") {
			throw (new UnexpectedDatasetFormat("First Attribute should be 'id'"));
		}
		
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clean() {
		// TODO Auto-generated method stub

	}

	@Override
	public int numEvals() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int numCacheHits() {
		// TODO Auto-generated method stub
		return 0;
	}

}
