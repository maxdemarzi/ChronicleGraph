package com.maxdemarzi;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ExternalMapQueryContext;
import net.openhft.chronicle.map.MapAbsentEntry;
import net.openhft.chronicle.map.MapEntry;

import java.util.*;

public class ChronicleGraph {

    Integer DEFAULT_MAXIMUM_RELATIONSHIPS = 10_000_000;
    Integer DEFAULT_OUTGOING = 100;
    Integer DEFAULT_INCOMING = 100;

    private static ChronicleMap<String, Object> nodes;
    private static ChronicleMap<String, Object> relationships;
    private static HashMap<String, ChronicleMap<String, Set<String>>> related = new HashMap<>();

    public ChronicleGraph(Integer maxNodes, Integer maxRelationships) {
        HashMap<String, Object> relProperties = new HashMap<>();
        relProperties.put("one", 10000);

        relationships = ChronicleMap
                .of(String.class, Object.class)
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

    public boolean addNode (String key) {
        return addNode(key,"");
    }

    public boolean addNode (String key, Object properties) {
        nodes.put(key, properties);
        return true;
    }

    public Object getNode(String id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id);
        } else {
            return new HashMap<>();
        }
    }

    public boolean removeNode(String id) {
        nodes.remove(id);

        for (Map.Entry<String, ChronicleMap<String, Set<String>>> entry : related.entrySet()) {
            ChronicleMap<String, Set<String>> cm = entry.getValue();
            if (entry.getKey().endsWith("-out")) {
                if(cm.containsKey(id)) {
                    ChronicleMap<String, Set<String>> reversecm = related.get(entry.getKey().replace("-out", "-in"));
                    for (String other : cm.get(id)) {
                        removeEdge(reversecm, other, id);
                        relationships.remove(id + "-" + other + entry.getKey().replace("-out",""));
                    }
                    cm.remove(id);
                }
            } else {
                if(cm.containsKey(id)) {
                    ChronicleMap<String, Set<String>> reversecm = related.get(entry.getKey().replace("-in", "-out"));
                    for (String other : cm.get(id)) {
                        removeEdge(reversecm, other, id);
                        relationships.remove(other + "-" + id + entry.getKey().replace("-in",""));
                    }
                    cm.remove(id);
                }
            }
        }
        return true;
    }

    public boolean addRelationship (String type, String from, String to) {
        if(!related.containsKey(type+"-out")) {
            addRelationshipType(type, DEFAULT_MAXIMUM_RELATIONSHIPS, DEFAULT_OUTGOING, DEFAULT_INCOMING);
        }
        addEdge(related.get(type + "-out"), from, to);
        addEdge(related.get(type + "-in"), to, from);

        return true;
    }

    public boolean addRelationship (String type, String from, String to, Object properties) {
        if(!related.containsKey(type+"-out")) {
            addRelationshipType(type, DEFAULT_MAXIMUM_RELATIONSHIPS, DEFAULT_OUTGOING, DEFAULT_INCOMING);
        }
        relationships.put(from + "-" + to + type, properties);
        addEdge(related.get(type+"-out"), from, to);
        addEdge(related.get(type+"-in"), to, from);
        return true;
    }

    public Object getRelationship(String type, String from, String to) {
        return relationships.get(from + "-" + to + type);
    }

    public boolean removeRelationship (String type, String from, String to) {
        if(!related.containsKey(type+"-out")) {
            return false;
        }
        removeEdge(related.get(type+"-out"), from, to);
        removeEdge(related.get(type+"-in"), to, from);
        relationships.remove(from + "-" + to + type);
        return true;
    }

    public Set<String> getOutgoingRelationshipNodeIds(String type, String from) {
        return related.get(type+"-out").get(from);
    }

    public Set<String> getIncomingRelationshipNodeIds(String type, String to) {
        return related.get(type+"-in").get(to);
    }

    public Set<Object> getOutgoingRelationshipNodes(String type, String from) {
        Set<Object> results = new HashSet<>();
        for (String key : related.get(type+"-out").get(from) ) {
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("_id", key);
            properties.put("properties", nodes.get(key));
            results.add(properties);
        }
        return results;
    }

    public Set<Object> getIncomingRelationshipNodes(String type, String from) {
        Set<Object> results = new HashSet<>();
        for (String key : related.get(type+"-in").get(from) ) {
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("_id", key);
            properties.put("properties", nodes.get(key));
            results.add(properties);
        }
        return results;
    }

    private static boolean addEdge(ChronicleMap<String, Set<String>> graph, String source, String target) {
        if (source.equals(target)) {
            return false;
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
        } catch (Exception e) {
            return false;
        }
    }

}
