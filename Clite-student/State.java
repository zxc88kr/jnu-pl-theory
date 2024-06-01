import java.util.*;

class StateFrame {
    Stack<State> stateStack = new Stack<>();

    public StateFrame() { }

    public StateFrame pushState(State state) {
		stateStack.push(state);
		return this;
	}

    public StateFrame popState() {
		stateStack.pop();
		return this;
	}

    public State peekState() {
		return stateStack.peek();
	}

    public StateFrame put(Variable key, Value val) {
		State top = stateStack.peek();
		top.put(key, val);
		return this;
	}

    public StateFrame remove(Variable key) {
		State top = stateStack.peek();
		top.remove(key);
		return this;
	}

    public Value get(Variable key) {
		State top = stateStack.peek();
		Iterator<State> stateIterator = stateStack.listIterator();
		State global = stateIterator.next();
		if (top.get(key) != null) {
			return top.get(key);
		} else {
            return global.get(key);
		}
	}

    public StateFrame onion(Variable key, Value val) {
		State top = stateStack.peek();
		Iterator<State> stateIterator = stateStack.listIterator();
		State global = stateIterator.next();
		if (top.get(key) != null) {
			top.put(key, val);
		} else {
			global.put(key, val);
		}
		return this;
	}

    public StateFrame onion(State t) {
		State top = stateStack.peek();
		for (Variable key : t.keySet()) {
			top.put(key, t.get(key));
		}
		return this;
	}

    public void display() {
        State top = stateStack.peek();
		Iterator<State> stateIterator = stateStack.listIterator();
		State global = stateIterator.next();
		if (global.equals(top)) {
			global.display();
		} else {
			global.display();
			top.display();
		}
	}
}

public class State extends HashMap<Variable, Value> {
    public State() { }
    
    public State(Variable key, Value val) {
        put(key, val);
    }
    
    public State onion(Variable key, Value val) {
        put(key, val);
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
