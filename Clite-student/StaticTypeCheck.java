import java.util.*;

public class StaticTypeCheck {
    public static TypeMap typing(Declarations d) {
        TypeMap map = new TypeMap();
        for (Declaration di: d) map.put(di.v, di.t);
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test) return;
        System.err.println(msg);
        System.exit(1);
    }

    public static void V(Declarations d) {
        for (int i = 0; i < d.size() - 1; i++)
            for (int j = i + 1; j < d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
    }

    public static void V(Program p) {
        V(p.decpart);
        V(p.body, typing(p.decpart));
    }

    public static Type typeOf(Expression e, TypeMap tm) {
        if (e instanceof Value) return ((Value)e).type;
        if (e instanceof Variable) {
            Variable v = (Variable)e;
            check(tm.containsKey(v), "undefined variable: " + v);
            return (Type)tm.get(v);
        }
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            if (b.op.ArithmeticOp())
                if (typeOf(b.term1, tm) == Type.FLOAT) return (Type.FLOAT);
                else return (Type.INT);
            if (b.op.RelationalOp() || b.op.BooleanOp())
                return (Type.BOOL);
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            if (u.op.NotOp()) return (Type.BOOL);
            if (u.op.NegateOp()) return typeOf(u.term,tm);
            if (u.op.intOp()) return (Type.INT);
            if (u.op.floatOp()) return (Type.FLOAT);
            if (u.op.charOp()) return (Type.CHAR);
        }
        throw new IllegalArgumentException("should never reach here");
    } 

    public static void V(Expression e, TypeMap tm) {
        if (e instanceof Value) return;
        if (e instanceof Variable) { 
            Variable v = (Variable)e;
            check(tm.containsKey(v), "undeclared variable: " + v);
            return;
        }
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            Type type1 = typeOf(b.term1, tm);
            Type type2 = typeOf(b.term2, tm);
            V(b.term1, tm);
            V(b.term2, tm);
            if (b.op.ArithmeticOp())
                check(type1 == type2 &&
                        (type1 == Type.INT || type1 == Type.FLOAT), "type error for " + b.op);
            else if (b.op.RelationalOp())
                check(type1 == type2 , "type error for " + b.op);
            else if (b.op.BooleanOp())
                check(type1 == Type.BOOL && type2 == Type.BOOL, "type error for " + b.op);
            else throw new IllegalArgumentException("should never reach here");
            return;
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            Type type = typeOf(u.term, tm);
            V(u.term, tm);
            if (u.op.NotOp())
                check(type == Type.BOOL, "type error for " + u.op);
            else if (u.op.NegateOp())
                check(type == Type.INT || type == Type.FLOAT, "type error for " + u.op);
            else if (u.op.floatOp() || u.op.charOp())
                check(type == Type.INT, "type error for " + u.op);
            else if (u.op.intOp())
                check(type == Type.FLOAT || type == Type.CHAR, "type error for " + u.op);
            else throw new IllegalArgumentException("should never reach here");
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V(Statement s, TypeMap tm) {
        if (s == null) throw new IllegalArgumentException("AST error: null statement");
        if (s instanceof Skip) return;
        if (s instanceof Assignment) {
            Assignment a = (Assignment)s;
            check(tm.containsKey(a.target), "undefined target in assignment: " + a.target);
            V(a.source, tm);
            Type ttype = (Type)tm.get(a.target);
            Type srctype = typeOf(a.source, tm);
            if (ttype != srctype) {
                if (ttype == Type.FLOAT)
                    check(srctype == Type.INT, "mixed mode assignment to " + a.target);
                else if (ttype == Type.INT)
                    check(srctype == Type.CHAR, "mixed mode assignment to " + a.target);
                else check(false, "mixed mode assignment to " + a.target);
            }
            return;
        }
        if (s instanceof Conditional) {
            Conditional c = (Conditional)s;
            V(c.test, tm);
            Type testtype = typeOf(c.test, tm);
            if (testtype == Type.BOOL) {
                V(c.thenbranch, tm);
                V(c.elsebranch, tm);
            }
            else check ( false, "poorly typed test: " + c.test);
            return;
        }
        if (s instanceof Loop) {
            Loop l = (Loop)s;
            V(l.test, tm);
            Type testtype = typeOf(l.test, tm);
            if (testtype == Type.BOOL)
                V(l.body, tm);
            else check ( false, "poorly typed test: " + l.test);
            return;
        }
        if (s instanceof Block) {
            Block b = (Block)s;
            for(Statement stmt: b.members)
                V(stmt, tm);
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);
        System.out.println("\nBegin type checking...");
        System.out.println("Type map:");
        TypeMap map = typing(prog.decpart);
        map.display();
        V(prog);
    }
}
