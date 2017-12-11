package Command;

import Grafo.Vertice;
import io.atomix.copycat.Query;

public class ReadVerticeCommand implements Query<Vertice> {

    public int nome;

    public ReadVerticeCommand(int nome) {
        this.nome = nome;
    }

}
