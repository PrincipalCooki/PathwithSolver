package edu.kit.iti.formal.pathwidth;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

/**
 * This is the base class for your solution.
 * <p>
 * Your solution needs to implement:
 * <ul>
 * <li>a constructor accepting a {@link Graph} instance</li>
 * <li>The solve method, returning the solution for the given graph instance.</li>
 * </ul>
 * <p>You have access to three members variables:
 * <ul>
 * <li>sat solver instance</li>
 * <li>graph instance</li>
 * <li>solution instance</li>
 * </ul>
 * <p>
 * For every riddle this class is instantiated.
 * Feel free add member variables to store information over the current graph,
 * e.g. mapping of nodes to propostional variables.
 * <p>
 * Have a lot of fun...
 *
 * @author Samuel Teuber
 */
public abstract class PathwidthSolver {
    /**
     * The {@link Graph} to solve
     */
    protected final Graph graph;

    /**
     * a preinitialized {@link Solution} instance,
     * should be returned in solve()
     */
    protected Solution solution;

    /**
     * The interface of the SAT Solver.
     */
    protected final ISolver solver;

    /**
     * This constructor is called from {@link PathwidthTest}.
     * Please ensure, that this constructor is present in your implementation.
     *
     * @param g the graph instance, that you should solve.
     */
    public PathwidthSolver(Graph g) {
        graph = g;
        solution = new Solution(g);
        solver = SolverFactory.newDefault();

        String timeout = System.getProperties().getProperty("SAT_TIMEOUT", null);
        if (timeout != null) {
            solver.setTimeout(Integer.parseInt(timeout));
        }
    }

    /**
     * Compute the solution of the given {@link Graph} instance.
     *
     * @return the solution (assignment of node intervals)
     */
    public abstract Solution solve();



    /**
     * Adds the given literals as one clause to the sat solver instance.
     * @param c the literals
     */
    public void addClause(int... c) throws ContradictionException {
        solver.addClause(new VecInt(c));
    }
}
