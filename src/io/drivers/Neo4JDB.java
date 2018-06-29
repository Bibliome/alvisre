package io.drivers;

public class Neo4JDB {
//	GraphDatabaseService graphDB = null;
//
//
//	public Neo4JDB(String path) {
//		System.out.println("This is broken");
//		graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(path);
//		registerShutdownHook( graphDB );
//		graphDB.schema().indexFor( DynamicLabel.label( "Candidate" ) ).on( "ID" ).create();
//		graphDB.schema().constraintFor(DynamicLabel.label("Candidate")).assertPropertyIsUnique("ID");
//
//	}
//
//
//	private static void registerShutdownHook( final GraphDatabaseService graphDB )
//	{
//		// Registers a shutdown hook for the Neo4j instance so that it
//		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
//		// running application).
//		Runtime.getRuntime().addShutdownHook( new Thread()
//		{
//			@Override
//			public void run()
//			{
//				graphDB.shutdown();
//			}
//		} );
//	}
//	private static enum RelTypes implements RelationshipType
//	{
//		SIMILARITY, EUCLIDEAN;
//
//	}
//
//
//	/**
//	 * Creates a node in the Neo4J graph db for a candidate
//	 * @param candidate the candidate
//	 * @param labelNature labeled/unlabeled/prediction
//	 * @return
//	 */
//	public Node createNeo4JNode(Candidate candidate, String labelNature){
//		if ((!labelNature.equals("labeled")) && (!labelNature.equals("unlabeled")) && (!labelNature.equals("prediction"))){
//			throw (new UnexpectedDatasetFormat("label possibilities are labeled, unlabeled and prediction"));
//		}
//		Node node = null;
//		Transaction tx = graphDB.beginTx();
//		try {
//			Label label = DynamicLabel.label("Candidate");
//			node = graphDB.createNode(label);
//			node.setProperty("ID", candidate.index);
//			node.setProperty("documendID", candidate.document.id);
//			node.setProperty("sentenceID", candidate.sentence.id);
//			node.setProperty("candidateRelationType", candidate.candidateRelationType.type);
//			node.setProperty("arg1", candidate.arg1.argument.tid.getMixID());
//			node.setProperty("arg2", candidate.arg2.argument.tid.getMixID());
//			node.setProperty("origin", labelNature);
//			if (labelNature.equals("unlabeled")) {
//				node.setProperty("relation", "N/A");
//			}
//			else {
//				node.setProperty("relation", candidate.relation.type.type);
//			}
//			tx.success();
//		}
//		finally {
//			tx.close();
//		}
//
//		return node;
//	}
//
//	public void addSimilarity(Node node1, Node node2, Double value){
//		Relationship rel = null;
//		Transaction tx = graphDB.beginTx();
//		try {
//			rel = node1.createRelationshipTo(node2, RelTypes.SIMILARITY);
//			rel.setProperty("similarity", value);
//			tx.success();
//		}
//		finally {
//			tx.close();
//		}
//	}
//
//	public void addEuclidean(Node node1, Node node2, Double value){
//		Relationship rel = null;
//		Transaction tx = graphDB.beginTx();
//		try {
//			rel = node1.createRelationshipTo(node2, RelTypes.EUCLIDEAN);
//			rel.setProperty("distance", value);
//			tx.success();
//		}
//		finally {
//			tx.close();
//		}
//	}
//
//
//	public void addCandidateSimilarity(Candidate cand1, String cand1origin, Candidate cand2, String cand2origin, Double similarity){
//		Node node1 = createNeo4JNode(cand1, cand1origin);
//		Node node2 = createNeo4JNode(cand2, cand2origin);
//		if (node1 != null && node2!=null) {
//			addSimilarity(node1, node2, similarity);
//		}
//		else {
//			throw new UnexpectedDatasetFormat("Null nodes");
//		}
//	}
//
//	public void addCandidateEuclidean(Candidate cand1, String cand1origin, Candidate cand2, String cand2origin, Double distance){
//		Node node1 = createNeo4JNode(cand1, cand1origin);
//		Node node2 = createNeo4JNode(cand2, cand2origin);
//		if (node1 != null && node2!=null) {
//			addEuclidean(node1, node2, distance);
//		}
//		else {
//			throw new UnexpectedDatasetFormat("Null nodes");
//		}
//	}


}
