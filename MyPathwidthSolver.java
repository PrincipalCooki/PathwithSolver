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
        
    private int[][] lower; //lower[v][i] is: L(v) = i
    private int[][] upper;
    private int[][] member; //member[v][x] is: x element [L(v), U(v)]

    public MyPathwidthSolver(Graph g){
        super(g);
        this.g = g;
        this.n = g.getNumNodes();
        
        lower = new int[n][n]; //lower[v][i] is: L(v) = i
        upper = new int[n][n];
        member = new int[n][n]; //member[v][x] is: x element [L(v), U(v)]

        
    }

    @Override
    public Solution solve() {
        // TODO: Implement solution
        solution.setState(SolutionState.UNKNOWN);
        
        System.out.println("Hello world");
        buildLiterals();
        clauseConsistency();

        return solution;
    }
    private void buildLiterals() {
        int counter = 1;
        for (int node = 0; node < n; node++) {
            for(int i = 0; i < n; i++) {
                this.lower[node][i] = counter++;
                upper[node][i] = counter++;
                member[node][i] = counter++;
            }
        }
    }

    // Clause for every node, that L(v) <= U(v) holds
    // representation: L || U 
    private void clauseConsistency() {
        
        for(int node = 0; node < n; node++) {
            for(int low = 0; low <n; low++) {
                for(int up = low; up < n; up++) {
                    VecInt clause = new VecInt(new int[] {lower[node][low], upper[node][up]});
                    try {
                        solver.addClause(clause);
                    } catch (ContradictionException e) {
                        System.out.println("Widerspruch bei der Klausel: " + lower[node][low] + ", " + upper[node][up]);
                    }
                    // solver.addClause(clause);
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
