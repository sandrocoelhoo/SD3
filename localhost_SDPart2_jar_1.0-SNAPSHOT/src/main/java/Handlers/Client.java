package Handlers;

import Grafo.Aresta;
import Grafo.KeyNotFound;
import Grafo.Thrift;
import Grafo.Vertice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class Client {

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        connection(args);
    }

    public static void connection(String[] args) {
        int opcao, nome, cor, v1, v2;
        String descricao;
        double peso;
        Vertice v = new Vertice();
        Aresta a = new Aresta();
        boolean direct;

        try {

            System.out.println("\n ===== CONEXÃO CLIENTE/SERVIDOR ===== \n");
            System.out.print("Conectando no servidor: " + args[0]);
            System.out.print("\nPorta: " + args[1]);

            if (args.length > 0) {

                // Recebe o endereço do servidor e porta para tentar conexão
                TTransport transport = new TSocket(args[0], Integer.parseInt(args[1]));
                transport.open();
                // Chama o protocolo de comunicação entre S/C
                TProtocol protocol = new TBinaryProtocol(transport);
                Thrift.Client client = new Thrift.Client(protocol);

                do {
                    System.out.println("\n =====> MENU PRINCIPAL: GRAFO <===== \n");
                    System.out.println("1  - Adicionar vertice");
                    System.out.println("2  - Atualizar vertice");
                    System.out.println("3  - Deletar vertice");
                    System.out.println("4  - Ler unico vertice");
                    System.out.println("5  - Ler todos os vertices do grafo");
                    System.out.println("6  - Ler os vizinhos de um vertice");
                    System.out.println("7  - Adicionar aresta");
                    System.out.println("8  - Atualizar aresta ");
                    System.out.println("9  - Deletar aresta ");
                    System.out.println("10 - Ler unica aresta ");
                    System.out.println("11 - Ler todas as arestas do grafo");
                    System.out.println("12 - Ler todas as arestas de um vertice");
                    System.out.println("13 - Menor Caminho entre Vertices");
                    System.out.println("14 - Finalizar conexão");
                    System.out.print("Opcao-> ");
                    opcao = sc.nextInt();

                    switch (opcao) {
                        case 1:
                            System.out.println("\n@@@@@ ADICIONAR VERTICE @@@@@ \n");
                            System.out.println("Estou no nó:" + client.getIDLocal());
                            System.out.print("Nome do vertice-> ");
                            nome = sc.nextInt();
                            System.out.println("Será colocado no nó:" + client.getSucessor(nome % (int) Math.pow(2, 5)).getId());
                            sc.nextLine();
                            System.out.print("Cor do vertice-> ");
                            cor = sc.nextInt();
                            sc.nextLine();
                            System.out.print("Descricao do vertice-> ");
                            descricao = sc.nextLine();
                            System.out.print("Peso do vertice-> ");
                            peso = sc.nextDouble();
                            sc.nextLine();

                            ConcurrentHashMap<Integer, Aresta> HashAresta = new ConcurrentHashMap<>();

                            v.setNome(nome);
                            v.setCor(cor);
                            v.setDescricao(descricao);
                            v.setPeso(peso);
                            v.setHashAresta(HashAresta);

                            if (client.addVertice(v)) {
                                System.out.println("# Vertice adicionado!");
                            } else {
                                System.out.println("# Problema na insercao do vertice. Repita a operacao.");
                            }
                            break;
                        case 2:
                            System.out.println("\n@@@@@ ATUALIZAR VERTICE @@@@@ \n");
                            System.out.print("Nome do vertice-> ");
                            nome = sc.nextInt();
                            sc.nextLine();
                            System.out.print("Cor do vertice-> ");
                            cor = sc.nextInt();
                            sc.nextLine();
                            System.out.print("Descricao do vertice-> ");
                            descricao = sc.nextLine();
                            System.out.print("Peso do vertice-> ");
                            peso = sc.nextDouble();
                            sc.nextLine();

                            v.setNome(nome);
                            v.setCor(cor);
                            v.setDescricao(descricao);
                            v.setPeso(peso);

                            try {
                                if (client.updateVertice(v)) {
                                    System.out.println("# Vertice atualizado!");
                                } else {
                                    System.out.println("# Problema na atualizacao do vertice. Repita a operacao.");
                                }

                            } catch (KeyNotFound e) {
                                System.out.println("Problema ");
                            }
                            break;
                        case 3:
                            System.out.println("\n@@@@@ DELETAR VERTICE @@@@@ \n");
                            System.out.print("Nome do vertice-> ");
                            nome = sc.nextInt();
                            sc.nextLine();

                            try {
                                v = client.readVertice(nome);

                                System.out.println("\n ----------- DADO ENCONTRADO -----------");
                                System.out.println("Nome: " + v.getNome());
                                System.out.println("Cor: " + v.getCor());
                                System.out.println("Descricao: " + v.getDescricao());
                                System.out.println("Peso: " + v.getPeso());
                                System.out.println("ID do Nó: " + v.getIdNode());

                                System.out.println("\n-> Tem certeza que deseja deletar este vertice?");
                                System.out.println("1 - SIM");
                                System.out.println("2 - NÃO");
                                System.out.print("Opcao-> ");
                                int certeza = sc.nextInt();
                                sc.nextLine();

                                switch (certeza) {
                                    case 1:
                                        if (client.deleteVertice(v)) {
                                            System.out.println("# Vertice deletado!");
                                        } else {
                                            System.out.println("# Problema na remocao do vertice. Repita a operacao.");
                                        }
                                        break;
                                    case 2:
                                        System.out.println("# Operacao cancelada.");
                                        break;
                                    default:
                                        System.out.println("# Opcao invalida!");
                                }

                            } catch (KeyNotFound e) {
                                System.out.println("Vertice nao encontrado.");
                            }

                            break;
                        case 4:
                            System.out.println("\n@@@@@ LER UNICO VERTICE @@@@@ \n");
                            System.out.print("Nome do vertice-> ");
                            nome = sc.nextInt();
                            sc.nextLine();

                            try {
                                v = client.readVertice(nome);

                                System.out.println("\n ----------- DADO ENCONTRADO -----------");
                                System.out.println("Nome: " + v.getNome());
                                System.out.println("Cor: " + v.getCor());
                                System.out.println("Descricao: " + v.getDescricao());
                                System.out.println("Peso: " + v.getPeso());
                                System.out.println("ID do Nó: " + v.getIdNode());

                            } catch (KeyNotFound e) {
                                System.out.println("Vertice nao encontrado.");
                            }
                            break;
                        case 5:
                            System.out.println("\n@@@@@ LER TODOS OS VERTICES DO GRAFO @@@@@ \n");

                            try {
                                List<Vertice> vertices = new ArrayList<>();
                                vertices = client.readAllVertice();

                                for (Vertice vertice : vertices) {
                                    System.out.println("\nNome: " + vertice.getNome());
                                    System.out.println("Cor: " + vertice.getCor());
                                    System.out.println("Descricao: " + vertice.getDescricao());
                                    System.out.println("Peso: " + vertice.getPeso());
                                    System.out.println("ID do Nó: " + vertice.getIdNode());
                                }
                            } catch (KeyNotFound e) {
                                System.out.println("Vertices nao encontrados.");
                            }
                            break;
                        case 6:
                            System.out.println("\n@@@@@ LER TODOS OS VIZINHOS DE UM VERTICE @@@@@ \n");
                            System.out.print("Nome do vertice-> ");
                            nome = sc.nextInt();
                            sc.nextLine();

                            try {

                                v = client.readVertice(nome);

                                List<Vertice> vertices = new ArrayList<>();
                                vertices = client.readVerticeNeighboors(v);

                                if (vertices == null) {
                                    System.out.println("Nenhum vizinho para este vertice.");
                                }

                                for (Vertice vertice : vertices) {
                                    System.out.print("\n--- Vertice Vizinho de V" + nome + " ---");
                                    System.out.println("\nNome: " + vertice.getNome());
                                    System.out.println("Cor: " + vertice.getCor());
                                    System.out.println("Descricao: " + vertice.getDescricao());
                                    System.out.println("Peso: " + vertice.getPeso());
                                    System.out.println("ID do Nó: " + vertice.getIdNode());
                                }
                            } catch (KeyNotFound e) {
                                System.out.println("Vertice de referencia nao encontrado.");
                            }
                            break;
                        case 7:
                            try {
                                System.out.println("\n@@@@@ ADICIONAR ARESTA @@@@@ \n");
                                System.out.print("Nome do 1º vertice-> ");
                                v1 = sc.nextInt();
                                sc.nextLine();

                                v = client.readVertice(v1);

                                System.out.print("Nome do 2º vertice-> ");
                                v2 = sc.nextInt();
                                sc.nextLine();
                                System.out.println("-------------------");

                                v = client.readVertice(v2);

                                System.out.print("Peso da aresta-> ");
                                peso = sc.nextDouble();
                                sc.nextLine();
                                System.out.print("Descricao da aresta-> ");
                                descricao = sc.nextLine();
                                System.out.print("Vertice direcionada? -> ");
                                direct = sc.nextBoolean();
                                sc.nextLine();

                                a.setV1(v1);
                                a.setV2(v2);
                                a.setPeso(peso);
                                a.setDescricao(descricao);
                                a.setDirect(direct);

                                if (client.addAresta(a)) {
                                    System.out.println("# Aresta entre os vertices V" + v1 + " e V" + v2 + " adicionada!");

                                    if (direct == false) {
                                        a.setV1(v2);
                                        a.setV2(v1);

                                        if (client.addAresta(a)) {
                                            System.out.println("# Aresta espelhada entre os vertices V" + v2 + " e V" + v1 + " adicionada!");
                                        } else {
                                            System.out.println("# Nao foi possivel inserir outra aresta espelhada");
                                        }
                                    }
                                } else {
                                    System.out.println("# Problema na insercao da aresta. Repita a operacao.");
                                }

                            } catch (KeyNotFound e) {
                                System.out.println("Dado nao encontrado no grafo.");
                            }
                            break;
                        case 8:
                            try {
                                System.out.println("\n@@@@@ ATUALIZAR ARESTA @@@@@ \n");
                                System.out.print("Nome do 1º vertice-> ");
                                v1 = sc.nextInt();
                                sc.nextLine();

                                v = client.readVertice(v1);

                                System.out.print("Nome do 2º vertice-> ");
                                v2 = sc.nextInt();
                                sc.nextLine();
                                System.out.println("-------------------");

                                v = client.readVertice(v2);

                                a = client.readAresta(v1, v2);

                                System.out.print("Peso da aresta-> ");
                                peso = sc.nextDouble();
                                sc.nextLine();
                                System.out.print("Descricao da aresta-> ");
                                descricao = sc.nextLine();

                                a.setV1(v1);
                                a.setV2(v2);
                                a.setPeso(peso);
                                a.setDescricao(descricao);

                                if (client.updateAresta(a)) {
                                    System.out.println("# Aresta atualizada!");

                                    if (a.direct == false) {
                                        a.setV1(v2);
                                        a.setV2(v1);

                                        if (client.updateAresta(a)) {
                                            System.out.println("# Aresta espelhada atualiza! ");
                                        }
                                    }
                                } else {
                                    System.out.println("# Problema na atualizacao da Aresta. Repita a operacao.");
                                }

                            } catch (KeyNotFound e) {
                                System.out.println("# Dado nao encontrado no grafo.");
                            }
                            break;
                        case 9:
                            try {
                                System.out.println("\n@@@@@ DELETAR ARESTA @@@@@ \n");
                                System.out.print("Nome do 1º vertice-> ");
                                v1 = sc.nextInt();
                                sc.nextLine();

                                v = client.readVertice(v1);

                                System.out.print("Nome do 2º vertice-> ");
                                v2 = sc.nextInt();
                                sc.nextLine();
                                System.out.println("-------------------");

                                v = client.readVertice(v2);

                                a = client.readAresta(v1, v2);

                                System.out.println("\n ----------- DADO ENCONTRADO -----------");
                                System.out.println("Nome do 1º vertice-> " + a.getV1());
                                System.out.println("Nome do 2º vertice-> " + a.getV2());
                                System.out.println("Peso da aresta-> " + a.getPeso());
                                System.out.println("Descricao da aresta-> " + a.getDescricao());
                                System.out.println("Vertice direcionada? -> " + a.direct);

                                if (a.direct == false) {
                                    a = client.readAresta(v2, v1);

                                    System.out.println("\n ----------- OUTRO DADO ENCONTRADO -----------");
                                    System.out.println("Nome do 1º vertice-> " + a.getV1());
                                    System.out.println("Nome do 2º vertice-> " + a.getV2());
                                    System.out.println("Peso da aresta-> " + a.getPeso());
                                    System.out.println("Descricao da aresta-> " + a.getDescricao());
                                    System.out.println("Vertice direcionada? -> " + a.direct);
                                }

                                System.out.println("\n-> Tem certeza que deseja deletar esta aresta?");
                                System.out.println("1 - SIM");
                                System.out.println("2 - NÃO");
                                System.out.print("Opcao-> ");
                                int certeza = sc.nextInt();
                                sc.nextLine();

                                switch (certeza) {
                                    case 1:
                                        a.setV1(v1);
                                        a.setV2(v2);

                                        if (client.deleteAresta(a)) {
                                            System.out.println("# Aresta deletada!");

                                            if (a.direct == false) {
                                                a.setV1(v2);
                                                a.setV2(v1);

                                                if (client.deleteAresta(a)) {
                                                    System.out.println("# Aresta espelhada deletada! ");
                                                }
                                            }
                                        } else {
                                            System.out.println("# Problema na remocao da aresta. Repita a operacao.");
                                        }
                                        break;
                                    case 2:
                                        System.out.println("# Operacao cancelada.");
                                        break;
                                    default:
                                        System.out.println("# Opcao invalida!");
                                }

                            } catch (KeyNotFound e) {
                                System.out.println("Vertice nao encontrado.");
                            }

                            break;
                        case 10:
                            try {
                                System.out.println("\n@@@@@ LER UNICA ARESTA @@@@@ \n");
                                System.out.print("Nome do vertice 1-> ");
                                v1 = sc.nextInt();
                                sc.nextLine();

                                client.readVertice(v1);

                                System.out.print("Nome do vertice 2-> ");
                                v2 = sc.nextInt();
                                sc.nextLine();

                                client.readVertice(v2);

                                a = client.readAresta(v1, v2);

                                System.out.println("\n ----------- DADO ENCONTRADO -----------");
                                System.out.println("Nome do 1º vertice-> " + a.getV1());
                                System.out.println("Nome do 2º vertice-> " + a.getV2());
                                System.out.println("Peso da aresta-> " + a.getPeso());
                                System.out.println("Descricao da aresta-> " + a.getDescricao());
                                System.out.println("Vertice direcionada? -> " + a.direct);

                            } catch (KeyNotFound e) {
                                System.out.println("Dados nao encontrados no grafo. Repita a operacao.");
                            }
                            break;
                        case 11:
                            System.out.println("\n@@@@@ LER TODAS AS ARESTAS DO GRAFO @@@@@ \n");

                            try {
                                List<Aresta> arestas = new ArrayList<>();
                                arestas = client.readAllAresta();

                                if (arestas == null) {
                                    System.out.println("Nenhuma aresta no grafo.");
                                }

                                for (Aresta aresta : arestas) {
                                    System.out.println("\nNome do 1º vertice-> " + aresta.getV1());
                                    System.out.println("Nome do 2º vertice-> " + aresta.getV2());
                                    System.out.println("Peso da aresta-> " + aresta.getPeso());
                                    System.out.println("Descricao da aresta-> " + aresta.getDescricao());
                                    System.out.println("Vertice direcionada? -> " + aresta.direct);
                                }
                            } catch (KeyNotFound e) {
                                System.out.println("Vertices nao encontrados.");
                            }
                            break;
                        case 12:
                            try {
                                System.out.println("\n@@@@@ LER TODAS AS ARESTAS DE UM VERTICE @@@@@ \n");
                                System.out.print("Nome do vertice 1-> ");
                                v1 = sc.nextInt();
                                sc.nextLine();

                                v = client.readVertice(v1);

                                List<Aresta> arestas = new ArrayList<>();
                                arestas = client.readAllArestaOfVertice(v);

                                if (arestas == null) {
                                    System.out.println("Nenhuma aresta para este vertice.");
                                }

                                for (Aresta aresta : arestas) {
                                    System.out.println("\nNome do vertice 1 -> " + aresta.getV1());
                                    System.out.println("Nome do vertice 2 -> " + aresta.getV2());
                                    System.out.println("Peso da aresta-> " + aresta.getPeso());
                                    System.out.println("Descricao da aresta-> " + aresta.getDescricao());
                                    System.out.println("Vertice direcionada? -> " + aresta.direct);
                                }
                            } catch (KeyNotFound e) {
                                System.out.println("Dados nao encontrados no grafo. Repita a operacao.");
                            }
                            break;
                        case 13:
                            System.out.println("\n@@@@@ MENOR CAMINHO ENTRE VERTICES @@@@@ \n");
                            System.out.print("Nome do vertice 1-> ");
                            v1 = sc.nextInt();
                            sc.nextLine();

                            client.readVertice(v1);

                            System.out.print("Nome do vertice 2-> ");
                            v2 = sc.nextInt();
                            sc.nextLine();

                            client.readVertice(v2);

                            HashMap<Integer, Integer> ant = new HashMap<>();
                            HashMap<Integer, Double> dist = new HashMap<>();
                            List<Vertice> vertex = new ArrayList<>();

                            vertex = client.menorCaminho(v1, v2, ant, dist);

                            if (vertex.equals(null)) {
                                System.out.println("Não há caminho para esse vértice. Repita a operação.");
                            } else {

                                Collections.reverse(vertex);

                                System.out.println("Menor caminho entre os vértices: " + v1 + " e " + v2);
                                int i = 0;
                                for (Vertice verticeg : vertex) {
                                    System.out.println(i + ":V" + verticeg.getNome() + "|Nó->" + verticeg.getIdNode());
                                    i++;
                                }
                            }
                            break;
                        case 14:
                            // Finaliza a conexão com o servidor
                            System.out.println("\n==> Conexão finalizada!");
                            transport.close();
                            System.exit(0);
                        default:
                            System.out.println("\n==> ERROR! Opção inválida. Selecione uma opção válida do menu.");
                    }

                } while (1 != 0);
            }
        } catch (TException x) {
            System.out.println(x.getMessage());
        }
    }
}
