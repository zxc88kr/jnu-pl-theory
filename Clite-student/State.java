import java.util.*;

public class State extends HashMap<Variable, Value> {
    public State() { }
    
    public State(Variable key, Value val) {
        put(key, val);
    }
    
    public State onion(Variable key, Value val) {
        put(key, val);
        System.out.print("\t{ ");
        for (Variable k : this.keySet())
            System.out.print("<" + k + ", " + this.get(k) + "> ");
        System.out.println("}");
        return this;
    }
    
    public State onion(State t) {
        for (Variable key : t.keySet())
            put(key, t.get(key));
        return this;
    }

    public void display() {
        System.out.print("\t{ ");
        for (Variable key : this.keySet())
            System.out.print("<" + key + ", " + this.get(key) + "> ");
        System.out.println("}");
    }
}
