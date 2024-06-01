import java.util.ArrayList;
import java.util.Iterator;

public class Semantics {
    StateFrame M(Program p) {
        StateFrame stateFrame = new StateFrame();
		stateFrame.pushState(initialState(p.globals));
        stateFrame = M(new Call("main", new Expressions()), stateFrame, p.functions);
        stateFrame.popState();
		return stateFrame;
    }

    State initialState(Declarations ds) {
        State state = new State();
        Value intUndef = new IntValue();
        for (Declaration decl : ds)
            state.put(decl.var, Value.mkValue(decl.type));
        return state;
    }

    StateFrame M(Call call, StateFrame stateFrame, Functions functions) {
		Function function = functions.getFunction(call.name);
		State newState = new State();
		for (Declaration decl : function.locals)
            newState.put(decl.var, Value.mkValue(decl.type));

		Iterator<Expression> argIt = call.args.iterator();
		Iterator<Declaration> funcIt = function.params.iterator();
		while (argIt.hasNext()) {
			Expression expression = argIt.next();
			Declaration declaration = funcIt.next();
			Value value = M(expression, stateFrame);
			newState.put(declaration.var, value);
		}
		stateFrame.pushState(newState);
		
		if (!call.name.equals(Token.mainTok.value())) {
			stateFrame.put(new Variable(call.name), Value.mkValue(functions.getFunction(call.name).type));
		}
		System.out.print("Call: " + call.name);
		stateFrame.display();
		
		Iterator<Statement> members = function.body.members.iterator();
		while (members.hasNext()) {
			Statement statement = members.next();
			if (stateFrame.get(new Variable(call.name)) != null && !stateFrame.get(new Variable(call.name)).isUndef()) {
				System.out.print("Return: " + call.name);
				stateFrame.display();
				return stateFrame;
			}
			if (statement instanceof Return) {
				Return r = (Return)statement;
				Value returnValue = M(r.result, stateFrame);
				stateFrame.put(r.target, returnValue);
				System.out.print("Return: " + call.name);
				stateFrame.display();
				return stateFrame;
			} else {
                stateFrame = M(statement, stateFrame);
            }
		}
		System.out.print("Return: " + call.name);
		stateFrame.display();
		return stateFrame;
	}

    StateFrame M(Statement s, StateFrame state) {
        if (s instanceof Skip) return M((Skip)s, state);
        if (s instanceof Assignment) return M((Assignment)s, state);
        if (s instanceof Block) return M((Block)s, state);
        if (s instanceof Loop) return M((Loop)s, state);
        if (s instanceof Conditional) return M((Conditional)s, state);
        throw new IllegalArgumentException("should never reach here");
    }

    StateFrame M(Skip s, StateFrame state) {
        return state;
    }

    StateFrame M(Assignment a, StateFrame state) {
        return state.onion(a.target, M(a.source, state));
    }

    StateFrame M(Block b, StateFrame state) {
        for (Statement s : b.members)
            state = M(s, state);
        return state;
    }

    StateFrame M(Loop l, StateFrame state) {
        if (M(l.test, state).boolValue())
            return M(l, M(l.body, state));
        else return state;
    }

    StateFrame M(Conditional c, StateFrame state) {
        if (M(c.test, state).boolValue())
            return M(c.thenbranch, state);
        else return M(c.elsebranch, state);
    }

    Value applyBinary(Operator op, Value v1, Value v2) {
        StaticTypeCheck.check(!v1.isUndef() && !v2.isUndef(), "reference to undef value");
        if (op.val.equals(Operator.INT_PLUS))
            return new IntValue(v1.intValue() + v2.intValue());
        if (op.val.equals(Operator.INT_MINUS))
            return new IntValue(v1.intValue() - v2.intValue());
        if (op.val.equals(Operator.INT_TIMES))
            return new IntValue(v1.intValue() * v2.intValue());
        if (op.val.equals(Operator.INT_DIV))
            return new IntValue(v1.intValue() / v2.intValue());

        if (op.val.equals(Operator.INT_LT))
            return new BoolValue(v1.intValue() < v2.intValue());
        if (op.val.equals(Operator.INT_LE))
            return new BoolValue(v1.intValue() <= v2.intValue());
        if (op.val.equals(Operator.INT_EQ))
            return new BoolValue(v1.intValue() == v2.intValue());
        if (op.val.equals(Operator.INT_NE))
            return new BoolValue(v1.intValue() != v2.intValue());
        if (op.val.equals(Operator.INT_GT))
            return new BoolValue(v1.intValue() > v2.intValue());
        if (op.val.equals(Operator.INT_GE))
            return new BoolValue(v1.intValue() >= v2.intValue());

        if (op.val.equals(Operator.FLOAT_PLUS))
            return new FloatValue(v1.floatValue() + v2.floatValue());
        if (op.val.equals(Operator.FLOAT_MINUS))
            return new FloatValue(v1.floatValue() - v2.floatValue());
        if (op.val.equals(Operator.FLOAT_TIMES))
            return new FloatValue(v1.floatValue() * v2.floatValue());
        if (op.val.equals(Operator.FLOAT_DIV))
            return new FloatValue(v1.floatValue() / v2.floatValue());

        if (op.val.equals(Operator.FLOAT_LT))
            return new BoolValue(v1.floatValue() < v2.floatValue());
        if (op.val.equals(Operator.FLOAT_LE))
            return new BoolValue(v1.floatValue() <= v2.floatValue());
        if (op.val.equals(Operator.FLOAT_EQ))
            return new BoolValue(v1.floatValue() == v2.floatValue());
        if (op.val.equals(Operator.FLOAT_NE))
            return new BoolValue(v1.floatValue() != v2.floatValue());
        if (op.val.equals(Operator.FLOAT_GT))
            return new BoolValue(v1.floatValue() > v2.floatValue());
        if (op.val.equals(Operator.FLOAT_GE))
            return new BoolValue(v1.floatValue() >= v2.floatValue());

        if (op.val.equals(Operator.CHAR_LT))
            return new BoolValue(v1.charValue() < v2.charValue());
        if (op.val.equals(Operator.CHAR_LE))
            return new BoolValue(v1.charValue() <= v2.charValue());
        if (op.val.equals(Operator.CHAR_EQ))
            return new BoolValue(v1.charValue() == v2.charValue());
        if (op.val.equals(Operator.CHAR_NE))
            return new BoolValue(v1.charValue() != v2.charValue());
        if (op.val.equals(Operator.CHAR_GT))
            return new BoolValue(v1.charValue() > v2.charValue());
        if (op.val.equals(Operator.CHAR_GE))
            return new BoolValue(v1.charValue() >= v2.charValue());

        if (op.val.equals(Operator.BOOL_AND))
            return new BoolValue(v1.boolValue() && v2.boolValue());
        if (op.val.equals(Operator.BOOL_OR))
            return new BoolValue(v1.boolValue() || v2.boolValue());
        if (op.val.equals(Operator.BOOL_EQ))
            return new BoolValue(v1.boolValue() == v2.boolValue());
        if (op.val.equals(Operator.BOOL_NE))
            return new BoolValue(v1.boolValue() != v2.boolValue());

        throw new IllegalArgumentException("should never reach here");
    }

    Value applyUnary(Operator op, Value v) {
        StaticTypeCheck.check(!v.isUndef(), "reference to undef value");
        if (op.val.equals(Operator.NOT))
            return new BoolValue(!v.boolValue());
        if (op.val.equals(Operator.INT_NEG))
            return new IntValue(-v.intValue());
        if (op.val.equals(Operator.FLOAT_NEG))
            return new FloatValue(-v.floatValue());
        if (op.val.equals(Operator.I2F))
            return new FloatValue((float)(v.intValue()));
        if (op.val.equals(Operator.F2I))
            return new IntValue((int)(v.floatValue()));
        if (op.val.equals(Operator.C2I))
            return new IntValue((int)(v.charValue()));
        if (op.val.equals(Operator.I2C))
            return new CharValue((char)(v.intValue()));
        throw new IllegalArgumentException("should never reach here");
    }

    Value M(Expression e, StateFrame state) {
        if (e instanceof Value)
            return (Value)e;
        if (e instanceof Variable)
            return (Value)(state.get((Variable)e));
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            return applyBinary(b.op, M(b.term1, state), M(b.term2, state));
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            return applyUnary(u.op, M(u.term, state));
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);
        System.out.println("\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals);
        map.display();
        StaticTypeCheck.V(prog);
        Program out = TypeTransformer.T(prog, map);
        System.out.println("Output AST");
        out.display(0);
        Semantics semantics = new Semantics();
        System.out.println("Change State");
        StateFrame state = semantics.M(out);
        System.out.println("Final State");
        state.display();
    }
}
