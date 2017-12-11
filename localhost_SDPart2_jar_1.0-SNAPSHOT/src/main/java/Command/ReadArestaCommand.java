package Command;

import Grafo.Vertice;
import io.atomix.copycat.Query;

public class ReadArestaCommand implements Query<Vertice> {

    public int v1, v2;

    public ReadArestaCommand(int v1, int v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

}
