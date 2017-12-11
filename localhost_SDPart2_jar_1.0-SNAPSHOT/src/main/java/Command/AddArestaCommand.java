
package Command;

import Grafo.Aresta;
import io.atomix.copycat.Command;

public class AddArestaCommand implements Command<Boolean> {
    public Aresta a;

    public AddArestaCommand(Aresta a) {
        this.a = a;
    }
    
    
}
