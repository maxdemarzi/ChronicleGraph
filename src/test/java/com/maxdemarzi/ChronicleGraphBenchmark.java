package com.maxdemarzi;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class ChronicleGraphBenchmark {

    private ChronicleGraph db;

    @Param({"1000000"})
    private int maxNodes;

    @Param({"10000000"})
    private int maxRels;

    @Param({"10000"})
    private int userCount;

    @Param({"200"})
    private int itemCount;

    @Param({"200"})
    private int likesCount;

   @Setup(Level.Invocation )
    public void prepare() throws IOException {
       db = new ChronicleGraph(maxNodes, maxRels);
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
            db.addNode();
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
            db.addNode(properties);
        }
        return user;
    }
}
