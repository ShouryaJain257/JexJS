package com.thunder.js;

import java.util.List;
import java.util.Map;

public abstract class Node {

    // ─── Statements ─────────────────────────────────────────────────────────────

    public static class Program extends Node {
        public final List<Node> body;
        public Program(List<Node> body) { this.body = body; }
    }

    public static class VarDecl extends Node {
        public final String kind; // let, const, var
        public final String name;
        public final Node init; // may be null
        public VarDecl(String kind, String name, Node init) {
            this.kind = kind; this.name = name; this.init = init;
        }
    }

    public static class VarDestructureDecl extends Node {
        public final String kind;
        public final List<String> names;
        public final Node init;
        public VarDestructureDecl(String kind, List<String> names, Node init) {
            this.kind = kind; this.names = names; this.init = init;
        }
    }

    public static class ArrayDestructureDecl extends Node {
        public final String kind;
        public final List<String> names;
        public final Node init;
        public ArrayDestructureDecl(String kind, List<String> names, Node init) {
            this.kind = kind; this.names = names; this.init = init;
        }
    }

    public static class ExprStmt extends Node {
        public final Node expr;
        public ExprStmt(Node expr) { this.expr = expr; }
    }

    public static class BlockStmt extends Node {
        public final List<Node> body;
        public BlockStmt(List<Node> body) { this.body = body; }
    }

    public static class IfStmt extends Node {
        public final Node condition;
        public final Node consequent;
        public final Node alternate; // may be null
        public IfStmt(Node condition, Node consequent, Node alternate) {
            this.condition = condition; this.consequent = consequent; this.alternate = alternate;
        }
    }

    public static class WhileStmt extends Node {
        public final Node condition;
        public final Node body;
        public WhileStmt(Node condition, Node body) {
            this.condition = condition; this.body = body;
        }
    }

    public static class DoWhileStmt extends Node {
        public final Node body;
        public final Node condition;
        public DoWhileStmt(Node body, Node condition) {
            this.body = body; this.condition = condition;
        }
    }

    public static class ForStmt extends Node {
        public final Node init;   // may be null
        public final Node test;   // may be null
        public final Node update; // may be null
        public final Node body;
        public ForStmt(Node init, Node test, Node update, Node body) {
            this.init = init; this.test = test; this.update = update; this.body = body;
        }
    }

    public static class ForOfStmt extends Node {
        public final String varKind;
        public final String varName;
        public final Node iterable;
        public final Node body;
        public ForOfStmt(String varKind, String varName, Node iterable, Node body) {
            this.varKind = varKind; this.varName = varName;
            this.iterable = iterable; this.body = body;
        }
    }

    public static class ForInStmt extends Node {
        public final String varKind;
        public final String varName;
        public final Node object;
        public final Node body;
        public ForInStmt(String varKind, String varName, Node object, Node body) {
            this.varKind = varKind; this.varName = varName;
            this.object = object; this.body = body;
        }
    }

    public static class SwitchStmt extends Node {
        public final Node discriminant;
        public final List<SwitchCase> cases;
        public SwitchStmt(Node discriminant, List<SwitchCase> cases) {
            this.discriminant = discriminant; this.cases = cases;
        }
    }

    public static class SwitchCase extends Node {
        public final Node test; // null = default
        public final List<Node> body;
        public SwitchCase(Node test, List<Node> body) {
            this.test = test; this.body = body;
        }
    }

    public static class ReturnStmt extends Node {
        public final Node value; // may be null
        public ReturnStmt(Node value) { this.value = value; }
    }

    public static class BreakStmt extends Node {}
    public static class ContinueStmt extends Node {}

    public static class FuncDecl extends Node {
        public final String name;
        public final List<String> params;
        public final Node body;
        public final boolean isArrow;
        public FuncDecl(String name, List<String> params, Node body, boolean isArrow) {
            this.name = name; this.params = params; this.body = body; this.isArrow = isArrow;
        }
    }

    public static class TryCatchStmt extends Node {
        public final Node tryBlock;
        public final String catchParam;
        public final Node catchBlock;
        public final Node finallyBlock;
        public TryCatchStmt(Node tryBlock, String catchParam, Node catchBlock, Node finallyBlock) {
            this.tryBlock = tryBlock; this.catchParam = catchParam;
            this.catchBlock = catchBlock; this.finallyBlock = finallyBlock;
        }
    }

    public static class ThrowStmt extends Node {
        public final Node value;
        public ThrowStmt(Node value) { this.value = value; }
    }

    // ─── Expressions ────────────────────────────────────────────────────────────

    public static class Literal extends Node {
        public final Object value; // Number, String, Boolean, null
        public Literal(Object value) { this.value = value; }
    }

    public static class Identifier extends Node {
        public final String name;
        public Identifier(String name) { this.name = name; }
    }

    public static class BinaryExpr extends Node {
        public final String op;
        public final Node left;
        public final Node right;
        public BinaryExpr(String op, Node left, Node right) {
            this.op = op; this.left = left; this.right = right;
        }
    }

    public static class UnaryExpr extends Node {
        public final String op;
        public final Node operand;
        public final boolean prefix;
        public UnaryExpr(String op, Node operand, boolean prefix) {
            this.op = op; this.operand = operand; this.prefix = prefix;
        }
    }

    public static class AssignExpr extends Node {
        public final String op;
        public final Node target;
        public final Node value;
        public AssignExpr(String op, Node target, Node value) {
            this.op = op; this.target = target; this.value = value;
        }
    }

    public static class LogicalExpr extends Node {
        public final String op;
        public final Node left;
        public final Node right;
        public LogicalExpr(String op, Node left, Node right) {
            this.op = op; this.left = left; this.right = right;
        }
    }

    public static class TernaryExpr extends Node {
        public final Node condition;
        public final Node consequent;
        public final Node alternate;
        public TernaryExpr(Node condition, Node consequent, Node alternate) {
            this.condition = condition; this.consequent = consequent; this.alternate = alternate;
        }
    }

    public static class CallExpr extends Node {
        public final Node callee;
        public final List<Node> args;
        public CallExpr(Node callee, List<Node> args) {
            this.callee = callee; this.args = args;
        }
    }

    public static class MemberExpr extends Node {
        public final Node object;
        public final Node property;
        public final boolean computed; // obj[prop] vs obj.prop
        public MemberExpr(Node object, Node property, boolean computed) {
            this.object = object; this.property = property; this.computed = computed;
        }
    }

    public static class ArrayExpr extends Node {
        public final List<Node> elements;
        public ArrayExpr(List<Node> elements) { this.elements = elements; }
    }

    public static class ObjectExpr extends Node {
        public final List<Map.Entry<Node, Node>> properties;
        public ObjectExpr(List<Map.Entry<Node, Node>> properties) { this.properties = properties; }
    }

    public static class SpreadExpr extends Node {
        public final Node argument;
        public SpreadExpr(Node argument) { this.argument = argument; }
    }

    public static class NewExpr extends Node {
        public final Node callee;
        public final List<Node> args;
        public NewExpr(Node callee, List<Node> args) {
            this.callee = callee; this.args = args;
        }
    }

    public static class TypeofExpr extends Node {
        public final Node operand;
        public TypeofExpr(Node operand) { this.operand = operand; }
    }

    public static class TemplateLiteral extends Node {
        public final String raw; // raw string with \u0000 and \u0001 markers
        public TemplateLiteral(String raw) { this.raw = raw; }
    }
}
