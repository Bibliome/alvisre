package representation;

import corpus.SyntacticRelationArgument;
import exceptions.UnexpectedDatasetFormat;

public class QElement {
	SyntacticRelationArgument arg;
	Double distance;
	public QElement(SyntacticRelationArgument a, Double d){
		if (a == null){
			throw new UnexpectedDatasetFormat("Q11:Why is this argument null?");
		}
		arg =a;
		distance = d;
	}
	
}
