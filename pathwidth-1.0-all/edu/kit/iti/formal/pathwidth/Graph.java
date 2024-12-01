package edu.kit.iti.formal.pathwidth;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;

/**
 * This class holds the information of a Graph
 * It holds the number of nodes (getNumNodes) and edges (getNumEdges).
 * Nodes are referenced by their unique id from 0 to getNumNodes()-1
 * The class provides an iterator over the edges of the graph (getEdgeIterator).
 *
 * @author Samuel Teuber
 */
public class Graph {

    private File loadedFrom;

    private int numNodes;

    private int pathwidth;
    private final ArrayList<GraphEdge> graphEdges = new ArrayList<GraphEdge>();

    private Graph() {
    }


    /**
     * @return The number of nodes in the graph
     */
    public int getNumNodes() {
        return numNodes;
    }

    /**
     * @return The number of edges in the graph
     */
    public int getNumEdges() {
        return graphEdges.size();
    }

    /**
     * @return The maximally allowed pathwidth
     */
    public int getPathwidth() {
        return pathwidth;
    }

    /**
     * @return The edges of the graph
     */
    public Iterator<GraphEdge> getEdgeIterator() {
        return graphEdges.iterator();
    }

    /**
     * @param file File to load
     * @return An instance of Graph
     * @throws IOException In case file cannot be loaded
     */
    public static @NotNull Graph load(@NotNull File file) throws IOException {
        try {
            Map<String,Integer> nodeToId = new HashMap<>();
            Graph g = new Graph();
            g.loadedFrom = file;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(file);
            Element root = doc.getDocumentElement();
            assert root.getNodeName().equals("graphml");
            // Load nodes
            NodeList nodeList = root.getElementsByTagName("node");
            int nextIndex = 0;
            for (int nodeIndex = 0; nodeIndex < nodeList.getLength(); nodeIndex++) {
                nodeToId.put(((Element) nodeList.item(nodeIndex)).getAttribute("id"), nextIndex);
                nextIndex++;
            }
            g.numNodes = nodeToId.size();
            // Load edges
            NodeList edgeList = root.getElementsByTagName("edge");
            int numEdges = edgeList.getLength();
            g.graphEdges.ensureCapacity(numEdges);
            for (int edgeIndex = 0; edgeIndex < edgeList.getLength(); edgeIndex++) {
                Element edgeElement = (Element) edgeList.item(edgeIndex);
                g.graphEdges.add(new GraphEdge(
                        nodeToId.get(edgeElement.getAttribute("source")),
                        nodeToId.get(edgeElement.getAttribute("target"))
                ));
            }

            // TODO(steuber): Initialize pathwidth
            g.pathwidth = 20;
            NodeList pathwidth = root.getElementsByTagName("pathwidth");
            if (pathwidth.getLength()<1) {
                g.pathwidth=20;
            } else {
                g.pathwidth = Integer.parseInt(((Element) pathwidth.item(0)).getAttribute("value"));
            }

            return g;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

}
