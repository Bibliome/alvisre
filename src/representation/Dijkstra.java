package representation;

/*************************************************************************
 *  Dijkstra's algorithm.
 *
 *************************************************************************/

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import corpus.ID;
import corpus.SyntacticRelation;
import corpus.SyntacticRelationArgument;

public class Dijkstra {
	protected static double INFINITY = 150000000;
	protected static double EPSILON = 0.000001;

	// protected EuclideanGraph G;
	protected HashMap<SyntacticRelationArgument, Double> distance = new HashMap<SyntacticRelationArgument, Double>();
	protected HashMap<SyntacticRelationArgument, SyntacticRelationArgument> previous = new HashMap<SyntacticRelationArgument, SyntacticRelationArgument>();
	protected HashMap<SyntacticRelationArgument, ArrayList<SyntacticRelationArgument>> multiprevious = new HashMap<>();
	//	protected ArrayList<HashMap<SyntacticRelationArgument, Double>> multidistance = new ArrayList<HashMap<SyntacticRelationArgument, Double>>();
	protected Graph G;
	protected List<QElement> qelements = new ArrayList<QElement>();

	public Dijkstra(Graph G) {
		this.G = G;
	}

	protected static Comparator<QElement> comp = new Comparator<QElement>() {

		@Override
		public int compare(QElement o1, QElement o2) {
			if (o1.distance < o2.distance)
				return -1;
			if (o1.distance.equals(o2.distance))
				return 0;
			return 1;
		}
	};

	public List<ID> getPath(SyntacticRelationArgument start, SyntacticRelationArgument end, Double wordpathCost) {
		//self loop finally?
		if (start.equals(end)){
			List<ID> result = new ArrayList<ID>();
			if (start.argw != null) {
				result.add(start.argw.wid);
			}
			if (start.argt != null) {
				result.add(start.argt.tid);
			}
			return result;
		}
		//self loop end
		
		dijkstra(start, end, wordpathCost);
		List<ID> result = new ArrayList<ID>();
		if (previous.get(end) == null) {
			System.out.println("Houston, we have a problem:11");
			return result;

		}
		if (previous.get(end) == end)  {
 			return result;
		}

		SyntacticRelationArgument current = end;
		while (true) {
			if (current == start) {
				/* we have reached the end (start) of the path
				 * no need to add more words or relations. 
				 */
				if (current.argw != null) {
					result.add(current.argw.wid);
				}
				if (current.argt != null) {
					result.add(current.argt.tid);
				}	
				Collections.reverse(result);
				return result;
			}
			SyntacticRelationArgument prev = previous.get(current);
			if (prev == current)  {
				return result;
			}
			if (current.argw != null) {
				result.add(current.argw.wid);
			}
			if (current.argt != null) {
				result.add(current.argt.tid);
			}
			List<SyntacticRelation> relations = G.getSyntacticRelationsHaving(current);
			Boolean found = false;
			for (SyntacticRelation relation:relations){
				if (relation.arg1.equals(prev) || relation.arg2.equals(prev)) {
					//					if (found == true){
					//						System.out.print("Duplicate syntactic dependency in "+this.G.s.id+": R"+relation.rid.id+". Possible originals");
					//						for (ID id:result){
					//							System.out.print(" R"+id.id);
					//						}
					//						System.out.println(" (while examining T"+current.argw.wid.id+" and T"+prev.argw.wid.id+")");
					//					}
					if (!found) {
						result.add(relation.rid);
						found = true;
					}

				}
			}
			if (found) current = prev;
			else {
				return null;
			}
		}
	}
//	public ArrayList<List<ID>> getPathPlus(SyntacticRelationArgument start, SyntacticRelationArgument end) {
//		return null;
//	}	
	
	
	

	// Dijkstra's algorithm to find shortest path from s to d
	protected void dijkstraPlus(SyntacticRelationArgument start, SyntacticRelationArgument end, Double wordpathCost){
		int V = G.V();
		// initialize
		for (SyntacticRelationArgument arg : G.getNodes()) {
			distance.put(arg, INFINITY);
			ArrayList<SyntacticRelationArgument> values = multiprevious.get(arg);
			if (values.isEmpty()) values = new ArrayList<>();
			values.add(arg);
			multiprevious.put(arg,values);
		}
		// priority queue
		PriorityQueue<QElement> pq = new PriorityQueue<QElement>(V, comp);
		for (SyntacticRelationArgument arg : G.getNodes()) {
			QElement tmp = new QElement(arg, distance.get(arg));
			pq.offer(tmp);
			qelements.add(tmp);
		}

		// set distance of source
		distance.put(start, 0.0);
		QElement tmp = new QElement(start, distance.get(start));
		pq.offer(tmp);
		qelements.add(tmp);

		// run Dijkstra's algorithm
		while (!pq.isEmpty()) {
			QElement top = pq.poll();
			SyntacticRelationArgument topnode = top.arg;

			// not reachable from start so stop
			if (distance.get(topnode) == INFINITY)
				break;

			// scan through all nodes neighbour adjacent to topnode
			for (SyntacticRelationArgument neighbour : G.getNeighbours(topnode)) {
				boolean exists = false;
				for (QElement q : qelements) {
					if (q.arg.equals(neighbour) && pq.contains(q)) {
						exists = true;
					}
				}
				if (exists) {
					if (distance.get(topnode) + G.distance(topnode, neighbour.argt.asSyntacticRelationArgument(), wordpathCost ) < distance.get(neighbour) - EPSILON) {
						distance.remove(neighbour);
						distance.put(neighbour, distance.get(topnode) + G.distance(topnode, neighbour, wordpathCost));
						pq.offer(new QElement(neighbour, distance.get(neighbour)));
						ArrayList<SyntacticRelationArgument> values = multiprevious.get(neighbour);
						if (values.isEmpty()) values = new ArrayList<>();
						values.add(topnode);
						multiprevious.put(neighbour,values);
					}
				}
			}

		}
	}
	protected void dijkstra(SyntacticRelationArgument start, SyntacticRelationArgument end, Double wordpathCost) {
		int V = G.V();
		// initialize
		for (SyntacticRelationArgument arg : G.getNodes()) {
			distance.put(arg, INFINITY);
			previous.put(arg, arg);
		}
		// priority queue
		PriorityQueue<QElement> pq = new PriorityQueue<QElement>(V, comp);
		for (SyntacticRelationArgument arg : G.getNodes()) {
			QElement tmp = new QElement(arg, distance.get(arg));
			pq.offer(tmp);
			qelements.add(tmp);
		}

		// set distance of source
		distance.put(start, 0.0);
		QElement tmp = new QElement(start, distance.get(start));
		pq.offer(tmp);
		qelements.add(tmp);

		// run Dijkstra's algorithm
		while (!pq.isEmpty()) {
			QElement top = pq.poll();
			SyntacticRelationArgument topnode = top.arg;

			// not reachable from start so stop
			if (distance.get(topnode) == INFINITY)
				break;

			// scan through all nodes neighbour adjacent to topnode
			for (SyntacticRelationArgument neighbour : G.getNeighbours(topnode)) {
				boolean exists = false;
				for (QElement q : qelements) {
					if (q.arg.equals(neighbour) && pq.contains(q)) {
						exists = true;
					}
				}
				if (exists) {
					if (distance.get(topnode) + G.distance(topnode, neighbour, wordpathCost) < distance.get(neighbour) - EPSILON) {
						distance.remove(neighbour);
						distance.put(neighbour, distance.get(topnode) + G.distance(topnode, neighbour, wordpathCost));
						pq.offer(new QElement(neighbour, distance.get(neighbour)));
						previous.put(neighbour, topnode);
					}
				}
			}

		}
	}
}
