package Command;

import Grafo.Vertice;
import io.atomix.copycat.Query;

public class ReadVerticeNeighboorsCommand implements Query<Vertice> {

    public Vertice v;

    public ReadVerticeNeighboorsCommand(Vertice v) {
        this.v = v;
    }

}
