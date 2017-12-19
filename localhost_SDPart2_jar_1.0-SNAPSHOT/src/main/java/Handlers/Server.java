package Handlers;

import java.io.File;

import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.apache.log4j.PropertyConfigurator;
import org.apache.thrift.server.TThreadPoolServer.Args;

public class Server extends StateMachine {

    public static Grafo.Thrift.Processor processor;

    public static void main(String[] args) throws InterruptedException {

        String ipLocal = args[0];
        int portaLocal = Integer.parseInt(args[1]);
        String ipRaiz = args[2];
        int portaRaiz = Integer.parseInt(args[3]);
        String ipRaftRaiz = args[4];
        int portaRaftRaiz = Integer.parseInt(args[5]);
        int portaRaft = Integer.parseInt(args[6]);

        Thread servidor = null;

        PropertyConfigurator.configure("source/log4j.properties");

        try {
            Handler handler = new Handler(args);
            processor = new Grafo.Thrift.Processor(handler);

            TServerTransport servertransport = new TServerSocket(portaLocal);
            TThreadPoolServer server = new TThreadPoolServer(new Args(servertransport).processor(processor));

            Runnable simple = () -> {
                simple(server);
            };

            servidor = new Thread(simple);
            servidor.start();

            Address address = new Address(ipLocal, portaRaft);
            CopycatServer builderCopy = CopycatServer.builder(address)
                    .withStateMachine(() -> {
                        return handler;
                    })
                    .withTransport(NettyTransport.builder()
                            .withThreads(4)
                            .build())
                    .withStorage(Storage.builder()
                            .withDirectory(new File("log"))
                            .withStorageLevel(StorageLevel.DISK)
                            .build())
                    .build();

            System.out.println("Endereço Thrift: " + ipLocal + ":" + portaRaiz);
            System.out.println("Endereço Copycat: " + ipLocal + ":" + portaRaft);

            if ((ipLocal == null ? ipRaiz == null : ipLocal.equals(ipRaiz)) && portaRaft == portaRaftRaiz && ipLocal.equals(ipRaftRaiz)) {
                CompletableFuture<CopycatServer> future = builderCopy.bootstrap();
                future.join();
                System.out.println("Cluster iniciado.");
            } else {
                Collection<Address> clusterRaft = Collections.singleton(new Address(ipRaftRaiz, portaRaftRaiz));
                builderCopy.join(clusterRaft).join();
                System.out.println("Nó conectado no cluster.");
            }
            handler.raftClientConnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void simple(TThreadPoolServer server) {
        try {
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
