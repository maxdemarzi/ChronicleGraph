package com.maxdemarzi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@State(Scope.Benchmark)
public class ChronicleGraphTest {
    public ChronicleGraph cg;

    @Before
    public void setup() throws IOException {
        cg = new ChronicleGraph(10000, 100000);
    }

    @Test
    public void shouldAddRelationshipType() {
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        Assert.assertEquals(0, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(0, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));

    }

    @Test
    public void shouldAddRelationship() {
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", "one", "two");
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));
    }

    @Test
    public void shouldAddRelationshipWithProperties() {
        cg.addRelationshipType("RATED", 10000, 100, 100);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("stars", 5);
        cg.addRelationship("RATED", "one", "two", properties);
        Map<String, Object> actual = cg.getRelationship("RATED", "one", "two");
        Assert.assertEquals(properties, actual);
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("RATED").get("RATED-out"));
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("RATED").get("RATED-in"));
    }

    @Test
    public void shouldRemoveRelationship() {
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", "one", "two");
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));
        cg.removeRelationship("FRIENDS", "one", "two");
        Assert.assertEquals(0, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(0, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));

    }

    @Test
    public void shouldAddNode() {
        String nodeId = cg.addNode();
        Assert.assertEquals(new HashMap<>(), cg.getNode(nodeId));
    }

    @Test
    public void shouldAddNodeWithProperties() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("name", "max");
        properties.put("email", "maxdemarzi@hotmail.com");
        String nodeId = cg.addNode(properties);
        Assert.assertEquals(properties, cg.getNode(nodeId));
    }

    @Test
    public void shouldAddNodeWithSimpleProperty() {
        String nodeId = cg.addNode(5);
        Assert.assertEquals(5, cg.getNode(nodeId));
    }

    @Test
    public void shouldAddNodeWithObjectProperties() {
        HashMap<String, Object> address = new HashMap<>();
        address.put("Country", "USA");
        address.put("Zip", "60601");
        address.put("State", "TX");
        address.put("City", "Chicago");
        address.put("Line1 ", "175 N. Harbor Dr.");
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("name", "max");
        properties.put("email", "maxdemarzi@hotmail.com");
        properties.put("address", address);
        String nodeId = cg.addNode(properties);
        Assert.assertEquals(properties, cg.getNode(nodeId));
    }

    @Test
    public void shouldGetNodeOutgoingRelationships() {
        String nodeOneId = cg.addNode();
        String nodeTwoId = cg.addNode();
        String nodeThreeId = cg.addNode();
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", nodeOneId, nodeTwoId);
        cg.addRelationship("FRIENDS", nodeOneId, nodeThreeId);
        Set<String> actual = cg.getOutgoingRelationships("FRIENDS", nodeOneId);
        Assert.assertEquals(new HashSet<String>() {{ add(nodeTwoId); add(nodeThreeId);}}, actual);
    }

    @Test
    public void shouldGetNodeIncomingRelationships() {
        String nodeOneId = cg.addNode();
        String nodeTwoId = cg.addNode();
        String nodeThreeId = cg.addNode();
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", nodeOneId, nodeTwoId);
        cg.addRelationship("FRIENDS", nodeOneId, nodeThreeId);
        Set<String> actual = cg.getIncomingRelationships("FRIENDS", nodeTwoId);
        Assert.assertEquals(new HashSet<String>() {{ add(nodeOneId); }}, actual);
    }

}
