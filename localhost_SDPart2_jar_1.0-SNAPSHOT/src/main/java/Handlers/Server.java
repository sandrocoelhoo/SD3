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
import org.apache.thrift.server.TThreadPoolServer.Args;

public class Server extends StateMachine {

    public static Handler handler;
    public static Grafo.Thrift.Processor processor;

    public static void main(String[] args) throws InterruptedException {

        String ipLocal = args[0],
                ipRaiz = args[2],
                ipRaft = args[4],
                ipRaftRaiz = args[6];
        int portaLocal = Integer.parseInt(args[1]),
                portaRaft = Integer.parseInt(args[5]),
                portaRaftRaiz = Integer.parseInt(args[7]);

        try {
            handler = new Handler(args);
            processor = new Grafo.Thrift.Processor(handler);
            Thread servidor = null;

            TServerTransport servertransport = new TServerSocket(portaLocal);
            TThreadPoolServer server = new TThreadPoolServer(new Args(servertransport).processor(processor));

            Runnable simple = () -> {
                simple(server);
            };

            System.out.println("Inicializando Servidor.....");
            servidor = new Thread(simple);
            servidor.start();
            System.out.println("Thrift Server Started on: ");
            Address address = new Address(ipRaft, portaRaft);
            CopycatServer builderCopy = CopycatServer.builder(address)
                    .withStateMachine(() -> {
                        return handler;
                    })
                    .withTransport(NettyTransport.builder()
                            .withThreads(4)
                            .build())
                    .withStorage(Storage.builder()
                            .withDirectory(new File("logs"))
                            .withStorageLevel(StorageLevel.DISK)
                            .build())
                    .build();

            if ((ipLocal == null ? ipRaiz == null : ipLocal.equals(ipRaiz)) && portaRaft == portaRaftRaiz && ipLocal.equals(ipRaftRaiz)) {
                System.out.println("Root node for Raft, starting cluster...");
                CompletableFuture<CopycatServer> future = builderCopy.bootstrap();
                future.join();
                System.out.println("Raft Cluster Started!");
            } else {                                                                                            //Nó Comum(Raft)
                System.out.println("Common Raft Node, connecting to cluster...");
                Collection<Address> clusterRaft = Collections.singleton(new Address(ipRaftRaiz, portaRaftRaiz));
                builderCopy.join(clusterRaft).join();
                System.out.println("Connected to Raft Cluster!");
            }
            handler.raftClientConnect();
            System.out.println("Servidor Inicializado");
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
