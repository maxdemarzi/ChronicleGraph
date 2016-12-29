package com.maxdemarzi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        cg.addRelationship("FRIENDS", 0, 1);
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-out"));
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("FRIENDS").get("FRIENDS-in"));
    }

    @Test
    public void shouldAddRelationshipWithProperties() {
        cg.addRelationshipType("RATED", 10000, 100, 100);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("stars", 5);
        cg.addRelationship("RATED", 0, 1, properties);
        Map<String, Object> actual = cg.getRelationship("RATED", 0, 1);
        Assert.assertEquals(properties, actual);
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("RATED").get("RATED-out"));
        Assert.assertEquals(1, cg.getRelationshipTypeAttributes("RATED").get("RATED-in"));
    }

}
