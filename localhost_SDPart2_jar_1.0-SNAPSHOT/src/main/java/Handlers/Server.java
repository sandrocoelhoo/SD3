package Handlers;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.apache.thrift.TException;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;

public class Server extends StateMachine{

    public static Handler handler;
    public static Grafo.Thrift.Processor processor;
    
    public static void main(String[] args) {
        
        int myId = Integer.parseInt(args[0]);
    	List<Address> addresses = new LinkedList<>();
    	
    	for(int i = 1; i <args.length; i+=2)
    	{
            Address address = new Address(args[i], Integer.parseInt(args[i+1]));
    		addresses.add(address);
    	}
    		
        CopycatServer.Builder builder = CopycatServer.builder(addresses.get(myId)).withStateMachine(Server::new)
                                                     .withTransport( NettyTransport.builder()
                                                                     .withThreads(4)
                                                                     .build())
                                                     .withStorage( Storage.builder()
                                                                   .withDirectory(new File("logs_"+myId)) //Must be unique
                                                                   .withStorageLevel(StorageLevel.DISK)
                                                                   .build());
        CopycatServer copycatServer = builder.build();

        Thread t = new Thread();
        
        if(myId == 0)
        {
            copycatServer.bootstrap().join();
        }
        else
        {
            copycatServer.join(addresses).join();
        }
        
        
        
        
        
        try {
            handler = new Handler(args);
            processor = new Grafo.Thrift.Processor(handler);

            TServerTransport servertransport = new TServerSocket(Integer.parseInt(args[1]));

            TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(servertransport).processor(processor));

            server.serve();

        } catch (TException x) {
            x.printStackTrace();
        }
    }
}
