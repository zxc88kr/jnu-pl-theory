import java.util.*;

public class Parser {
    Token token;
    Variable currentFunction;
    Lexer lexer;

    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts; // as a token stream, and
        token = lexer.next(); // retrieve its first Token
    }

    private String match(TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else error(t);
        return value;
    }

    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok + "; saw: " + token);
        System.exit(1);
    }

    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok + "; saw: " + token);
        System.exit(1);
    }

    public Program program() {
        // Program --> { Type Identifier FunctionOrGlobal } MainFunction
        Declarations globals = new Declarations();
		Functions functions = new Functions();
		while (isType()) {
			FunctionOrGlobal(globals, functions);
		}
		Function mainFunction = MainFunction();
		functions.add(mainFunction);
		return new Program(globals, functions);
    }

    private void FunctionOrGlobal(Declarations globals, Functions functions) {
		// FunctionOrGlobal -> (Parameters) {Declarations Statements} | Global
        Variable v;
		Type t = type();
        Declarations params, locals;
        Block b;
		if (t.equals(Type.INT) && isMain()) return;
		v = new Variable(match(TokenType.Identifier));
		if (isLeftParen()) {
			currentFunction = v;
			token = lexer.next();
			params = Parameters();
			match(TokenType.RightParen);
			match(TokenType.LeftBrace);
			locals = declarations();
			b = progstatements();
			match(TokenType.RightBrace);
			functions.add(new Function(t, v.toString(), params, locals, b));
		} else {
			globals.add(new Declaration(v, t));
			Global(t, globals);
		}
	}
	
	private Declarations Parameters() {
		// Parameters -> [ Parameter {, Parameter } ]
		// Parameter -> Type Identifier
        Variable v;
        Type t = type();
		Declarations param = new Declarations();
        v = new Variable(match(TokenType.Identifier));
		param.add(new Declaration(v, t));
		while (isComma()) {
			token = lexer.next();
			t = type();
			v = new Variable(match(TokenType.Identifier));
			param.add(new Declaration(v, t));
		}
		return param;
	}
	
	private void Global(Type t, Declarations globals) {
        // Global -> { , Identifier }
        Variable v;
		while (isComma()) {
			token = lexer.next();
			v = new Variable(match(TokenType.Identifier));
			globals.add(new Declaration(v, t));
		}
		match(TokenType.Semicolon);
	}
	
	private Function MainFunction() {
		// MainFunction -> int main() { Declarations Statements }
        Declarations params, locals;
        Block b;
		match(TokenType.Main);
		match(TokenType.LeftParen);
		params = declarations();
		match(TokenType.RightParen);
		match(TokenType.LeftBrace);
		locals = declarations();
		b = progstatements();
		match(TokenType.RightBrace);
		return new Function(Type.INT, Token.mainTok.toString(), params, locals, b);
	}

    private Declarations declarations() {
        // Declarations --> { Declaration }
        Declarations ds = new Declarations();
        while (isType()) {
            declaration(ds);
        }
        return ds;
    }

    private void declaration(Declarations ds) {
        // Declaration --> Type Identifier {, Identifier };
        Variable v;
        Declaration d;
        Type t = type();
        v = new Variable(match(TokenType.Identifier));
        d = new Declaration(v, t);
        ds.add(d);
        while (isComma()) {
            token = lexer.next();
            v = new Variable(match(TokenType.Identifier));
            d = new Declaration(v, t);
            ds.add(d);
        }
        match(TokenType.Semicolon);
    }

    private Type type() {
        // Type --> int | bool | float | char | void
        Type t = null;
        if (isInt()) {
            t = Type.INT;
        } else if (isBoolean()) {
            t = Type.BOOL;
        } else if (isFloat()) {
            t = Type.FLOAT;
        } else if (isChar()) {
            t = Type.CHAR;
        } else if (isVoid()) {
			t = Type.VOID;
		} else error("type token");
        token = lexer.next();
        return t;
    }

    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement | Return | Call
        Statement s = null;
        if (isSemicolon()) {
            s = new Skip();
        } else if (isLeftBrace()) {
            s = statements();
        } else if (isIdentifier()) {
            Variable id = new Variable(token.value());
			match(TokenType.Identifier);
			if (isAssign()) {
				s = assignment(id);
			} else if (isLeftParen()) {
				s = callStatement(id.toString());
			} else {
				error("statement token");
			}
        } else if (isIf()) {
            s = ifStatement();
        } else if (isWhile()) {
            s = whileStatement();
        } else if (isReturn()) {
			s = returnStatement();
		} else error("statement token");
        return s;
    }

    private Block statements() {
        // Block --> '{' Statements '}'
        Statement s;
        Block b = new Block();
        match(TokenType.LeftBrace);
        while (isStatement()) {
            s = statement();
            b.members.add(s);
        }
        match(TokenType.RightBrace);
        return b;
    }

    private Block progstatements() {
        // Block --> Statements
        Statement s;
        Block b = new Block();
        while (isStatement()) {
            s = statement();
            b.members.add(s);
        }
        return b;
    }

    private Assignment assignment(Variable id) {
        // Assignment --> Identifier = Expression;
        Expression e;
        match(TokenType.Assign);
        e = expression();
        match(TokenType.Semicolon);
        return new Assignment(id, e);
    }

    private Conditional ifStatement() {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        Conditional con;
        Expression exp;
        Statement s;
        match(TokenType.If);
        match(TokenType.LeftParen);
        exp = expression();
        match(TokenType.RightParen);
        s = statement();
        if (isElse()) {
            token = lexer.next();
            Statement elsestate = statement();
            con = new Conditional(exp, s, elsestate);
        } else {
            con = new Conditional(exp, s);
        }
        return con;
    }

    private Loop whileStatement() {
        // WhileStatement --> while ( Expression ) Statement
        Expression exp;
        Statement s;
        match(TokenType.While);
        match(TokenType.LeftParen);
        exp = expression();
        match(TokenType.RightParen);
        s = statement();
        return new Loop(exp, s);
    }

	private Call callStatement(String id) {
		// CallStatement -> Call;
		// Call -> Identifier ( Arguments )
        Expressions args;
		match(TokenType.LeftParen);
		args = new Expressions();
		while (!isRightParen()) {
			args.add(expression());
			if (isComma()) match(TokenType.Comma);
		}
		match(TokenType.RightParen);
        match(TokenType.Semicolon);
		return new Call(id, args);
	}
	
	private Return returnStatement() {
		// ReturnStatement -> return Expression;
        Expression ret;
		match(TokenType.Return);
		ret = expression();
		match(TokenType.Semicolon);
		return new Return(currentFunction, ret);
	}

    private Expression expression() {
        // Expression --> Conjunction { || Conjunction }
        Expression con = conjunction();
        while (isOr()) {
            Operator op = new Operator(match(token.type()));
            Expression con2 = conjunction();
            con = new Binary(op, con, con2);
        }
        return con;
    }

    private Expression conjunction() {
        // Conjunction --> Equality { && Equality }
        Expression eq = equality();
        while (isAnd()) {
            Operator op = new Operator(match(token.type()));
            Expression eq2 = equality();
            eq = new Binary(op, eq, eq2);
        }
        return eq;
    }

    private Expression equality() {
        // Equality --> Relation [ EquOp Relation ]
        Expression rel = relation();
        while (isEqualityOp()) {
            Operator op = new Operator(match(token.type()));
            Expression rel2 = relation();
            rel = new Binary(op, rel, rel2);
        }
        return rel;
    }

    private Expression relation() {
        // Relation --> Addition [ RelOp Addition ]
        Expression add = addition();
        while (isRelationalOp()) {
            Operator op = new Operator(match(token.type()));
            Expression add2 = addition();
            add = new Binary(op, add, add2);
        }
        return add;
    }

    private Expression addition() {
        // Addition --> Term { AddOp Term }
        Expression term = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            term = new Binary(op, term, term2);
        }
        return term;
    }

    private Expression term() {
        // Term --> Factor { MultiplyOp Factor }
        Expression fac = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression fac2 = factor();
            fac = new Binary(op, fac, fac2);
        }
        return fac;
    }

    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        }
        return primary();
    }

    private Expression primary() {
        // Primary --> Identifier | Literal | ( Expression ) | Type ( Expression )
        Expression e = null;
        if (isIdentifier()) {
            e = new Variable(match(TokenType.Identifier));
        } else if (isLiteral()) {
            e = literal();
        } else if (isLeftParen()) {
            token = lexer.next();
            e = expression();
            match(TokenType.RightParen);
        } else if (isType()) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal() {
        Value value = null;
        String stval = token.value();
        if (isIntLiteral()) {
            value = new IntValue(Integer.parseInt(stval));
            token = lexer.next();
        } else if (isBooleanLiteral()) {
			value = new BoolValue(Boolean.parseBoolean(stval));
			token = lexer.next();
		} else if (isFloatLiteral()) {
            value = new FloatValue(Float.parseFloat(stval));
            token = lexer.next();
        } else if (isCharLiteral()) {
            value = new CharValue(stval.charAt(0));
            token = lexer.next();
        } else {
            error("literal token");
        }
        return value;
    }

    private boolean isBooleanOp() {
        return isAnd() || isOr();
    }

    private boolean isAnd() {
		return token.type().equals(TokenType.And);
	}

    private boolean isOr() {
		return token.type().equals(TokenType.Or);
	}

    private boolean isAddOp() {
        return token.type().equals(TokenType.Plus) ||
               token.type().equals(TokenType.Minus);
    }

    private boolean isMultiplyOp() {
        return token.type().equals(TokenType.Multiply) ||
               token.type().equals(TokenType.Divide);
    }

    private boolean isUnaryOp() {
        return token.type().equals(TokenType.Not) ||
               token.type().equals(TokenType.Minus);
    }

    private boolean isEqualityOp() {
        return token.type().equals(TokenType.Equals) ||
               token.type().equals(TokenType.NotEqual);
    }

    private boolean isRelationalOp() {
        return token.type().equals(TokenType.Less) ||
               token.type().equals(TokenType.LessEqual) ||
               token.type().equals(TokenType.Greater) ||
               token.type().equals(TokenType.GreaterEqual);
    }

    private boolean isType() {
        return isInt() || isBoolean() || isFloat() || isChar() || isVoid();
    }

    private boolean isInt() {
        return token.type().equals(TokenType.Int);
    }

    private boolean isBoolean() {
        return token.type().equals(TokenType.Bool);
    }

    private boolean isFloat() {
        return token.type().equals(TokenType.Float);
    }

    private boolean isChar() {
        return token.type().equals(TokenType.Char);
    }

    private boolean isVoid() {
        return token.type().equals(TokenType.Void);
    }

    private boolean isLiteral() {
        return isIntLiteral() || isBooleanLiteral() ||
               isFloatLiteral() || isCharLiteral();
    }

    private boolean isIntLiteral() {
        return token.type().equals(TokenType.IntLiteral);
    }

    private boolean isBooleanLiteral() {
        return token.type().equals(TokenType.True) ||
               token.type().equals(TokenType.False);
    }

    private boolean isFloatLiteral() {
        return token.type().equals(TokenType.FloatLiteral);
    }

    private boolean isCharLiteral() {
        return token.type().equals(TokenType.CharLiteral);
    }

    private boolean isComma() {
        return token.type().equals(TokenType.Comma);
    }

    private boolean isStatement() {
        return isSemicolon() || isLeftBrace() ||
               isIf() || isWhile() || isIdentifier();
    }

    private boolean isSemicolon() {
        return token.type().equals(TokenType.Semicolon);
    }

    private boolean isLeftBrace() {
        return token.type().equals(TokenType.LeftBrace);
    }

    private boolean isRightBrace() {
        return token.type().equals(TokenType.RightBrace);
    }

    private boolean isLeftParen() {
		return token.type().equals(TokenType.LeftParen);
	}

    private boolean isRightParen() {
		return token.type().equals(TokenType.RightParen);
	}

    private boolean isIf() {
		return token.type().equals(TokenType.If);
	}

    private boolean isElse() {
		return token.type().equals(TokenType.Else);
	}

    private boolean isWhile() {
		return token.type().equals(TokenType.While);
	}

    private boolean isReturn() {
		return token.type().equals(TokenType.Return);
	}

    private boolean isIdentifier() {
		return token.type().equals(TokenType.Identifier);
	}

    private boolean isAssign() {
        return token.type().equals(TokenType.Assign);
    }

    private boolean isMain() {
		return token.type().equals(TokenType.Main);
	}

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);
    }
}
