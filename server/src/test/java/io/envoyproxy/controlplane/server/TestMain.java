package io.envoyproxy.controlplane.server;

import com.google.common.collect.ImmutableList;
import io.envoyproxy.controlplane.cache.TestResources;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;

public class TestMain {

  private static final String GROUP = "key";

  /**
   * Example minimal xDS implementation using the java-control-plane lib. This example configures a
   * DiscoveryServer with a v3 cache, and handles only v3 requests from data planes.
   *
   * @param arg command-line args
   */
  public static void main(String[] arg) throws IOException, InterruptedException {
    SimpleCache<String> cache = new SimpleCache<>(node -> GROUP);

    cache.setSnapshot(
        GROUP,
        Snapshot.create(
            ImmutableList.of(
                TestResources.createCluster("httpbin")),

            ImmutableList.of(
                TestResources.createCLA("httpbin", "34.194.112.169", 80, 0)
            ),
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of(),
            "1"));

    V3DiscoveryServer v3DiscoveryServer = new V3DiscoveryServer(cache);

    ServerBuilder builder =
        NettyServerBuilder.forPort(12345)
            .addService(v3DiscoveryServer.getAggregatedDiscoveryServiceImpl())
            .addService(v3DiscoveryServer.getClusterDiscoveryServiceImpl())
            .addService(v3DiscoveryServer.getEndpointDiscoveryServiceImpl())
            .addService(v3DiscoveryServer.getListenerDiscoveryServiceImpl())
            .addService(v3DiscoveryServer.getRouteDiscoveryServiceImpl());

    Server server = builder.build();

    server.start();

    System.out.println("Server has started on port " + server.getPort());

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

    Thread.sleep(10000);


    for (int i = 1 ; i < 100 ; i ++) {

      System.out.println("deliverying config version " + i);


      cache.setSnapshot(
          GROUP,
          Snapshot.create(
              ImmutableList.of(
                  TestResources.createCluster("httpbin")),
              ImmutableList.of(
                  TestResources.createCLA("httpbin", "34.194.112.169", 80, i % 2)
              ),
              ImmutableList.of(),
              ImmutableList.of(),
              ImmutableList.of(),
              String.valueOf(i)));

      Thread.sleep(2000);

    }


    server.awaitTermination();
  }
}
