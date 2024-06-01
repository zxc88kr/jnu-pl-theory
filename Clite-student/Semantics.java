import java.util.ArrayList;

public class Semantics {
    StateFrame M(Program p) {
        StateFrame stateFrame = new StateFrame();
		stateFrame.pushState(initialState(p.globals));
        stateFrame = M(new Call("main", new ArrayList<>()), stateFrame, p.functions);
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
		// Call 하는 함수 가져옴.
		Function function = functions.getFunction(call.name);
		
		// 새로운 State
		State newState = new State();
		
		// 로컬 변수 추가.
		for (Declaration declaration : function.locals)
		{
			newState.put(declaration.v, Value.mkValue(declaration.t));
		}
		
		// 매개변수와 파라미터의 이터레이터.
		// 각각을 매핑
		Iterator<Expression> argIt = call.args.iterator();
		Iterator<Declaration> funcIt = function.params.iterator();
		while (argIt.hasNext())
		{
			Expression expression = argIt.next();
			Declaration declaration = funcIt.next();
			// 매개변수 값 계산.
			Value value = M(expression, stateFrame, functions);
			// 파라미터에 넣음.
			newState.put(declaration.v, value);
		}
		
		// 추가
		stateFrame.pushState(newState);
		
		// 현재 함수도 State 에 넣음
		// main 의 경우는 넣지 않는다.
		if (!call.name.equals(Token.mainTok.value()))
		{
			stateFrame.put(new Variable(call.name), Value.mkValue(functions.getFunction(call.name).t));
		}
		
		// Call Display
		Display.print(0, "Calling " + call.name);
		stateFrame.display();
		
		// 함수 body 의 모든 Statement 계산.
		Iterator<Statement> members = function.body.members.iterator();
		while (members.hasNext())
		{
			Statement statement = members.next();
			
			// 다른 Statement 에서 리턴했으면 함수 이름이 있음
			if(stateFrame.get(new Variable(call.name)) != null && !stateFrame.get(new Variable(call.name)).isUndef())
			{
				Display.print(0, "Returning " + call.name);
				stateFrame.display();
				
				return stateFrame;
			}
			
			// 리턴이면 함수 종료.
			if (statement instanceof Return)
			{
				Return r = (Return) statement;
				// 리턴할 값 계산.
				Value returnValue = M(r.result, stateFrame, functions);
				// 삽입
				stateFrame.put(r.target, returnValue);
				
				Display.print(0, "Returning " + call.name);
				stateFrame.display();
				
				return stateFrame;
			}
			// 아니면 Statement 계산
			else
			{
				stateFrame = M(statement, stateFrame, functions);
			}
		}
		
		// Display
		Display.print(0, "Returning " + call.name);
		stateFrame.display();
		
		return stateFrame;
	}

    State M(Statement s, State state) {
        if (s instanceof Skip) return M((Skip)s, state);
        if (s instanceof Assignment) return M((Assignment)s, state);
        if (s instanceof Block) return M((Block)s, state);
        if (s instanceof Loop) return M((Loop)s, state);
        if (s instanceof Conditional) return M((Conditional)s, state);
        throw new IllegalArgumentException("should never reach here");
    }

    State M(Skip s, State state) {
        return state;
    }

    State M(Assignment a, State state) {
        return state.onion(a.target, M(a.source, state));
    }

    State M(Block b, State state) {
        for (Statement s : b.members)
            state = M(s, state);
        return state;
    }

    State M(Loop l, State state) {
        if (M(l.test, state).boolValue())
            return M(l, M(l.body, state));
        else return state;
    }

    State M(Conditional c, State state) {
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

    Value M(Expression e, State state) {
        if (e instanceof Value)
            return (Value)e;
        if (e instanceof Variable)
            return (Value)(state.get(e));
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
        TypeMap map = StaticTypeCheck.typing(prog.decpart);
        map.display();
        StaticTypeCheck.V(prog);
        Program out = TypeTransformer.T(prog, map);
        System.out.println("Output AST");
        out.display(0);
        Semantics semantics = new Semantics();
        System.out.println("Change State");
        State state = semantics.M(out);
        System.out.println("Final State");
        state.display();
    }
}
