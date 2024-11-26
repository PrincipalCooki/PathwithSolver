import edu.kit.iti.formal.pathwidth.*;

import java.util.Iterator;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;      
import java.util.stream.IntStream;  

// Alle knoten haben ein intervall: 
// 

public class MyPathwidthSolver extends PathwidthSolver {
    private Graph g;
    private int n;

    public MyPathwidthSolver(Graph g){
        super(g);
        this.g = g;
        this.n = g.getNumNodes();
    }

    @Override
    public Solution solve() {
        // TODO: Implement solution
        solution.setState(SolutionState.UNKNOWN);
        System.out.println(this.g.getNumNodes());
        if(g.getNumNodes() != 5) {
            return solution;
        }

        System.out.println(solution.getInterval(0)[0]);

        this.g.getEdgeIterator();
        
        solution.print(System.out);

        buildIntervals();
        buildSATInstance();
        solveSATInstance();

        return solution;
    }
    private void buildLiterals() {
        int[][] lower = new int[n][n]; //lower[v][i] is: L(v) = i
        int[][] upper = new int[n][n];
        int[][] member = new int[n][n]; //member[v][x] is: x element [L(v), U(v)]
        
        for (int node = 0; node < n; node++) {
            lower[node] = range(0, n);
            upper[node] = range(n,2*n);
            member[node] = range(2*n, 3*n);
        }
    }

    // Clause for every node, that L(v) <= U(v) holds
    // representation: -L || U 
    private void clauseConsistency() {

        for(int node = 0; node <= n; node++) {
            for(int lower = 0; lower <=n, lower++) {
                for(int upper = lower; upper <= n, upper++) {
                    int literalL = 
                }
            }
        }
    }

    // e = uv:  L(v)<=U(u) && L(u) <= L(v)
    // 
    private void clauseMembership() {

    }

    // e = uv:  L(v)<=U(u) && L(u) <= L(v)
    //example representation:  1<=2 && 5<=8 is the literal 1258
    private void clauseNeighbours() {

        Iterator<GraphEdge> edgeIterator = g.getEdgeIterator();
        while (edgeIterator.hasNext()) {
            GraphEdge edge = edgeIterator.next();

        }
    }

    private void buildIntervals() {
        Iterator<GraphEdge> edgeIterator = g.getEdgeIterator();
        float min = 1;
        float max = 2;
        while (edgeIterator.hasNext()) {
            GraphEdge edge = edgeIterator.next();
            int n0 = edge.getEndpoint1();
            int n1 = edge.getEndpoint2();
            
            float n0_left = solution.getInterval(n0)[0];
            float n0_right = solution.getInterval(n0)[1];

            float n1_left = solution.getInterval(n1)[0];
            float n1_right = solution.getInterval(n1)[1];

            n1_left = (n1_left == 0) ? n0_right : n1_left;

            solution.setInterval(n0, n0_left, n0_right + 1);
            solution.setInterval(n1, n1_left, n0_right + 1);

        }
    }

    // literals: positive inside interval, negativ outside interval
    private void buildSATInstance() {
        
        for (int interval_index = 0; interval_index < g.getNumNodes(); interval_index++) {
            float[] interval = solution.getInterval(interval_index);
            
            int left = Math.round(interval[0]);
            int right = Math.round(interval[1]);

            int[] negativs = addAll(
                scalarMult(-1, range(0, left)), scalarMult(-1, range(right, n))
                );
            int[] positives = range(right, left);

            VecInt clause = new VecInt(addAll(negativs, positives));
            solver.addClause(clause);
        }
        int k = g.getPathwidth();
        solver.addAtMost(new VecInt(range(0, n)), k);
    }

        
    private boolean solveSATInstance() {
        boolean satisfiable = solver.isSatisfiable();
        
        if (satisfiable) {
            System.out.println("Das Problem ist erfüllbar!");

            int[] solution = solver.model();  
            System.out.println("Lösungsbelegung: " + Arrays.toString(solution));
        } else {
            System.out.println("Das Problem ist nicht erfüllbar.");
        }
        return satisfiable;
    }

    private int[] range(int a, int b) {
        return IntStream.rangeClosed(a, b).toArray();     
    }

    private int[] scalarMult(int scalar, int[] array) {
        for (int i=0; i<array.length; i++) {
            array[i] *= scalar;
        }
        return array;
    }

    public static int[] addAll(int[] array1, int[] array2) {
        int[] result = new int[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

}
