public class TypeTransformer {
    public static Program T(Program p, TypeMap tm) {
        Functions functions = T(p.functions, tm);
        return new Program(p.globals, functions);
    }

    public static Functions T(Functions fs, TypeMap globals) {
        Functions out = new Functions();
        for (Function f : fs) {
            TypeMap tm = new TypeMap();
            tm.putAll(globals);
            tm.putAll(StaticTypeCheck.typing(f.params));
            tm.putAll(StaticTypeCheck.typing(f.locals));
            Block b = (Block)T(f.body, tm);
            out.add(new Function(f.type, f.id, f.params, f.locals, b));
        }
        return out;
    }

    public static Expression T(Expression e, TypeMap tm) {
        if (e instanceof Value) return e;
        if (e instanceof Variable) return e;
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            Type type1 = StaticTypeCheck.typeOf(b.term1, tm);
            Type type2 = StaticTypeCheck.typeOf(b.term2, tm);
            Expression t1 = T(b.term1, tm);
            Expression t2 = T(b.term2, tm);
            if (type1 == Type.INT) {
                if (type2 == Type.FLOAT)
                    t2 = new Unary(new Operator(Operator.F2I), t2);
                if (type2 == Type.CHAR)
                    t2 = new Unary(new Operator(Operator.C2I), t2);
                return new Binary(b.op.intMap(b.op.val), t1, t2);
            }
            if (type1 == Type.FLOAT) {
                if (type2 == Type.INT)
                    t2 = new Unary(new Operator(Operator.I2F), t2);
                return new Binary(b.op.floatMap(b.op.val), t1, t2);
            }
            if (type1 == Type.CHAR) {
                if (type2 == Type.INT)
                    t2 = new Unary(new Operator(Operator.I2C), t2);
                return new Binary(b.op.charMap(b.op.val), t1, t2);
            }
            if (type1 == Type.BOOL)
                return new Binary(b.op.boolMap(b.op.val), t1, t2);
            throw new IllegalArgumentException("should never reach here");
        }
        if (e instanceof Unary) {
            Unary u = (Unary)e;
            Type type = StaticTypeCheck.typeOf(u.term, tm);
            Expression t0 = T(u.term, tm);
            if (u.op.NotOp()) {
                if (type == Type.BOOL)
                    return new Unary(u.op.boolMap(u.op.val), t0);
            }
            if (u.op.NegateOp()) {
                if (type == Type.INT)
                    return new Unary(u.op.intMap(u.op.val), t0);
                if (type == Type.FLOAT)
                    return new Unary(u.op.floatMap(u.op.val), t0);
            }
            if (u.op.intOp()) {
                if (type == Type.INT)
                    return new Unary(u.op.intMap(u.op.val), t0);
                if (type == Type.FLOAT)
                    return new Unary(u.op.floatMap(u.op.val), t0);
                if (type == Type.CHAR)
                    return new Unary(u.op.charMap(u.op.val), t0);
            }
            if (u.op.floatOp()) {
                if (type == Type.INT)
                    return new Unary(u.op.intMap(u.op.val), t0);
                if (type == Type.FLOAT)
                    return new Unary(u.op.floatMap(u.op.val), t0);
            }
            if (u.op.charOp()) {
                if (type == Type.INT)
                    return new Unary(u.op.intMap(u.op.val), t0);
                if (type == Type.CHAR)
                    return new Unary(u.op.charMap(u.op.val), t0);
            }
            throw new IllegalArgumentException("should never reach here");
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static Statement T(Statement s, TypeMap tm) {
        if (s instanceof Skip) return s;
        if (s instanceof Assignment) {
            Assignment a = (Assignment)s;
            Variable target = a.target;
            Expression src = T(a.source, tm);
            Type ttype = tm.get(a.target);
            Type srctype = StaticTypeCheck.typeOf(a.source, tm);
            if (ttype == Type.FLOAT) {
                if (srctype == Type.INT) {
                    src = new Unary(new Operator(Operator.I2F), src);
                    srctype = Type.FLOAT;
                }
            }
            else if (ttype == Type.INT) {
                if (srctype == Type.CHAR) {
                    src = new Unary(new Operator(Operator.C2I), src);
                    srctype = Type.INT;
                }
            }
            StaticTypeCheck.check(ttype == srctype, "bug in assignment to " + target);
            return new Assignment(target, src);
        }
        if (s instanceof Conditional) {
            Conditional c = (Conditional)s;
            Expression test = T(c.test, tm);
            Statement tbr = T(c.thenbranch, tm);
            Statement ebr = T(c.elsebranch, tm);
            return new Conditional(test, tbr, ebr);
        }
        if (s instanceof Loop) {
            Loop l = (Loop)s;
            Expression test = T(l.test, tm);
            Statement body = T(l.body, tm);
            return new Loop(test, body);
        }
        if (s instanceof Block) {
            Block b = (Block)s;
            Block out = new Block();
            for (Statement stmt : b)
                out.add(T(stmt, tm));
            return out;
        }
        if (s instanceof Call) {
            Call c = (Call)s;
            Expressions exp = new Expressions();
            for (Expression e : c.args)
                exp.add(T(e, tm));
            return new Call(c.name, exp);
        }
        if (s instanceof Return) {
            Return r = (Return)s;
            Expression result = T(r.result, tm);
            return new Return(r.target, result);
        }
        throw new IllegalArgumentException("should never reach here");
    }
    
    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0);
        System.out.println("Type map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals);
        map.display();
        StaticTypeCheck.V(prog);
        Program out = T(prog, map);
        System.out.println("Output AST");
        out.display(0);
    }
}
