/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Command;

import Grafo.Vertice;
import io.atomix.copycat.Query;
import java.util.List;

/**
 *
 * @author davyjones
 */
public class ReadVerticeNodeCommand implements Query<List<Vertice>> {

    public ReadVerticeNodeCommand() {

    }

}
