namespace java Grafo

struct Aresta {
	1:i32 v1,
	2:i32 v2,
	3:double peso,
	4:bool direct,
	5:string descricao
}

struct Vertice {
	1:i32 nome,
	2:i32 cor,
	3:string descricao,
	4:double peso,
	5:map<i32,Aresta> HashAresta,
	6:i32 idNode
}

exception KeyNotFound {
}

struct Finger{
    1:i32 id,
    2:string ip,
    3:i32 port,
	4:i32 portaRaft
}

struct Node{
    1:i32 id,
    2:list<list<Finger>> ft,
    3:Finger pred,
    4:string ip,
    5:i32 port,
   	6:string ipRaftRaiz,
    7:i32 portaRaftRaiz,
    8:i32 portaRaft,
    9:list<Finger> cluster,
	10:i32 idRaftRaiz

}



service Chord {
    void join(1:Node n),
    Node getSucessor(1:i32 id),
    Node getPredecessor(1:i32 id),
    Node closestPrecedingFinger(1:i32 id),
    void stabilize(),
    void notify(1:Node n),
    void fixFingers(),
    Node sendSelf(),
    void setPredecessor(1:Node n),
	i32 verifyID(1:Node n),
	Node randomID(1:Node node),
	i32 getIDLocal(),
	void printTable(),
	list<Finger> sendSelfCluster() throws (1:KeyNotFound knf)
}

service Thrift extends Chord{
	bool addVertice(1:Vertice v) throws (1:KeyNotFound knf),
	Vertice readVertice(1:i32 nome) throws (1:KeyNotFound knf),
	bool updateVertice(1:Vertice v) throws (1:KeyNotFound knf),
	bool deleteVertice(1:Vertice v) throws (1:KeyNotFound knf),
	list<Vertice> readAllVertice() throws (1:KeyNotFound knf),
	list<Vertice> readVerticeNeighboors(1:Vertice v) throws (1:KeyNotFound knf),

	bool addAresta(1:Aresta a) throws (1:KeyNotFound knf),
	Aresta readAresta(1:i32 nomeV1, 2:i32 nomeV2) throws (1:KeyNotFound knf),
	list<Aresta> readAllAresta() throws (1:KeyNotFound knf),
	list<Aresta> readAllArestaOfVertice(1:Vertice v) throws (1:KeyNotFound knf),
	bool updateAresta(1:Aresta a) throws (1:KeyNotFound knf),
	bool deleteAresta(1:Aresta a) throws (1:KeyNotFound knf), 
	list<Vertice> readVerticeNode() throws (1:KeyNotFound knf),
	list<Aresta> readArestaNode() throws (1:KeyNotFound knf),
	i32 procuraMenorDistancia(1:map<i32,double> dist, 2:map<i32,i32> visitado,3:list<Vertice> vertices),
	list<Vertice> menorCaminho(1:i32 ini,2:i32 fim, 3:map<i32,i32> ant, 4:map<i32,double> dist) throws (1:KeyNotFound knf),
	void stabilizeCluster() throws (1:KeyNotFound knf),
	void setCluster(1:list<Finger> cluster) throws (1:KeyNotFound knf)
}
