package com.maxdemarzi;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ExternalMapQueryContext;
import net.openhft.chronicle.map.MapAbsentEntry;
import net.openhft.chronicle.map.MapEntry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChronicleGraph {

    private static ChronicleMap<Integer, Map<String, Object>> nodes;
    private static ChronicleMap<String, Map<String, Object>> relationships;
    private static HashMap<String, ChronicleMap<Integer, Set<Integer>>> related = new HashMap<>();
    private static final AtomicInteger nodeCounter = new AtomicInteger();

    public ChronicleGraph(Integer maxNodes, Integer maxRelationships) {
        HashMap<String, Object> relProperties = new HashMap<>();
        relProperties.put("one", 10000);

        relationships = ChronicleMap
                .of(String.class, (Class<Map<String, Object>>) (Class) Map.class)
                .name("relationships")
                .entries(maxRelationships)
                .averageValue(relProperties)
                .averageKey("100000-100000-TYPE")
                .create();

        HashMap<String, Object> nodeProperties = new HashMap<>();
        nodeProperties.put("one", 10000);
        nodeProperties.put("two", "username");
        nodeProperties.put("three", "email@yahoo.com");
        nodeProperties.put("four", 50.55D);

        nodes = ChronicleMap
                .of(Integer.class, (Class<Map<String, Object>>) (Class) Map.class)
                .name("nodes")
                .entries(maxNodes)
                .averageValue(nodeProperties)
                .create();
    }


    public void addRelationshipType(String type, Integer maximum, Integer average_outgoing, Integer average_incoming) {
        HashSet<Integer> avgOutgoingValue = new HashSet<>();
        for (int i = 0; i < average_outgoing; i++) {
            avgOutgoingValue.add(i);
        }

        HashSet<Integer> avgIncomingValue = new HashSet<>();
        for (int i = 0; i < average_incoming; i++) {
            avgIncomingValue.add(i);
        }

        ChronicleMap<Integer, Set<Integer>> cmOut = ChronicleMap
                .of(Integer.class, (Class<Set<Integer>>) (Class) Set.class)
                .name(type+ "-out")
                .entries(maximum)
                .averageValue(avgOutgoingValue)
                .create();
        ChronicleMap<Integer, Set<Integer>> cmIn = ChronicleMap
                .of(Integer.class, (Class<Set<Integer>>) (Class) Set.class)
                .name(type+ "-in")
                .entries(maximum)
                .averageValue(avgIncomingValue)
                .create();

        related.put(type + "-out", cmOut);
        related.put(type + "-in", cmIn);
    }

    public HashMap<String, Object> getRelationshipTypeAttributes(String type) {
        HashMap<String, Object> attributes = new HashMap<>();
        ChronicleMap relationshipTypeOut = related.get(type+"-out");
        ChronicleMap relationshipTypeIn = related.get(type+"-in");

        attributes.put(relationshipTypeOut.name(), relationshipTypeOut.size());
        attributes.put(relationshipTypeIn.name(), relationshipTypeIn.size());

        return attributes;
    }

    public Integer addNode () {
        return addNode(new HashMap<>());
    }

    public Integer addNode (HashMap<String, Object> properties) {
        Integer nodeId = nodeCounter.incrementAndGet();
        nodes.put(nodeId, properties);
        return nodeId;
    }

    public Map<String, Object> getNode(Integer id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id);
        } else {
            return new HashMap<>();
        }
    }

    public String addRelationship (String type, Integer from, Integer to) {
        if(related.containsKey(type+"-out")) {
            addEdge(related.get(type + "-out"), from, to);
            addEdge(related.get(type + "-in"), to, from);
            return from + "-" + to + type;
        } else {
            // TODO: 12/29/16 Maybe create it with default values instead
            throw new IllegalStateException("Relationship Type: " + type + " should be present in the graph. Try addRelationshipType()");
        }
    }

    public String addRelationship (String type, Integer from, Integer to, HashMap<String, Object> properties) {
        relationships.put(from + "-" + to + type, properties);
        addEdge(related.get(type+"-out"), from, to);
        addEdge(related.get(type+"-in"), to, from);
        return  from + "-" + to + type;
    }

    public Map<String, Object> getRelationship(String type, Integer from, Integer to) {
        return relationships.get(from + "-" + to + type);
    }

    public String removeRelationship (String type, Integer from, Integer to) {
        removeEdge(related.get(type), from, to);
        relationships.remove(from + "-" + to + type);
        return  from + "-" + to + type;
    }

    public Set<Integer> getOutgoingRelationships(String type, Integer from) {
        return related.get(type+"-out").get(from);
    }

    public Set<Integer> getIncomingRelationships(String type, Integer to) {
        return related.get(type+"-in").get(to);
    }
    private static boolean addEdge(ChronicleMap<Integer, Set<Integer>> graph, int source, int target) {
        if (source == target) {
            throw new IllegalArgumentException("loops are forbidden");
        }
        try (ExternalMapQueryContext<Integer, Set<Integer>, ?> sc = graph.queryContext(source)) {
            sc.updateLock().lock();
            MapEntry<Integer, Set<Integer>> sEntry = sc.entry();
            if (sEntry != null) {
                Set<Integer> sNeighbours = sEntry.value().get();
                if (sNeighbours.add(target)) {
                    sEntry.doReplaceValue(sc.wrapValueAsData(sNeighbours));
                }
            } else {
                Set<Integer> sNeighbours = new HashSet<>();
                sNeighbours.add(target);
                MapAbsentEntry<Integer, Set<Integer>> sAbsentEntry = sc.absentEntry();
                assert sAbsentEntry != null;
                sAbsentEntry.doInsert(sc.wrapValueAsData(sNeighbours));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private static boolean removeEdge(
            ChronicleMap<Integer, Set<Integer>> graph, int source, int target) {
        ExternalMapQueryContext<Integer, Set<Integer>, ?> sourceC = graph.queryContext(source);
        ExternalMapQueryContext<Integer, Set<Integer>, ?> targetC = graph.queryContext(target);
        // order for consistent lock acquisition => avoid dead lock
        if (sourceC.segmentIndex() <= targetC.segmentIndex()) {
            return innerRemoveEdge(source, sourceC, target, targetC);
        } else {
            return innerRemoveEdge(target, targetC, source, sourceC);
        }
    }

    private static boolean innerRemoveEdge(
            int source, ExternalMapQueryContext<Integer, Set<Integer>, ?> sourceContext,
            int target, ExternalMapQueryContext<Integer, Set<Integer>, ?> targetContext) {
        try (ExternalMapQueryContext<Integer, Set<Integer>, ?> sc = sourceContext) {
            try (ExternalMapQueryContext<Integer, Set<Integer>, ?> tc = targetContext) {
                sc.updateLock().lock();
                MapEntry<Integer, Set<Integer>> sEntry = sc.entry();
                if (sEntry == null)
                    return false;
                Set<Integer> sNeighbours = sEntry.value().get();
                if (!sNeighbours.remove(target))
                    return false;

                tc.updateLock().lock();
                MapEntry<Integer, Set<Integer>> tEntry = tc.entry();
                if (tEntry == null)
                    throw new IllegalStateException("target node should be present in the graph");
                Set<Integer> tNeighbours = tEntry.value().get();
                if (!tNeighbours.remove(source))
                    throw new IllegalStateException("the target node have an edge to the source");
                sEntry.doReplaceValue(sc.wrapValueAsData(sNeighbours));
                tEntry.doReplaceValue(tc.wrapValueAsData(tNeighbours));
                return true;
            }
        }
    }



}
