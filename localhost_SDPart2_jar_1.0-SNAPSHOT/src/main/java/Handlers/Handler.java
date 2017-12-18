package Handlers;

import Grafo.Aresta;
import Grafo.Chord;
import Grafo.Finger;
import Grafo.KeyNotFound;
import Grafo.Node;
import Grafo.Thrift;
import Grafo.Vertice;
import io.atomix.copycat.server.StateMachine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class Handler extends StateMachine implements Thrift.Iface {

    private static final ConcurrentHashMap<Integer, Vertice> HashVertice = new ConcurrentHashMap<Integer, Vertice>();
    private static Node node, nodeRaiz;
    private static int numBits = 7;

    public Handler(String args[]) throws TException {
        /* Ex. da sequência de argumentos:
            IP Local | Porta Local | IP Raíz | Porta Raíz
            1ª Vez: localhost 4000 localhost 4000 (IP/Porta Iguais para upar o nó raíz)
            2ª+ Vez: localhost 4001 localhost 4000 (Nó que quer entrar tem que ter porta diferente)
         */

        int port = Integer.parseInt(args[1]); //Porta do nó local (Que quer entrar no chord)
        int nodeRaizPort = Integer.parseInt(args[3]); //Porta do nó nodeRaiz

        node = new Node();
        node.setFt(new ArrayList<Finger>());
        node.setIp(args[0]); // IP Local
        node.setPort(port); // Porta Local

        // Se o IP/Porta "Raiz" for igual ao IP e porta Local estabelece o nó raíz
        if (args[2].equals(node.getIp()) && (port == nodeRaizPort)) {
            randomID(node);
            join(node);
            System.out.println("# Nó RAIZ estabelecido: \n"
                    + "*ID: " + node.getId()
                    + "\n*IP: " + node.getIp()
                    + "\n*Port: " + node.getPort() + "\n");
        } else {
            /* Quer dizer que já existe um nó no chord então irá iniciar uma comunicação com
            o nó raíz e vai atribuir esse nó raíz à variável nodeRaiz. Então o nó local, 
            ao setar seu ID primeiro deve verificar que não existe nenhum nó pertencente ao 
            chord com o mesmo ID, por meio da função VerifyID.
             */
            TTransport transport = new TSocket(args[2], nodeRaizPort);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Chord.Client client = new Chord.Client(protocol);
            nodeRaiz = client.sendSelf();
            transport.close();
            node.setId(verifyID(nodeRaiz));
            join(nodeRaiz);
            System.out.println("# Nó local estabelecido no Chord: \n"
                    + "*ID: " + node.getId()
                    + "\n*IP: " + node.getIp()
                    + "\n*Port: " + node.getPort() + "\n");
        }

        /* Variável de thread usada nas funções fixFingers e Stabilize
        pra permitir que os nós entrem a qualquer momento ao chord. [Artigo]
        É primeiramente criado um serviço que irá executar as threads com numBits = 5 Threads. 
        O scheduleWithFixedDelay permite que as threads sejam reexecutadas após o tempo demarcado,
        porém, existe também um tempo de delay até que a próxima thread inicie após o fim da primeira. 
         */
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(numBits);

        ScheduledFuture scheduledFutureStabilize = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                System.out.println("\n-> Executando estabilização do Chord...");
                stabilize();
            } catch (TException ex) {
                System.out.println("\n-> Erro ao iniciar Thread de estabilização do Chord.");
                ex.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);

        ScheduledFuture scheduledFutureFixFingers = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                System.out.println("\n-> Executando atualização das FT's...");
                fixFingers();
            } catch (TException ex) {
                System.out.println("\n-> Erro ao iniciar Thread para atualizar as Finger Tables");
                ex.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public boolean addVertice(Vertice v) throws TException {
        // Nó que define onde será colocado o vértice a partir do resto da operação
        Node aux = getSucessor(v.getNome() % (int) Math.pow(2, numBits));

        // Se o nó ID do nó local for o mesmo ID do vértice auxiliar então já insere, senão 
        // abre uma nova conexão com o nó onde deve ser inserido o vértice e envia pra os dados pra ele. 
        if (node.getId() == aux.getId()) {

            if (Handler.HashVertice.putIfAbsent(v.nome, v) == null) {
                v.setIdNode(aux.getId());
                return true;
            } else {
                return false;
            }

        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            // Verifica o resultado da função recursiva e retorna o valor para o cliente
            if (client.addVertice(v)) {
                transport.close();
                return true;
            } else {
                transport.close();
                return false;
            }
        }
    }

    @Override
    public Vertice readVertice(int nome) throws TException, KeyNotFound {
        // Verifica onde o nó poderá estar se ele existir
        Node aux = getSucessor(nome % (int) Math.pow(2, numBits));

        Vertice v = null;

        if (aux.getId() == node.getId()) {

            v = HashVertice.computeIfPresent(nome, (a, b) -> {
                return b;
            });

        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);
            v = client.readVertice(nome);
            transport.close();
        }

        if (v != null) {
            return v;
        }

        throw new KeyNotFound();

    }

    @Override
    public boolean updateVertice(Vertice v) throws KeyNotFound, TException {
        // Verifica onde o nó poderá estar se ele existir
        Node aux = getSucessor(v.getNome() % (int) Math.pow(2, numBits));

        if (aux.getId() == node.getId()) {
            try {
                Vertice vertice = readVertice(v.getNome());

                synchronized (vertice) {
                    vertice.setCor(v.getCor());
                    vertice.setDescricao(v.getDescricao());
                    vertice.setPeso(v.getPeso());
                    return true;
                }

            } catch (KeyNotFound e) {
                return false;
            }
        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            // Verifica o resultado da função recursiva e retorna o valor para o cliente
            if (client.updateVertice(v)) {
                transport.close();
                return true;
            } else {
                transport.close();
                return false;
            }
        }
    }

    @Override
    public boolean deleteVertice(Vertice v) throws KeyNotFound, TException {
        List<Vertice> Vertices = new ArrayList<>();
        Aresta a;

        Node aux = getSucessor(v.getNome() % (int) Math.pow(2, numBits));

        if (aux.getId() == node.getId()) {

            synchronized (v) {
                for (Integer key : v.HashAresta.keySet()) {
                    a = this.readAresta(v.HashAresta.get(key).getV1(), v.HashAresta.get(key).getV2());
                    this.deleteAresta(a);
                }
            }

            Vertices = this.readAllVertice();

            for (Vertice vertice : Vertices) {
                for (Integer keyAresta : vertice.HashAresta.keySet()) {
                    if (vertice.HashAresta.get(keyAresta).getV2() == v.getNome()) {
                        this.deleteAresta(vertice.HashAresta.get(keyAresta));
                    }
                }
            }

            if (Handler.HashVertice.remove(v.getNome()) != null) {
                return true;
            } else {
                return false;
            }
        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            // Verifica o resultado da função recursiva e retorna o valor para o cliente
            if (client.deleteVertice(v)) {
                transport.close();
                return true;
            } else {
                transport.close();
                return false;
            }
        }
    }

    @Override
    public List<Vertice> readVerticeNode() throws TException {
        ArrayList<Vertice> Vertices = new ArrayList<>();

        for (Integer key : HashVertice.keySet()) {
            Vertices.add(this.readVertice(key));
        }

        return Vertices;
    }

    @Override
    public List<Vertice> readAllVertice() throws TException {
        ArrayList<Vertice> Vertices = new ArrayList<>();

        Vertices.addAll(readVerticeNode());

        Node aux = getSucessor(node.getId() + 1);
        TTransport transport = null;

        while (aux.getId() != node.getId()) {
            transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            Vertices.addAll(client.readVerticeNode());
            transport.close();
            aux = getSucessor(aux.getId() + 1);
        }

        return Vertices;
    }

    @Override
    public List<Vertice> readVerticeNeighboors(Vertice v) throws TException {
        ArrayList<Vertice> Vertices = new ArrayList<>();

        for (Integer key : v.HashAresta.keySet()) {
            Vertices.add(this.readVertice(v.HashAresta.get(key).v2));
        }

        return Vertices;
    }

    @Override
    public boolean addAresta(Aresta a) throws TException {
        Vertice v;
        v = this.readVertice(a.getV1());

        Node aux = getSucessor(v.getNome() % (int) Math.pow(2, numBits));

        if (aux.getId() == node.getId()) {

            if (v.HashAresta.putIfAbsent(a.getV2(), a) == null) {
                return true;
            } else {
                return false;
            }
        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            if (client.addAresta(a)) {
                transport.close();
                return true;
            } else {
                transport.close();
                return false;
            }
        }
    }

    @Override
    public Aresta readAresta(int nomeV1, int nomeV2) throws TException {

        Vertice vertice;
        vertice = this.readVertice(nomeV1);

        Aresta aresta;
        aresta = vertice.HashAresta.computeIfPresent(nomeV2, (a, b) -> {
            return b;
        });

        if (aresta != null) {
            return aresta;
        }
        
        throw new KeyNotFound();

    }

    @Override
    public List<Aresta> readArestaNode() throws TException {
        ArrayList<Aresta> Arestas = new ArrayList<>();

        for (Integer keyVertice : HashVertice.keySet()) {
            synchronized (keyVertice) {
                for (Integer keyAresta : HashVertice.get(keyVertice).HashAresta.keySet()) {
                    Arestas.add(HashVertice.get(keyVertice).HashAresta.get(keyAresta));
                }
            }
        }

        return Arestas;
    }

    @Override
    public List<Aresta> readAllAresta() throws TException {

        ArrayList<Aresta> Arestas = new ArrayList<>();

        Arestas.addAll(readArestaNode());

        Node aux = getSucessor(node.getId() + 1);
        TTransport transport = null;

        while (aux.getId() != node.getId()) {
            transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            Arestas.addAll(client.readArestaNode());
            transport.close();
            aux = getSucessor(aux.getId() + 1);
        }

        return Arestas;
    }

    @Override
    public List<Aresta> readAllArestaOfVertice(Vertice v) throws TException {
        ArrayList<Aresta> Arestas = new ArrayList<>();
        Vertice vertice;

        for (Integer key : v.HashAresta.keySet()) {
            vertice = this.readVertice(v.HashAresta.get(key).getV2());
            Arestas.add(this.readAresta(v.getNome(), vertice.getNome()));
        }

        return Arestas;
    }

    @Override
    public boolean updateAresta(Aresta a) throws KeyNotFound, TException {

        Node aux = getSucessor(a.getV1() % (int) Math.pow(2, numBits));

        if (aux.getId() == node.getId()) {
            try {
                Aresta aresta = this.readAresta(a.v1, a.v2);

                synchronized (aresta) {
                    aresta.setDescricao(a.descricao);
                    aresta.setDirect(a.isDirect());
                    aresta.setPeso(a.getPeso());
                    return true;
                }

            } catch (KeyNotFound e) {
                return false;
            }
        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            // Verifica o resultado da função recursiva e retorna o valor para o cliente
            if (client.updateAresta(a)) {
                transport.close();
                return true;
            } else {
                transport.close();
                return false;
            }
        }

    }

    @Override
    public boolean deleteAresta(Aresta a) throws KeyNotFound, TException {
        Node aux = getSucessor(a.getV1() % (int) Math.pow(2, numBits));

        if (aux.getId() == node.getId()) {

            synchronized (a) {
                Vertice v1 = this.readVertice(a.getV1());
                Vertice v2 = this.readVertice(a.getV2());

                v1.HashAresta.remove(v2.getNome());
                v2.HashAresta.remove(v1.getNome());

                return true;
            }
        } else {
            TTransport transport = new TSocket(aux.getIp(), aux.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Thrift.Client client = new Thrift.Client(protocol);

            // Verifica o resultado da função recursiva e retorna o valor para o cliente
            if (client.deleteAresta(a)) {
                transport.close();
                return true;
            } else {
                transport.close();
                return false;
            }
        }
    }

    @Override
    public void join(Node raiz) throws TException {
        // Seta o predecessor para nulo
        node.setPred(null);

        // Criação da FINGER TABLE e atribuição do mesmo nó para todos os campos
        for (int i = 1; i <= numBits; i++) {
            Finger aux = new Finger();
            aux.setId(node.getId());
            aux.setIp(node.getIp());
            aux.setPort(node.getPort());
            node.getFt().add(aux);
        }

        /* Se o nó for o nó raiz, não faz nada. 
            Caso seja outro nó que deseja entrar no chord, então ele pede para o nó raiz
        qual é os dados que ele possui de sucessor e predecessor e então atualiza seus 
        campos da ft.
         */
        if (node.getId() != raiz.getId()) {
            TTransport transport = new TSocket(raiz.getIp(), raiz.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Chord.Client client = new Chord.Client(protocol);
            Node nodeAux = client.getSucessor(node.getId());
            transport.close();

            // Atualiza o primeiro valor da tabela FT do nó local com os dados do nó sucessor 
            synchronized (node.getFt().get(0)) {
                node.getFt().get(0).setId(nodeAux.getId());
                node.getFt().get(0).setIp(nodeAux.getIp());
                node.getFt().get(0).setPort(nodeAux.getPort());
            }
        }
    }

    @Override
    public Node getSucessor(int id) throws TException {
        // Para encontrar o sucessor, primeiro ele deve procurar pelo predecessor. 
        Node aux = getPredecessor(id);

        if (aux.getFt().get(0).getId() == node.getId()) {
            System.out.println("# Sucessor " + node.getId() + ", encontrado para ID:" + id);
            return node;
        } else {
            TTransport transport = new TSocket(aux.getFt().get(0).getIp(), aux.getFt().get(0).getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Chord.Client client = new Chord.Client(protocol);

            System.out.println(aux.getId());
            System.out.println(aux.getFt().get(0).getId());
            aux = client.sendSelf();
            transport.close();
            System.out.println("Else = Sucessor ID: " + aux.getId() + " encontrado para ID:" + id);
            return aux;
        }
    }

    @Override
    public Node getPredecessor(int id) throws TException {
        System.out.println("\n-> Procurando predecessor para ID: " + id);

        // Aux recebe o nó local
        Node aux = node;

        /* Entra em loop com a função de verificação de intervalo. 
        Caso encontre um predecessor é passado para a função closest encontrar o 
        predecessor mais próximo, pois há casos em em que mesmo encontrando o predecessor
        não é o predecessor que deve ser usado.
         */
        while (!interval(id, aux.getId(), true, aux.getFt().get(0).getId(), false)) {
            if (aux != node) {
                TTransport transport = new TSocket(aux.getIp(), aux.getPort());
                transport.open();
                TProtocol protocol = new TBinaryProtocol(transport);
                Chord.Client client = new Chord.Client(protocol);
                aux = client.closestPrecedingFinger(id);
                transport.close();
            } else {
                aux = closestPrecedingFinger(id);
            }
        }
        System.out.println("-> Predecessor " + node.getId() + ", encontrado para ID:" + id);
        return aux;
    }

    @Override
    public Node closestPrecedingFinger(int id) throws TException {

        for (int i = numBits - 1; i >= 0; i--) {
            System.out.println("Procurando Finger Predecessor mais próximo para ID: "
                    + id + " na tabela de ID:" + node.getId() + " Entrada(" + i + ")->"
                    + node.getFt().get(i).getId());

            if (interval(node.getFt().get(i).getId(), node.getId(), true, id, true)) {
                if (node.getId() != node.getFt().get(i).getId()) {
                    Finger finger = node.getFt().get(i);
                    TTransport transport = new TSocket(finger.getIp(), finger.getPort());
                    transport.open();
                    TProtocol protocol = new TBinaryProtocol(transport);
                    Chord.Client client = new Chord.Client(protocol);
                    Node aux = client.sendSelf();
                    transport.close();
                    return aux;
                } else {
                    return node;
                }
            }
        }
        return node;
    }

    @Override
    public void stabilize() throws TException {

        /* Método serve para pegar o nó local n, perguntar qual é o sucessor dele,
        suponhamos que seja ns e a partir desse sucessor setar no próprio sucessor
        o predecessor como sendo n e no nó local n setar o sucessor como sendo ns
        Como a thread será executada novamente np que era o predecessor e sucessor de ns
        passa a perguntar para ns qual é o predecessor dele, que no caso é n
        e então atualiza a sua ft para sucessor n e no local n atualiza o predeessor
        de null para np*/
        Finger fingerAux;
        Node nodeAux = null;
        TTransport transport = null;
        TProtocol protocol = null;
        Chord.Client client = null;
        fingerAux = node.getFt().get(0); // Pega o primeiro campo da FT do nó local [sucessor]

        if (fingerAux.getId() != node.getId()) {
            transport = new TSocket(fingerAux.getIp(), fingerAux.getPort());
            transport.open();
            protocol = new TBinaryProtocol(transport);
            client = new Chord.Client(protocol);
            nodeAux = client.sendSelf(); // Envia os dados do sucessor para o nó auxiliar
            transport.close();
        } else {
            nodeAux = node; // Acontece caso o sucessor do nó for o próprio nó.
        }

        fingerAux = nodeAux.getPred(); // Recebe o finger do predecessor [IP/port/id] 

        if (fingerAux != null && interval(fingerAux.getId(), node.getId(), true, node.getFt().get(0).getId(), true)) {
            Finger aux = new Finger();
            aux.setId(fingerAux.getId());
            aux.setIp(fingerAux.getIp());
            aux.setPort(fingerAux.getPort());
            printTable();
            // Altera o sucessor na FT. 
            node.getFt().set(0, aux);
            printTable(); // Imprime a FT do nó
        }

        if (node.getFt().get(0).getId() != node.getId()) {
            transport = new TSocket(node.getFt().get(0).getIp(), node.getFt().get(0).getPort());
            transport.open();
            protocol = new TBinaryProtocol(transport);
            client = new Chord.Client(protocol);
            client.notify(node);
            transport.close();
        }
        System.out.println("-> Estabilização do chord concluída.");
        printTable();
    }

    @Override
    public void notify(Node n) throws TException {
        /* Esse método notifica o nó sucessor que a partir daquele momento ele será 
        o predecessor do nó sucessor.
         */

        if (node.getPred() == null || interval(n.getId(), node.getPred().getId(), true, node.getId(), true)) {
            Finger aux = new Finger();
            aux.setId(n.getId());
            aux.setIp(n.getIp());
            aux.setPort(n.getPort());
            node.setPred(aux);
            System.out.println("\n-> O predecessor de:" + node.getId() + " agora é: " + n.getId());
        } else {
            System.out.println("\n-> Não houveram alterações de predecessor");
        }
    }

    @Override
    public void fixFingers() throws TException {
        // Atualiza a finger table do nó local exceto o sucessor

        System.out.println("\n-> Corrigindo a Finger Table dos nós...");

        System.out.println("\n ######## Tabela Atual #######");
        printTable();

        for (int i = 1; i < numBits; i++) {
            Node aux = getSucessor((node.getId() + (int) Math.pow(2, i)) % (int) Math.pow(2, numBits));
            Finger finger = new Finger();
            finger.setId(aux.getId());
            finger.setIp(aux.getIp());
            finger.setPort(aux.getPort());
            node.getFt().set(i, finger);
        }

        System.out.println("\n ######## Tabela Após correção #######");
        printTable();
    }

    @Override
    public Node sendSelf() throws TException {
        // Função que serve para pedir informações de um servidor remoto. 
        return node;
    }

    @Override
    public void setPredecessor(Node n) throws TException {
        synchronized (node.getPred()) {
            node.getPred().setId(n.getId());
            node.getPred().setIp(n.getIp());
            node.getPred().setPort(n.getPort());
        }
    }

    @Override
    public int verifyID(Node n) throws TException {
        int trueID = -1;

        System.out.println("\nGerando ID... \n");

        while (1 == 1) {
            trueID = randomID(node).getId();
            // Abre a conexão com os dados locais do nó e verifica no sucessor se o ID pode ser usado.
            TTransport transport = new TSocket(n.getIp(), n.getPort());
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            Chord.Client client = new Chord.Client(protocol);

            Node aux = client.getSucessor(trueID); // Pergunta qual é o ID do sucessor

            int idAux = aux.getId(); // Variável de verificação de ID

            /* Se retornar um ID do sucessor diferente do perguntado 
            ao chord quer dizer que esse ID perguntado pode ser atribuído. 
            Senão continua o loop até encontrar um ID factível. 
            Ex.: Gerou o ID 5 e ao perguntar o sucessor, retornou o 9, 
            logo o 5 pode ser atribuído ao nó local. Caso retornasse o ID 5, 
            teria que gerar outro ID sem ser o 5.*/
            if (idAux != trueID) {
                System.out.println("ID gerado!");
                break;
            } else {
                System.out.println("ID já atribuído.");
            }
        }

        return trueID;

    }

    @Override
    public Node randomID(Node node) {
        // Função destinada a geração de um número aleatório dentro do intervalo pre-definido
        int a = (int) (Math.random() * Math.pow(2, numBits));
        node.setId(a);

        return node;
    }

    @Override
    public void printTable() {
        for (int i = 0; i < numBits; i++) {

            Finger aux = node.getFt().get(i);

            System.out.println("(" + i + ") |" + (node.getId() + (int) Math.pow(2, i)) % (int) Math.pow(2, numBits)
                    + "| -------> ID:" + aux.getId()
                    + " IP:" + aux.getIp()
                    + " PORT:" + aux.getPort());
        }
    }

    @Override
    public int getIDLocal() throws TException {
        return node.getId();
    }

    public static boolean interval(int x, int a, boolean flagOpenA, int b, boolean flagOpenB) {
        //Verifica se x está no intervalo de valores "a" e "b".
        //As flags informam se o intervalo é aberto ou não. 

        if (a == b) {
            return !((flagOpenA && flagOpenB) && x == a);
        } else {
            if ((!flagOpenA && x == a) || (!flagOpenB && x == b)) {
                return true;
            }

            if (a < b) {
                return ((x > a) && (x < b));
            } else {
                return ((x > a) || (x >= 0 && x < b));
            }
        }

    }

    @Override
    public int procuraMenorDistancia(Map<Integer, Double> dist, Map<Integer, Integer> visitado, List<Vertice> vertices) {
        int i, menor = -1;
        boolean primeiro = true;

        for (Vertice v : vertices) {
            if (dist.get(v.getNome()) >= 0 && visitado.get(v.getNome()) == 0) {
                if (primeiro) {
                    menor = v.getNome();
                    primeiro = false;
                } else {
                    if (dist.get(menor) > dist.get(v.getNome())) {
                        menor = v.getNome();
                    }
                }
            }
        }
        return menor;
    }

    @Override
    public List<Vertice> menorCaminho(int ini, int fim, Map<Integer, Integer> ant, Map<Integer, Double> dist) throws TException {
        int i, cont, NV, ind, u;

        List<Vertice> vertices = readAllVertice();
        HashMap<Integer, Vertice> verticesG = new HashMap<>();
        HashMap<Integer, Integer> visitado = new HashMap<>();

        cont = NV = vertices.size();

        for (Vertice v : vertices) {
            verticesG.put(v.getNome(), v);
            visitado.put(v.getNome(), 0);
            ant.put(v.getNome(), -1);
            dist.put(v.getNome(), -1.0);
        }

        dist.replace(ini, 0.0);

        while (cont > 0) {
            u = procuraMenorDistancia(dist, visitado, vertices);
            if (u == -1) {
                break;
            }

            Vertice v = readVertice(u);
            visitado.replace(u, 1);
            cont--;

            List<Vertice> list = readVerticeNeighboors(v);
            
            
            if (list == null){
                return null;
            }

            for (i = 0; i < list.size(); i++) {
                ind = list.get(i).getNome();

                Aresta ar = readAresta(u, ind);
                if (dist.get(ind) < 0) {
                    dist.replace(ind, dist.get(u) + ar.getPeso());
                    ant.replace(ind, u);
                } else {
                    if (dist.get(ind) > dist.get(u) + ar.getPeso()) {
                        dist.replace(ind, dist.get(u) + ar.getPeso());
                        ant.replace(ind, u);
                    }
                }
            }
        }

        List<Vertice> resp = new ArrayList<>();
        int v = fim;
        while (v != ini) {
            resp.add(verticesG.get(v));
            v = ant.get(v);
            if (v == ini) {
                resp.add(verticesG.get(v));
            }
        }

        return resp;
    }
    
    public void raftClientConnect(){
        
    }

}
