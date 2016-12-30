package com.maxdemarzi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
    public void shouldAddRelationshipBeforeItExists() {
        cg.addRelationship("FRIENDS", "one", "two", 3);
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));
    }

    @Test
    public void shouldAddRelationshipWithProperties() {
        cg.addRelationshipType("RATED", 10000, 100, 100);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("stars", 5);
        cg.addRelationship("RATED", "one", "two", properties);
        Object actual = cg.getRelationship("RATED", "one", "two");
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
        boolean created = cg.addNode("key");
        Assert.assertTrue(created);
        Assert.assertEquals("", cg.getNode("key"));
    }

    @Test
    public void shouldAddNodeWithProperties() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("name", "max");
        properties.put("email", "maxdemarzi@hotmail.com");
        boolean created = cg.addNode("max", properties);
        Assert.assertTrue(created);
        Assert.assertEquals(properties, cg.getNode("max"));
    }

    @Test
    public void shouldAddNodeWithSimpleProperty() {
        boolean created = cg.addNode("simple", 5);
        Assert.assertTrue(created);
        Assert.assertEquals(5, cg.getNode("simple"));
    }

    @Test
    public void shouldRemoveNode() {
        boolean result = cg.addNode("simple", 5);
        Assert.assertTrue(result);
        result = cg.removeNode("simple");
        Assert.assertTrue(result);
    }

    @Test
    public void shouldRemoveNodeRelationships() {
        cg.addNode("one");
        cg.addNode("two");
        cg.addNode("three");
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", "one", "two", 9);
        cg.addRelationship("FRIENDS", "three", "one", 10);

        boolean result = cg.removeNode("one");
        Assert.assertTrue(result);
        Assert.assertEquals(0, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(0, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));

        Assert.assertEquals(null, cg.getRelationship("FRIENDS", "one", "two"));
        Assert.assertEquals(null, cg.getRelationship("FRIENDS", "three", "one"));
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
        boolean created = cg.addNode("complex", properties);
        Assert.assertTrue(created);
        Assert.assertEquals(properties, cg.getNode("complex"));
    }

    @Test
    public void shouldGetNodeOutgoingRelationships() {
        cg.addNode("one");
        cg.addNode("two");
        cg.addNode("three");
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", "one", "two");
        cg.addRelationship("FRIENDS", "one", "three");
        Set<String> actual = cg.getOutgoingRelationships("FRIENDS", "one");
        Assert.assertEquals(new HashSet<String>() {{ add("two"); add("three");}}, actual);
    }

    @Test
    public void shouldGetNodeIncomingRelationships() {
        cg.addNode("one");
        cg.addNode("two");
        cg.addNode("three");
        cg.addRelationshipType("FRIENDS", 10000, 100, 100);
        cg.addRelationship("FRIENDS", "one", "two");
        cg.addRelationship("FRIENDS", "one", "three");
        Set<String> actual = cg.getIncomingRelationships("FRIENDS", "two");
        Assert.assertEquals(new HashSet<String>() {{ add("one"); }}, actual);
    }

}
