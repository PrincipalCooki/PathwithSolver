import edu.kit.iti.formal.pathwidth.*;

import java.util.Iterator;

import org.sat4j.core.Vec;
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
        clauseMembership();
        clauseOverlapping();

        pathwidthCondition();


        int[] model = solveSAT();
        computeIntervals(model);

        return solution;
    }

    // for every x there are only k many M(v,x) true
    private void pathwidthCondition() {
        int k = g.getPathwidth();

        System.out.println("Pathwidth: " + k);

        for(int x = 0; x < n; x++) {
            VecInt clause = new VecInt();
            System.out.println("x: " + x);
            for(int node = 0; node < n; node++) {
                clause.push(member[node][x]);
                System.out.println("member: " + (member[node][x] + 1) );
            }
            
            try {
                solver.addAtMost(clause, 1);
            } catch (ContradictionException e) {
                System.out.println("Pathwidth Condition error");
            }
        }
    }

    private int[] solveSAT() {
        try {
            if(solver.isSatisfiable()) {
                solution.setState(SolutionState.SAT);
                int[] model = solver.model();
                for(int i = 0; i < model.length; i++) {
                    System.out.println(model[i]);
                }
                System.out.println("Member literals");
                for(int node = 0; node < n; node++) {
                    System.out.println("node: " + node);
                    for(int i = 0; i < n; i++) {
                        System.out.println(model[member[node][i]-1]);
                    }
                }
                System.out.println("Lower literals");
                for(int node = 0; node < n; node++) {
                    System.out.println("node: " + node);
                    for(int i = 0; i < n; i++) {
                        System.out.println(model[lower[node][i]-1]);
                    }
                }
                System.out.println("Upper literals");
                for(int node = 0; node < n; node++) {
                    System.out.println("node: " + node);
                    for(int i = 0; i < n; i++) {
                        System.out.println(model[upper[node][i]-1]);
                    }
                }
                return model;
            } else {
                solution.setState(SolutionState.UNSAT);
                return new int[0];
            }
        } catch (TimeoutException e) {
            System.out.println("Timeout error");
            return new int[0];
        }
    }

    private void computeIntervals(int[] model) {
        for(int node = 0; node < n; node++) {
            int lower_bound = -1;
            int upper_bound = -1;
            for(int i = 0; i < n; i++) {
                if(model[lower[node][i]-1] > 0) {
                    lower_bound = i+1;
                }
                if(model[upper[node][i]-1] > 0) {
                    upper_bound = i+1;
                    break;
                }
            }
            solution.setInterval(node, lower_bound, upper_bound);
        }
    }

    private void buildLiterals() {
        int counter = 1;
        for (int node = 0; node < n; node++) {
            for(int i = 0; i < n; i++) {
                lower[node][i] = counter++;
                upper[node][i] = counter++;
                member[node][i] = counter++;
            }
        }
    }

    // L(v) = i => U(v) = i:n 
    // representation: -L(v) =i || U = i:n || -L(v) = 1, ...
    // one lower bound must be set: {L(v) = 0, L(v) = 1,... ,L(v) = n-1}   
    // and only one of them can be true: addExactly(L(v) =0, ...1) 
    private void clauseConsistency() {
        try {
            for(int node = 0; node < n; node++) {

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

                for(int low = 0; low <n; low++) {
                    VecInt clause = new VecInt(new int[] {-lower[node][low]});
                    for(int up = low + 1; up < n; up++) {
                        clause.push(upper[node][up]);
                    }    
                    solver.addClause(clause);
                    }
                }
            }   catch (ContradictionException e) {
            System.out.println("consistency Clause error");
        }

    }


    // WRONG: -c must be rewritten with de morgan
    // b: L(v) < x: L(v) = 0, L(v) = 1,... ,L(v) = x 
    // c: U(v) > x: U(v) = x, U(v) = x+1,... ,U(v) = n-1 
    // M(v,x) <=> (L(v) <= x && U(v) >= x)
    // a <=> (b && c)
    // (a => (b && c) ) && ((b && c) => a)
    // (-a || (b && c)) && (-(b && c) || a)
    // (-a || b) && (-a || c) && (-b || -c || a)
    private void clauseMembership() {
        for(int node = 0; node < n; node++) {
            for(int x = 0; x < n; x++) {

                VecInt clause_ab = new VecInt();
                VecInt clause_ac = new VecInt();
                VecInt clause_abc = new VecInt();

                VecInt clause_a_neg = new VecInt();
                clause_a_neg.push(-member[node][x]);
                VecInt clause_a_pos= new VecInt();
                clause_a_pos.push(member[node][x]);


                clause_ab.pushAll(clause_a_neg);
                clause_ac.pushAll(clause_a_neg);
                clause_abc.pushAll(clause_a_pos);

                
                VecInt clause_b_pos = new VecInt();
                VecInt clause_b_neg = new VecInt();
                VecInt clause_c_pos = new VecInt();
                VecInt clause_c_neg = new VecInt();
                
                for(int low = 0; low <= x; low++) {
                    clause_b_pos.push(lower[node][low]);
                    clause_b_neg.push(-lower[node][low]);
                }

                for(int up = x; up < n; up++) {
                    clause_c_pos.push(upper[node][up]);
                    clause_c_neg.push(-upper[node][up]);
                }

                System.out.println("critical ones: ");
                System.out.println(clause_b_pos.toString());
                System.out.println(clause_c_pos.toString());
                System.out.println(clause_a_pos.toString());

                clause_ab.pushAll(clause_b_pos);
                clause_ac.pushAll(clause_c_pos);
                clause_abc.pushAll(clause_b_neg); 
                clause_abc.pushAll(clause_c_neg); 
               
                // (-a || b) && (-a || c) && (-b || -c || a)
                if (member[node][x] == 9) {
                    System.out.println("clause (-a || b): " + clause_ab.toString());
                    System.out.println("clause (-a || c): " + clause_ac.toString());
                    System.out.println("clause (-b || -c || a): " + clause_abc.toString());
                }

                try {
                    solver.addClause(clause_ab);
                    solver.addClause(clause_ac);
                    solver.addClause(clause_abc);
                } catch (ContradictionException e) {
                    System.out.println("Membership Clause error");
                }

            }
        }
    }

    //New try:
    // 

    // e = uv:  L(v) < U(u) && L(u) < L(v) || U(v) < U(u) && L(u) < U(v) 
    //                a     &&      b      ||      c      &&      d
    // a: L(v) < U(u): L(v) = i => U(u) = i:n 
    // equivilant to: {-L(v) = i, U(u) = i, U(u) = i+1, ... , U(u) = n-1} 
    // {a, b} {b, c} {a, d} {b, d}

    private void clauseOverlapping() {
        Iterator<GraphEdge> edgeIterator = g.getEdgeIterator();
        while (edgeIterator.hasNext()) {
            GraphEdge edge = edgeIterator.next();
            
            VecInt a = new VecInt();
            VecInt b = new VecInt();
            VecInt c = new VecInt();
            VecInt d = new VecInt();

            VecInt ab = new VecInt();
            VecInt bc = new VecInt();
            VecInt ad = new VecInt();
            VecInt bd = new VecInt();


            for(int l_v = 0; l_v < n; l_v++) {
                a.push(-lower[edge.getEndpoint2()][l_v]);
                for(int u_u = l_v; u_u < n; u_u++) {
                    a.push(upper[edge.getEndpoint1()][u_u]);
                }
            }
            for(int l_u = 0; l_u < n; l_u++) {
                b.push(-lower[edge.getEndpoint1()][l_u]);
                for(int l_v = l_u; l_v < n; l_v++) {
                    b.push(upper[edge.getEndpoint2()][l_v]);
                }
            }
            for(int u_v = 0; u_v < n; u_v++) {
                c.push(-lower[edge.getEndpoint2()][u_v]);
                for(int u_u = u_v; u_u < n; u_u++) {
                    c.push(upper[edge.getEndpoint1()][u_u]);
                    }
            }
            for(int l_u = 0; l_u < n; l_u++) {
                d.push(-lower[edge.getEndpoint1()][l_u]);
                for(int u_v = l_u; u_v < n; u_v++) {
                    d.push(upper[edge.getEndpoint2()][u_v]);
                }
            }
            ab.pushAll(a);
            ab.pushAll(b);
            bc.pushAll(b);
            bc.pushAll(c);
            ad.pushAll(a);
            ad.pushAll(d);
            bd.pushAll(b);
            bd.pushAll(d);
            
            try {
                solver.addClause(ab);
                solver.addClause(ad);
                solver.addClause(bc);
                solver.addClause(bd);
            } catch (ContradictionException e) {
                System.out.println("Overlapping Clause error");
            }
            

        }
    }
        



    // e = uv:  M(v,x) && M(u,x) || M(v,x+1) && M(u,x+1) || ... || M(v,n-1) && M(u,n-1)
    // equivilant to (De Morgan):
    // not {-M(v,1), -M(u,1)}, {-M(v,2), -M(u,3)}, ...
    private void clauseNeighbours() {

        Iterator<GraphEdge> edgeIterator = g.getEdgeIterator();
        while (edgeIterator.hasNext()) {
            GraphEdge edge = edgeIterator.next();
            try {
                for(int x = 0; x < n; x++) {;
                    solver.addClause(new VecInt(new int[] {
                        -member[edge.getEndpoint1()][x],
                        -member[edge.getEndpoint2()][x]
                        }));
                }
            } catch (ContradictionException e) {
                System.out.println("Neighbours Clause error");
            }
        }
    }

    // Point x is in the interval [a,b] 
    private void clauseOverlappingIntervals() {

    }
}


