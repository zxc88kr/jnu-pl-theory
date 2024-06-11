import java.util.*;

public class TypeMap extends HashMap<Variable, Type> {
    public TypeMap onion(TypeMap tm) {
        TypeMap res = new TypeMap();
        res.putAll(this);
        res.putAll(tm);
        return res;
    }

    public void display() {
        for (Variable key : keySet()) {
            System.out.print("\t" + key + ", " );
            Type t = this.get(key);
            if (t instanceof ProtoType) {
                System.out.print(((ProtoType)t).id + ", ");
                ((ProtoType)t).params.display(0);
            } else {
                System.out.print(get(key).id + ",\t");
            }
        }
    }
}
