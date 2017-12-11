
package Command;

import Grafo.Vertice;
import io.atomix.copycat.Command;

public class DeleteVerticeCommand implements Command<Boolean> {
    public Vertice v;

    public DeleteVerticeCommand(Vertice v) {
        this.v = v;
    }
    
    
}
