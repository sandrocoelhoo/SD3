package Command;

import Grafo.Vertice;
import io.atomix.copycat.Query;

public class ReadAllVerticeCommand implements Query<Vertice> {

    public ReadAllVerticeCommand() {
    }

}
