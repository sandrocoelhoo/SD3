
package Command;

import Grafo.Aresta;
import io.atomix.copycat.Command;

public class DeleteArestaCommand implements Command<Boolean> {
    public Aresta a;

    public DeleteArestaCommand(Aresta a) {
        this.a = a;
    }
    
    
}
