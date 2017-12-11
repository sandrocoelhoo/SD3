package Command;

import Grafo.Vertice;
import io.atomix.copycat.Query;

public class ReadAllArestaOfVerticeCommand implements Query<Vertice> {

    public Vertice v;

    public ReadAllArestaOfVerticeCommand(Vertice v) {
        this.v = v;
    }

}
