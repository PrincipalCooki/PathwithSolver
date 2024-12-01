package edu.kit.iti.formal.pathwidth;

/**
 * Edge of a graph represented by its two ends (getEndpoint1, getEndpoint2)
 * The edge is undirected, i.e.
 *
 * @author Samuel Teuber
 */
public class GraphEdge {
    private int endpoint1;
    private int endpoint2;

    /**
     * Creates an undirected graph edge
     * @param a First end of edge
     * @param b Second end of edge
     */
    protected GraphEdge(int a, int b) {
        this.endpoint1 = a;
        this.endpoint2 = b;
    }

    /**
     * @return One end of the graph edge; note the edge is undirected
     */
    public int getEndpoint1() {
        return endpoint1;
    }

    /**
     * @return One end of the graph edge; note the edge is undirected
     */
    public int getEndpoint2() {
        return endpoint2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GraphEdge) {
            GraphEdge otherEdge = (GraphEdge) obj;
            return (this.endpoint1==otherEdge.endpoint1 && this.endpoint2 == otherEdge.endpoint2) ||
                   (this.endpoint1==otherEdge.endpoint2 && this.endpoint2 == otherEdge.endpoint1);
        }
        return false;
    }
}
