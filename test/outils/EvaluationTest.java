package outils;

import java.io.IOException;

import org.junit.Test;

import tools.Evaluation;

public class EvaluationTest {

	@Test
	public void testEvaluateBBStringStringStringStringString() {
		try {
			Evaluation.evaluateBB("/home/dvalsamou/workspace/re-kernels/data/biotopes/BB.py", "/bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/io.output/dev/", "/home/dvalsamou/workspace/re-kernels/data/biotopes/official/dev/","/bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/io.output/out-dev/pred/", "/bibdev/travail/biotope_work/bionlp2013_task2/ccg/anaphora/io.output/out-dev/pred/ref/", false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
