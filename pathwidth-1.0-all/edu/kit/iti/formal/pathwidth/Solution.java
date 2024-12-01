package edu.kit.iti.formal.pathwidth;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.PrintStream;
import java.util.*;

/**
 * This class represents a solution to a {@link Graph} game.
 * <p>
 * If you found a solution, please use {@link Solution#setState(SolutionState)} )} and {@link Solution#setInterval(int, float, float)}
 *
 * @author Samuel Teuber
 */
public class Solution {

    private final Graph g;

    /**
     * Solution intervals of nodes;
     * note the interval from a to b is understood as inclusive (i.e. [a,b])
     */
    private final float[][] solution;

    private SolutionState state = SolutionState.UNKNOWN;

    /**
     * Constructs a solution for a given game.
     * <p>
     * Initialize the internal data structure of the game field.
     *
     * @param g -- a {@link Graph} graph
     */
    public Solution(Graph g) {
        solution = new float[g.getNumNodes()][2];
        this.g = g;
    }

    /**
     * Indices start with 0;
     * Note, that the interval from a to b is understood as inclusive (i.e. [a,b])
     * and must be non-trivial (i.e. a &lt; b)
     *
     * @param i index of node
     * @param a lower end of interval
     * @param b upper end of interval
     */
    public void setInterval(int i, float a, float b) {
        assert 0 <= i && i < g.getNumNodes();
        assert a < b;
        solution[i][0]=a;
        solution[i][1]=b;
    }

    /**
     * Returns the interval of graph node of index i
     * @param i Node index
     */
    public float[] getInterval(int i) {
        assert 0 <= i && i < g.getNumNodes();
        return solution[i];
    }

    /**
     * @return return true if the solution fulfills all constraints
     */
    public boolean isValid() {
        // no lazy evaluation
        return kConstraintSatisfied() && edgeConstraintsSatisfied();
    }

    private boolean edgeConstraintsSatisfied() {
        for (Iterator<GraphEdge> it = g.getEdgeIterator(); it.hasNext(); ) {
            GraphEdge curEdge = it.next();
            float[] interval1 = getInterval(curEdge.getEndpoint1());
            float[] interval2 = getInterval(curEdge.getEndpoint2());
            if (interval1[1] < interval2[0] || interval2[1] < interval1[0]) {
                // Two graph nodes which have to be connected are disconnected in the interval representation
                System.out.println("Node "+Integer.toString(curEdge.getEndpoint1())+" and "+Integer.toString(curEdge.getEndpoint2())+" should be connected, but are not.");
                return false;
            }
        }
        return true;
    }

    private boolean kConstraintSatisfied() {
        int maxK = 1;
        float pos = 0.0f;
        for(int nodeIndex=0; nodeIndex< g.getNumNodes(); nodeIndex++) {
            if (solution[nodeIndex][0] >= solution[nodeIndex][1]) {
                System.out.printf("Empty interval detected for node %d\n", nodeIndex);
                return false;
            }
            for (int i = 0; i < 1; i++) {
                int curK = 0;
                float curVal = solution[nodeIndex][i];
                for (int secondNodeIndex = 0; secondNodeIndex < g.getNumNodes(); secondNodeIndex++) {
                    if (solution[secondNodeIndex][0] <= curVal && curVal <= solution[secondNodeIndex][1]) {
                        curK++;
                    }
                }
                if (curK > maxK) {
                    maxK = curK;
                    pos = curVal;
                }
            }
        }
        if ((maxK - 1) > g.getPathwidth()) {
            System.out.printf("%.2f has more than %d overlaps\n", pos, g.getPathwidth()+1);
        }
        return ((maxK - 1) <= g.getPathwidth());
    }

    /**
     * @return SAT/UNSAT
     */
    public SolutionState getState() {
        return state;
    }

    /**
     * Sets the state of the solution, SAT means there is a solution otherwise UNSAT.
     *
     * @param state state of the solution
     */
    public void setState(@NotNull SolutionState state) {
        this.state = state;
    }


    /**
     * Helper method to print the interval graph
     * @param report
     */
    public void print(PrintStream report) {
        if (this.state == SolutionState.SAT) {
            for (int i = 0; i < g.getNumNodes(); i++) {
                report.print("Node ");
                report.print(i);
                report.print(": [");
                report.print(String.format(Locale.US,"%.2f", solution[i][0]));
                report.print(",");
                report.print(String.format(Locale.US,"%.2f", solution[i][1]));
                report.print("]");
                report.println();
            }
        } else {
            report.println("UNSAT");
        }
    }

    //endregion
}

