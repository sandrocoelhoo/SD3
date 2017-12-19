
package Command;

import Grafo.Vertice;
import io.atomix.copycat.Command;

public class AddVerticeCommand implements Command<Boolean> {
    public Vertice v;

    public AddVerticeCommand(Vertice v) {
        this.v = v;
    }

}
