package edu.kit.iti.formal.pathwidth;

/**
 * A {@link Graph} game can either be solvable or not.
 * Use <code>SAT</code> iff you find a solution for the given graph instance
 * or <code>UNSAT</code> if there is no solution given.
 * @author Samuel Teuber
 */
public enum SolutionState {
    /**
     * No solution decision was set.
     */
    UNKNOWN,
    /**
     * Game has a solution, a valid assignment of white/black fields.
     */
    SAT,
    /**
     * Game has <b>no</b> solution
     */
    UNSAT;
}
