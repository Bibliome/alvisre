package experiments;

import java.util.ArrayList;
import java.util.HashMap;

import exceptions.UnexpectedDatasetFormat;
import kernels.DummyKernel;
import kernels.EmpiricalKernelMap;
import kernels.SimilarityFunction;
import representation.Candidate;
import representation.Path;
import tools.ProgressBar;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WHThread implements Runnable {
	ArrayList<Candidate> cands = new ArrayList<Candidate>();
	EmpiricalKernelMap EKM = null;
	ArrayList<Attribute> fvWekaAttributes = new ArrayList<Attribute>();
	Integer numOfTrainingInstances = 0;
	HashMap<String, String> classes = new HashMap<String, String>();
	Instances subSet = null;
	ProgressBar bar = null;
	SimilarityFunction kernel = null;
	Boolean useTriggerWords = false;
	Double verbose = 1.0;
	int threadNo = 0;

	public WHThread(int threadNo, Double verbose, ArrayList<Candidate> cands, SimilarityFunction kernel,
			EmpiricalKernelMap EKM, ArrayList<Attribute> fvWekaAttributes, Integer numOfTrainingInstances,
			Instances subSet, HashMap<String, String> Classes, ProgressBar bar, Boolean useTriggerWords) {
		this.EKM = EKM;
		this.fvWekaAttributes = fvWekaAttributes;
		this.subSet = subSet;
		this.numOfTrainingInstances = numOfTrainingInstances;
		this.classes = Classes;
		this.cands = cands;
		this.bar = bar;
		this.kernel = kernel;
		this.useTriggerWords = useTriggerWords;
		this.verbose = verbose;
		this.threadNo = threadNo;

	}

	@Override
	public void run() {
		long threadstart = java.lang.System.currentTimeMillis();

		Integer classCol = fvWekaAttributes.size() - 1;
		int candNo = 0;
		if (kernel instanceof DummyKernel) {
			for (Candidate candidate : cands) {
				long start = java.lang.System.currentTimeMillis();
				// String id = candidate.sentence.id;
				// Path path = (Path) candidate;
				// String thisText = candidate.sentence.text;
				Instance iExample = new DenseInstance(fvWekaAttributes.size());
				iExample.setValue(fvWekaAttributes.get(0), candidate.index);
				iExample.setValue(fvWekaAttributes.get(1), candidate.candidateRelationType.type);
				if (useTriggerWords) {
					setTriggerWords(new Integer(1), classCol, candidate, iExample);
				}
				setClassValueAndMoveOn(candNo++, start, classCol, candidate, iExample);
			}
		} else {
			for (Candidate candidate : cands) {
				long start = java.lang.System.currentTimeMillis();
				ArrayList<Double> thisVector = EKM.getVector(candidate);
				Instance iExample = new DenseInstance(fvWekaAttributes.size());
				iExample.setValue(fvWekaAttributes.get(0), candidate.index);
				for (int i = 0; i < numOfTrainingInstances; i++) {
					iExample.setValue(fvWekaAttributes.get(i + 1), thisVector.get(i));
				}
				if (useTriggerWords) {
					setTriggerWords(numOfTrainingInstances, classCol, candidate, iExample);
				}
				setClassValueAndMoveOn(candNo++, start, classCol, candidate, iExample);
			}
		}
		if (verbose > 5) {
			long threadend = java.lang.System.currentTimeMillis();
			for (int i = 0; i < threadNo; i++) {
				System.out.print("\t");
			}
			System.out.print("Thread" + threadNo + ": Finished " + cands.size() + ": ");
			double diff = threadend - threadstart;
			double average = diff / cands.size();
			if (diff > 1000) {
				diff /= 1000;
				System.out.print(diff + "s");
			} else {
				System.out.print(diff + "ms");
			}
			if (average > 1000) {
				average /= 1000;
				System.out.println(" " + average + "s on average");
			} else {
				System.out.println(" " + average + "ms on average");
			}

		}

	}

	/**
	 * Set the values to the TW attributes.
	 * 
	 * @param firstAttributeIdx
	 *            The index of the first "TW" attribute
	 * @param nextAttributeIdx
	 *            The index of the attribute that comes afterwards (generally
	 *            the Class Column)
	 * @param candidate
	 * @param iExample
	 */
	private void setTriggerWords(Integer firstAttributeIdx, Integer nextAttributeIdx, Candidate candidate,
			Instance iExample) {
		for (int i = firstAttributeIdx; i < nextAttributeIdx; i++) {
			String reltype = fvWekaAttributes.get(i).name();
			Path path = null;
			if (candidate instanceof Path) {
				path = (Path) candidate;
			} else {
				throw (new UnexpectedDatasetFormat("this should be a path!"));
			}
			// Double val =
			// kernel.configuration.triggerWordTable.containsTriggerWord(reltype,
			// path.getWordsOnPath())? 1.0 : 0.0;
			Double val = kernel.configuration.triggerWordTable.numOfTriggerWordOccurrences(reltype,
					path.getWordsOnPath());
			iExample.setValue(fvWekaAttributes.get(i), val);
		}
	}

	private void setClassValueAndMoveOn(int candNo, long start, Integer classCol, Candidate candidate,
			Instance iExample) {
		iExample.setValue(fvWekaAttributes.get(classCol), classes.get(candidate.index));
		subSet.add(iExample);
		long end = java.lang.System.currentTimeMillis();
		if (verbose > 6) {
			for (int i = 0; i < threadNo; i++) {
				System.out.print("\t");
			}
			System.out.print("Thread" + threadNo + ": " + candNo + "/" + cands.size() + ": ");
			double diff = end - start;
			if (diff > 1000) {
				diff /= 1000;
				System.out.print(diff + "s");
			} else {
				System.out.print(diff + "ms");
			}
			System.out.println("(l:" + candidate.getLength() + ")");
		} else {
			bar.updatePlusPlus();
		}
	}

	public Instances getValue() {
		return subSet;
	}

}
