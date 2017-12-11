
package Command;

import Grafo.Aresta;
import io.atomix.copycat.Command;

public class UpdateArestaCommand implements Command<Boolean> {
    public Aresta a;

    public UpdateArestaCommand(Aresta a) {
        this.a = a;
    }
    
    
}
