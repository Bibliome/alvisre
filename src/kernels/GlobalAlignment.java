package kernels;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import exceptions.UnexpectedDatasetFormat;
import representation.Candidate;
import representation.Path;

public class GlobalAlignment extends SimilarityFunction {
	private Double gapPenalty = 0.0;
	PositionScore posScore = null;


	

//	public GlobalAlignment(SimilarityFunctionConfiguration conf) throws IOException {
//		super(  new ArrayList<Candidate>());
//		posScore= new PositionScore(configuration);
//	}

	public GlobalAlignment(SimilarityFunctionConfiguration conf, List<PartSimilarityTable> tables) throws Exception {
		super(  conf, tables);
		posScore= new PositionScore(conf, tables);

	}

	public Double alignmentScore(Candidate c1, Candidate c2) { // sentence to
		return alignmentScore(c1, c2, false);
	}

	
	public Double alignmentScore(Candidate c1, Candidate c2, Boolean threadedMaps) { 
		if (c1 == null || c2 == null) {
			throw new UnexpectedDatasetFormat("Candidates are empty, impossible to calculate distance. Are you serious?");
		}
		if (c1.equals(c2)){
			return 1.0;
		}
		if ((c1 instanceof Path) && (c2 instanceof Path)) {
			Path p1 = (Path) c1;
			Path p2 = (Path) c2;
			Integer m = p1.getPath().size();
			Integer n = p2.getPath().size();
			Double[][] F;
			F = new Double[m + 1][n + 1];
			Double maxScore = 0.0;
			F[0][0] = 0.0;
			F[1][0] = -gapPenalty;
			F[0][1] = -gapPenalty;
			for (Integer i = 1; i <= m; i++) {
				F[i][0] = F[i - 1][0] - gapPenalty;
				if (maxScore < F[i][0]) {
					maxScore = F[i][0];
				}
			}
			for (Integer j = 1; j <= n; j++) {
				F[0][j] = F[0][j - 1] - gapPenalty;
				if (maxScore < F[0][j]) {
					maxScore = F[0][j];
				}
			}
			for (Integer i = 1; i <= m; i++) {
				for (Integer j = 1; j <= n; j++) {
					maxScore = F[i - 1][j] - gapPenalty;
					if (maxScore < F[i][j - 1] - gapPenalty) {
						maxScore = F[i][j - 1] - gapPenalty;
					}
					Double score = posScore.score(p1.getPath().get(i - 1), p1, p2.getPath().get(j - 1), p2, threadedMaps);
					if (score > 1.0){
						throw new UnexpectedDatasetFormat("this score is invalid");
					}
					if (maxScore < F[i - 1][j - 1] + score) {
						maxScore = F[i - 1][j - 1] + score;
					}
					F[i][j] = maxScore;
				}

			}
			Double result = (2 * F[m][n] / (m + n));
			if (result > 1.0) {
				throw new UnexpectedDatasetFormat("this score is invalid");
			}
			return result;
		}
		return null;

	}

	@Override
	public Double calculateScore(Candidate a, Candidate b) {
		return alignmentScore(a, b);
	}


	@Override
	public Double calculateScore(Candidate a, Candidate b, boolean c) {
		// TODO Auto-generated method stub
		return alignmentScore(a, b, c);
	}
	
	public ConcurrentHashMap<String, Double>  getDependencyMap () {
		return posScore.knowndep;
	}

	
}
