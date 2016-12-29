package com.maxdemarzi;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ExternalMapQueryContext;
import net.openhft.chronicle.map.MapAbsentEntry;
import net.openhft.chronicle.map.MapEntry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChronicleGraph {

    private static ChronicleMap<String, Object> nodes;
    private static ChronicleMap<String, Map<String, Object>> relationships;
    private static HashMap<String, ChronicleMap<String, Set<String>>> related = new HashMap<>();
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
        nodeProperties.put("two", "username10000");
        nodeProperties.put("three", "email@yahoo.com");
        nodeProperties.put("four", 50.55D);

        nodes = ChronicleMap
                .of(String.class, Object.class)
                .name("nodes")
                .entries(maxNodes)
                .averageKey("uno-dos-tres-cuatro")
                .averageValue(nodeProperties)
                .create();
    }


    public void addRelationshipType(String type, Integer maximum, Integer average_outgoing, Integer average_incoming) {
        HashSet<String> avgOutgoingValue = new HashSet<>();
        for (int i = 0; i < average_outgoing; i++) {
            avgOutgoingValue.add("some key" + i);
        }

        HashSet<String> avgIncomingValue = new HashSet<>();
        for (int i = 0; i < average_incoming; i++) {
            avgIncomingValue.add("some key" + i);
        }

        ChronicleMap<String, Set<String>> cmOut = ChronicleMap
                .of(String.class, (Class<Set<String>>) (Class) Set.class)
                .name(type+ "-out")
                .entries(maximum)
                .averageValue(avgOutgoingValue)
                .averageKey("one key - another key")
                .create();
        ChronicleMap<String, Set<String>> cmIn = ChronicleMap
                .of(String.class, (Class<Set<String>>) (Class) Set.class)
                .name(type+ "-in")
                .entries(maximum)
                .averageValue(avgIncomingValue)
                .averageKey("one key - another key")
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

    public String addNode () {
        return addNode(new HashMap<>());
    }

    public String addNode (Object properties) {
        Integer nodeId = nodeCounter.incrementAndGet();
        nodes.put(nodeId.toString(), properties);
        return nodeId.toString();
    }

    public Object getNode(String id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id);
        } else {
            return new HashMap<>();
        }
    }

    public String addRelationship (String type, String from, String to) {
        if(related.containsKey(type+"-out")) {
            addEdge(related.get(type + "-out"), from, to);
            addEdge(related.get(type + "-in"), to, from);
            return from + "-" + to + type;
        } else {
            // TODO: 12/29/16 Maybe create it with default values instead
            throw new IllegalStateException("Relationship Type: " + type + " should be present in the graph. Try addRelationshipType()");
        }
    }

    public String addRelationship (String type, String from, String to, HashMap<String, Object> properties) {
        relationships.put(from + "-" + to + type, properties);
        addEdge(related.get(type+"-out"), from, to);
        addEdge(related.get(type+"-in"), to, from);
        return  from + "-" + to + type;
    }

    public Map<String, Object> getRelationship(String type, String from, String to) {
        return relationships.get(from + "-" + to + type);
    }

    public String removeRelationship (String type, String from, String to) {
        removeEdge(related.get(type+"-out"), from, to);
        removeEdge(related.get(type+"-in"), to, from);
        relationships.remove(from + "-" + to + type);
        return  from + "-" + to + type;
    }

    public Set<String> getOutgoingRelationships(String type, String from) {
        return related.get(type+"-out").get(from);
    }

    public Set<String> getIncomingRelationships(String type, String to) {
        return related.get(type+"-in").get(to);
    }

    private static boolean addEdge(ChronicleMap<String, Set<String>> graph, String source, String target) {
        if (source == target) {
            throw new IllegalArgumentException("loops are forbidden");
        }
        try (ExternalMapQueryContext<String, Set<String>, ?> sc = graph.queryContext(source)) {
            sc.updateLock().lock();
            MapEntry<String, Set<String>> sEntry = sc.entry();
            if (sEntry != null) {
                Set<String> sNeighbours = sEntry.value().get();
                if (sNeighbours.add(target)) {
                    sEntry.doReplaceValue(sc.wrapValueAsData(sNeighbours));
                }
            } else {
                Set<String> sNeighbours = new HashSet<>();
                sNeighbours.add(target);
                MapAbsentEntry<String, Set<String>> sAbsentEntry = sc.absentEntry();
                assert sAbsentEntry != null;
                sAbsentEntry.doInsert(sc.wrapValueAsData(sNeighbours));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static boolean removeEdge(ChronicleMap<String, Set<String>> graph, String source, String target) {
        try (ExternalMapQueryContext<String, Set<String>, ?> sc = graph.queryContext(source)) {
            sc.updateLock().lock();
            MapEntry<String, Set<String>> sEntry = sc.entry();
            if (sEntry == null) {
                return false;
            }
            Set<String> sNeighbours = sEntry.value().get();
            if (!sNeighbours.remove(target)) {
                return false;
            }
            if (sNeighbours.isEmpty()) {
                sc.remove(sEntry);
            } else {
                sEntry.doReplaceValue(sc.wrapValueAsData(sNeighbours));
            }
            return true;
        }
    }

}
