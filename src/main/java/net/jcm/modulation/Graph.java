package net.jcm.modulation;

import imgui.extension.nodeditor.flag.NodeEditorPinKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Graph {
    public AtomicInteger nodeId = new AtomicInteger(1);
    public AtomicInteger pinId = new AtomicInteger(1);

    public final Map<Integer, GraphNode> nodes = new HashMap<>();

    public Graph() {}

    public GraphNode createGraphNode() {
        final GraphNode node = new GraphNode(nodeId.getAndIncrement(), pinId.getAndIncrement(), pinId.getAndIncrement());
        this.nodes.put(node.nodeId, node);
        return node;
    }

    public GraphNode findByInput(final long inputPinId) {
        for (GraphNode node : nodes.values()) {
            if (node.getInputPinId() == inputPinId) {
                return node;
            }
        }
        return null;
    }

    public GraphNode findByOutput(final long outputPinId) {
        for (GraphNode node : nodes.values()) {
            if (node.getOutputPinId() == outputPinId) {
                return node;
            }
        }
        return null;
    }

    public record GraphType(String displayName, Map<String, NodeEditorPinKind> pins){}


    public static final class GraphNode {
        public final int nodeId;
        public final int inputPinId;
        public final int outputPinId;

        public int outputNodeId = -1;

        public GraphNode(final int nodeId, final int inputPinId, final int outputPintId) {
            this.nodeId = nodeId;
            this.inputPinId = inputPinId;
            this.outputPinId = outputPintId;
        }

        public int getInputPinId() {
            return inputPinId;
        }

        public int getOutputPinId() {
            return outputPinId;
        }

        public String getName() {
            return "Node " + (char) (64 + nodeId);
        }
    }
}