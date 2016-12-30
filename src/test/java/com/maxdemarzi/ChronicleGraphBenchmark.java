package com.maxdemarzi;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ChronicleGraphBenchmark {

    private ChronicleGraph db;
    private Random rand = new Random();

    @Param({"1000000"})
    private int maxNodes;

    @Param({"10000000"})
    private int maxRels;

    @Param({"1000"})
    private int userCount;

    @Param({"1000"})
    private int personCount;

    @Param({"200"})
    private int itemCount;

    @Param({"100"})
    private int friendsCount;

    @Param({"200"})
    private int likesCount;

   @Setup(Level.Invocation )
    public void prepare() throws IOException {
       db = new ChronicleGraph(maxNodes, maxRels);
       db.addRelationshipType("FRIENDS", maxRels, 100, 100);
       db.addRelationshipType("LIKES", maxRels, 100, 100);
    }

    @Benchmark
    @Warmup(iterations = 10)
    @Measurement(iterations = 10)
    @Fork(1)
    @Threads(4)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int measureCreateEmptyNodes() throws IOException {
        int user;
        for (user = 0; user < userCount; user++) {
            db.addNode("user" + user);
        }
        return user;
    }

    @Benchmark
    @Warmup(iterations = 10)
    @Measurement(iterations = 10)
    @Fork(1)
    @Threads(4)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int measureCreateNodesWithProperties() throws IOException {
        int user;
        for (user = 0; user < userCount; user++) {
            HashMap<String, Object> properties = new HashMap<>();
            properties.put("id", user);
            properties.put("username", "username" + user );
            db.addNode("user" +user, properties);
        }
        return user;
    }

    @Benchmark
    @Warmup(iterations = 10)
    @Measurement(iterations = 10)
    @Fork(1)
    @Threads(1)
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public int measureCreateEmptyNodesAndRelationships() throws IOException {
        int user;
        for (user = 0; user < userCount; user++) {
            db.addNode("user" + user);
        }
        for (user = 0; user < userCount; user++) {
            for (int like = 0; like < friendsCount; like++) {
                db.addRelationship("FRIENDS", "user" + user, "user" + rand.nextInt(userCount));
            }
        }
        return user;
    }
}
