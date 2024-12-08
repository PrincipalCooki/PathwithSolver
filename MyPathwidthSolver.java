import edu.kit.iti.formal.pathwidth.*;

import java.util.Iterator;
import java.util.Map;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

// Alle knoten haben ein intervall: 

public class MyPathwidthSolver extends PathwidthSolver {
    private Graph g;
    private int n;
    private int counter;

    private int[][] lower; // lower[v][i] is: L(v) = i
    private int[][] upper;
    private int[][] member; // member[v][x] is: x element [L(v), U(v)];
    private int[][] lower_range;
    private int[][] upper_range;
    private int[][] edgeMember;

    public MyPathwidthSolver(Graph g) {
        super(g);
        this.g = g;
        this.n = g.getNumNodes();

        lower = new int[n][n]; // lower[v][i] is: L(v) = i
        upper = new int[n][n];
        member = new int[n][n]; // member[v][x] is: x element [L(v), U(v)]

        lower_range = new int[n][n];
        upper_range = new int[n][n];

        int m = g.getNumEdges();
        edgeMember = new int[m][n];
    }

    @Override
    public Solution solve() {
        // TODO: Implement solution
        solution.setState(SolutionState.UNKNOWN);

        System.out.println("Hello world");
        buildLiterals();
        clauseConsistency();
        clauseSetRanges();
        clauseMembership();
        clauseNeighbours();

        pathwidthCondition();

        int[] model = solveSAT();
        computeIntervals(model);

        System.out.println("edgeMember");
        printLiterals(model, edgeMember);
        System.out.println("Member");
        printLiterals(model, member);
        System.out.println("lowerRange");
        printLiterals(model, lower_range);
        System.out.println("lower  Range");
        printLiterals(model, lower);
        System.out.println("Upper  Range");
        printLiterals(model, upper_range);
        System.out.println("Upper  ");
        printLiterals(model, upper);




        return solution;
    }

    // for every x there are only k many M(v,x) true
    private void pathwidthCondition() {
        int k = g.getPathwidth();

        System.out.println("Pathwidth: " + k);

        for (int x = 0; x < n; x++) {
            VecInt clause = new VecInt();
            for (int node = 0; node < n; node++) {
                clause.push(member[node][x]);
            }

            try {
                solver.addAtMost(clause, k + 1);
            } catch (ContradictionException e) {
                System.out.println("Pathwidth Condition error");
            }
        }
    }

    private void printLiterals(int[] satModel, int[][] literals) {
        // Create a map of variable assignments from satModel
        Map<Integer, Integer> assignments = new HashMap<>();
        for (int var : satModel) {
            int varIndex = Math.abs(var);
            int value = var > 0 ? 1 : 0;
            assignments.put(varIndex, value);
        }

        // Build and print the result matrix
        for (int i = 0; i < literals.length; i++) {
            for (int j = 0; j < literals[i].length; j++) {
                int literal = literals[i][j];
                int varIndex = Math.abs(literal);
                int value = assignments.getOrDefault(varIndex, 0);
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    private int[] solveSAT() {
        try {
            if (solver.isSatisfiable()) {
                System.out.println("SAT");
                solution.setState(SolutionState.SAT);
                int[] model = solver.model();
                return model;
            } else {
                solution.setState(SolutionState.UNSAT);
                System.out.println("UNSAT");
                return new int[0];
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout error");
            return new int[0];
        }
    }

    private void computeIntervals(int[] model) {
        for (int node = 0; node < n; node++) {
            int lower_bound = -1;
            int upper_bound = -1;

            for (int i = 0; i < n; i++) {
                if(model[lower_range[node][i] - 1] > 0) {
                    lower_bound = i + 1;
                    break;
                }
            }
            for (int i = n-1; i > 0; i--) {
                if(model[upper_range[node][i] - 1] > 0) {
                    upper_bound = i + 1;
                    break;
                }
            }

            for (int i = 0; i < n; i++) {
                if (model[lower[node][i] - 1] > 0) {
                    lower_bound = i + 1;
                }
                if (model[upper[node][i] - 1] > 0) {
                    upper_bound = i + 1;
                    break;
                }
            }
            solution.setInterval(node, lower_bound, (float) (upper_bound + 0.5));
        }
    }

    private void buildLiterals() {
        counter = 1;
        for (int node = 0; node < n; node++) {
            for (int i = 0; i < n; i++) {
                lower[node][i] = counter++;
                upper[node][i] = counter++;
                member[node][i] = counter++;
                lower_range[node][i] = counter++;
                upper_range[node][i] = counter++;
            }
        }

    }

    // L(v) = i => U(v) = i:n
    // L(v) = i => L(V) = i:n
    // U(v) = i => U(v) = 0:i
    // representation: L(v) = i => L(v) = i+1, L(v) = i+2, ... , L(v) = n-1
    // representation: U(v) = i => U(v) = 0, U(v) = 1, ... , U(v) = i-1
    // same as: -L(v) = i || L
    private void clauseConsistency() {
        try {
            for (int node = 0; node < n; node++) {

                VecInt clause_set_lower_bound = new VecInt();
                for (int low = 0; low < n; low++) {
                    clause_set_lower_bound.push(lower[node][low]);
                }
                solver.addExactly(clause_set_lower_bound, 1);

                VecInt clause_set_upper_bound = new VecInt();
                for (int up = 0; up < n; up++) {
                    clause_set_upper_bound.push(upper[node][up]);
                }
                solver.addExactly(clause_set_upper_bound, 1);

                for (int low = 0; low < n; low++) {
                    VecInt clause = new VecInt(new int[] { -lower[node][low] });
                    for (int up = low + 1; up < n; up++) {
                        clause.push(upper[node][up]);
                    }
                    solver.addClause(clause);
                }
            }
        } catch (ContradictionException e) {
            System.out.println("consistency Clause error");
        }
    }

    // not L(v) = i => not L(v) = i-1
    // {L(v) = i, -L(v) = i-1}
    private void clauseSetRanges() {
        for (int node = 0; node < n; node++) {
            VecInt clauseSetRangeLow = new VecInt();
            
            VecInt clauseUntilL = new VecInt();
            
            VecInt clauseSetRangeUp = new VecInt();

            for (int low = 0; low < n; low++) {
                clauseSetRangeLow.push(-lower[node][low]);
                clauseSetRangeLow.push(lower_range[node][low]);

                if (low > 0) {
                    clauseUntilL.push(lower_range[node][low]);
                    clauseUntilL.push(-lower_range[node][low-1]);
                }


                if (low < n - 1) {
                    VecInt clauseContinueLow = new VecInt();
                    clauseContinueLow.push(-lower_range[node][low]);
                    clauseContinueLow.push(lower_range[node][low + 1]);
                    try {
                        solver.addClause(clauseContinueLow);
                    } catch (ContradictionException e) {
                        e.printStackTrace();
                    }
                }

                clauseSetRangeUp.push(-upper[node][low]);
                clauseSetRangeUp.push(upper_range[node][low]);
                if (low > 0) {
                    VecInt clauseContinueUp = new VecInt();
                    clauseContinueUp.push(-upper_range[node][low]);
                    clauseContinueUp.push(upper_range[node][low - 1]);
                    try {
                        solver.addClause(clauseContinueUp);
                    } catch (ContradictionException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    solver.addClause(clauseSetRangeUp);
                    solver.addAtLeast(clauseSetRangeUp, 1);
                    solver.addClause(clauseSetRangeLow);
                    solver.addAtLeast(clauseSetRangeLow, 1);
                } catch (ContradictionException e) {
                    e.printStackTrace();
                }

                clauseSetRangeLow.clear();
                clauseSetRangeUp.clear();
            }
        }
    }

    // M(v,x) <=> L_range(v) == x && x == U_range(v)
    // {M(v,x), -L_range(v), U_range(v)}
    // {-M(v,x), L_range(v)}
    // {-M(v,x), U_range(v)}
    // {a, -b, -c} {-a, b} {-a, c}

    // and every intervall must contain one element
    // {M(v,x), M(v, x+1),...}
    private void clauseMembership() {
        for (int node = 0; node < n; node++) {
            VecInt clauseLeastOneMember = new VecInt();
            for (int x = 0; x < n; x++) {
                clauseLeastOneMember.push(member[node][x]);

                VecInt clause_ab = new VecInt();
                VecInt clause_ac = new VecInt();
                VecInt clause_abc = new VecInt();

                clause_ab.push(-member[node][x]);
                clause_ac.push(-member[node][x]);
                clause_abc.push(member[node][x]);

                clause_ab.push(lower_range[node][x]);
                clause_ac.push(upper_range[node][x]);
                clause_abc.push(-upper_range[node][x]);
                clause_abc.push(-lower_range[node][x]);

                try {
                    solver.addClause(clause_ab);
                    solver.addClause(clause_ac);
                    solver.addClause(clause_abc);
                } catch (ContradictionException e) {
                    System.out.println("Membership Clause error");
                }

            }
            try {
                solver.addClause(clauseLeastOneMember);
            } catch (ContradictionException e) {
                System.out.println("Membership Clause error");
            }
        }
    }

    // e = uv: M(v,x) && M(u,x) || M(v,x+1) && M(u,x+1) || ... || M(v,n-1) &&
    // h(e,x) true if x lies in the edge interval of u and v
    // h(e,x) <=> (M(v,x) && M(u,x))
    // {h(e,x), -M(v,x), -M(u,x)}
    // {-h(e,x), M(v,x)}
    // {-h(e,x), M(u,x)}

    private void clauseNeighbours() {

        Iterator<GraphEdge> edgeIterator = g.getEdgeIterator();
        int edgeIndex = 0;
        while (edgeIterator.hasNext()) {
            GraphEdge edge = edgeIterator.next();
            VecInt clauseLeastOneMember = new VecInt();

            int u = edge.getEndpoint1();
            int v = edge.getEndpoint2();

            System.out.println("Edge: " + u + " " + v);

            for (int x = 0; x < n; x++) {
                edgeMember[edgeIndex][x] = counter++;
                clauseLeastOneMember.push(edgeMember[edgeIndex][x]);

                VecInt clause_ab = new VecInt();
                VecInt clause_ac = new VecInt();
                VecInt clause_abc = new VecInt();

                clause_ab.push(-edgeMember[edgeIndex][x]);
                clause_ac.push(-edgeMember[edgeIndex][x]);
                clause_abc.push(edgeMember[edgeIndex][x]);

                clause_ab.push(member[u][x]);
                clause_ac.push(member[v][x]);
                clause_abc.push(-member[u][x]);
                clause_abc.push(-member[v][x]);

                try {
                    solver.addClause(clause_ab);
                    solver.addClause(clause_ac);
                    solver.addClause(clause_abc);
                } catch (ContradictionException e) {
                    System.out.println("Membership Clause error");
                }
            }
            try {
                solver.addAtLeast(clauseLeastOneMember, 1);
            } catch (ContradictionException e) {
                System.out.println("Membership Clause error");
            }
            edgeIndex++;
        }
    }
}
