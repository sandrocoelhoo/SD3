package Command;

import Grafo.Vertice;
import io.atomix.copycat.Command;

public class UpdateVerticeCommand implements Command<Boolean> {

    public Vertice vertice;

    public UpdateVerticeCommand(Vertice vertice) {
        this.vertice = vertice;
    }

}
