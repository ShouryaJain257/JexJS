package com.thunder.js;

import java.util.*;
import java.util.stream.*;

public class Interpreter {

    // Sentinel values
    public static final Object UNDEFINED = new Object() { public String toString() { return "undefined"; } };
    public static final Object BREAK_SIGNAL = new Object();
    public static final Object CONTINUE_SIGNAL = new Object();

    public static class ReturnException extends RuntimeException {
        public final Object value;
        public ReturnException(Object value) { super(null, null, true, false); this.value = value; }
    }

    public static class ThrowException extends RuntimeException {
        public final Object value;
        public ThrowException(Object value) { super(value != null ? jsToString(value) : "null", null, true, false); this.value = value; }
    }

    private final Environment global;
    private final StringBuilder output;

    public Interpreter() {
        this.output = new StringBuilder();
        this.global = new Environment(null);
        setupGlobals();
    }

    public String getOutput() { return output.toString(); }

    private void print(String s) { output.append(s).append("\n"); }

    // ─── Global setup ──────────────────────────────────────────────────────────

    private void setupGlobals() {
        global.define("undefined", UNDEFINED);
        global.define("null", null);
        global.define("NaN", Double.NaN);
        global.define("Infinity", Double.POSITIVE_INFINITY);

        // console
        JSObject console = new JSObject();
        console.set("log", (NativeFunction) args -> {
            if (args.length == 0) { print(""); return UNDEFINED; }
            String msg = Arrays.stream(args).map(Interpreter::consoleFormat).collect(Collectors.joining(" "));
            print(msg);
            return UNDEFINED;
        });
        console.set("error", console.get("log"));
        console.set("warn", console.get("log"));
        global.define("console", console);

        // Math
        JSObject math = new JSObject();
        math.set("PI", Math.PI);
        math.set("E", Math.E);
        math.set("abs", (NativeFunction) args -> Math.abs(toNumber(args[0])));
        math.set("ceil", (NativeFunction) args -> Math.ceil(toNumber(args[0])));
        math.set("floor", (NativeFunction) args -> Math.floor(toNumber(args[0])));
        math.set("round", (NativeFunction) args -> (double) Math.round(toNumber(args[0])));
        math.set("sqrt", (NativeFunction) args -> Math.sqrt(toNumber(args[0])));
        math.set("pow", (NativeFunction) args -> Math.pow(toNumber(args[0]), toNumber(args[1])));
        math.set("max", (NativeFunction) args -> Arrays.stream(args).mapToDouble(Interpreter::toNumber).max().orElse(Double.NEGATIVE_INFINITY));
        math.set("min", (NativeFunction) args -> Arrays.stream(args).mapToDouble(Interpreter::toNumber).min().orElse(Double.POSITIVE_INFINITY));
        math.set("random", (NativeFunction) args -> Math.random());
        math.set("log", (NativeFunction) args -> Math.log(toNumber(args[0])));
        math.set("log2", (NativeFunction) args -> Math.log(toNumber(args[0])) / Math.log(2));
        math.set("log10", (NativeFunction) args -> Math.log10(toNumber(args[0])));
        math.set("sin", (NativeFunction) args -> Math.sin(toNumber(args[0])));
        math.set("cos", (NativeFunction) args -> Math.cos(toNumber(args[0])));
        math.set("tan", (NativeFunction) args -> Math.tan(toNumber(args[0])));
        math.set("trunc", (NativeFunction) args -> { double v = toNumber(args[0]); return v >= 0 ? Math.floor(v) : Math.ceil(v); });
        math.set("sign", (NativeFunction) args -> (double) (int) Math.signum(toNumber(args[0])));
        math.set("hypot", (NativeFunction) args -> Math.hypot(toNumber(args[0]), toNumber(args[1])));
        global.define("Math", math);

        // Number
        JSObject numberObj = new JSObject();
        numberObj.set("isInteger", (NativeFunction) args -> {
            if (!(args[0] instanceof Double)) return false;
            double v = (Double) args[0];
            return !Double.isInfinite(v) && !Double.isNaN(v) && v == Math.floor(v);
        });
        numberObj.set("isFinite", (NativeFunction) args -> {
            if (!(args[0] instanceof Double)) return false;
            return Double.isFinite((Double) args[0]);
        });
        numberObj.set("isNaN", (NativeFunction) args -> {
            if (!(args[0] instanceof Double)) return false;
            return Double.isNaN((Double) args[0]);
        });
        numberObj.set("parseInt", (NativeFunction) args -> {
            try { return (double) Integer.parseInt(jsToString(args[0])); }
            catch (NumberFormatException e) { return Double.NaN; }
        });
        numberObj.set("parseFloat", (NativeFunction) args -> {
            try { return Double.parseDouble(jsToString(args[0])); }
            catch (NumberFormatException e) { return Double.NaN; }
        });
        numberObj.set("MAX_VALUE", Double.MAX_VALUE);
        numberObj.set("MIN_VALUE", Double.MIN_VALUE);
        numberObj.set("POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        numberObj.set("NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY);
        numberObj.set("NaN", Double.NaN);
        global.define("Number", numberObj);

        // parseInt / parseFloat global
        global.define("parseInt", (NativeFunction) args -> {
            if (args.length == 0) return Double.NaN;
            String s = jsToString(args[0]).trim();
            int radix = args.length > 1 ? (int) toNumber(args[1]) : 10;
            if (radix == 0) radix = 10;
            try {
                // Strip non-digit chars from end
                StringBuilder sb = new StringBuilder();
                String digits = "0123456789abcdefghijklmnopqrstuvwxyz".substring(0, radix);
                boolean neg = s.startsWith("-");
                if (neg) s = s.substring(1);
                if (radix == 16 && (s.startsWith("0x") || s.startsWith("0X"))) s = s.substring(2);
                for (char c : s.toLowerCase().toCharArray()) {
                    if (digits.indexOf(c) == -1) break;
                    sb.append(c);
                }
                if (sb.isEmpty()) return Double.NaN;
                return (double) Long.parseLong((neg ? "-" : "") + sb, radix);
            } catch (Exception e) { return Double.NaN; }
        });
        global.define("parseFloat", (NativeFunction) args -> {
            if (args.length == 0) return Double.NaN;
            try { return Double.parseDouble(jsToString(args[0]).trim()); }
            catch (NumberFormatException e) { return Double.NaN; }
        });
        global.define("isNaN", (NativeFunction) args -> {
            if (args.length == 0) return true;
            double v = toNumber(args[0]);
            return Double.isNaN(v);
        });
        global.define("isFinite", (NativeFunction) args -> {
            if (args.length == 0) return false;
            return Double.isFinite(toNumber(args[0]));
        });

        // String constructor
        global.define("String", (NativeFunction) args -> args.length > 0 ? jsToString(args[0]) : "");

        // Boolean constructor
        global.define("Boolean", (NativeFunction) args -> args.length > 0 && isTruthy(args[0]));

        // Array
        JSObject arrayObj = new JSObject();
        arrayObj.set("isArray", (NativeFunction) args -> args.length > 0 && args[0] instanceof JSArray);
        arrayObj.set("from", (NativeFunction) args -> {
            if (args.length == 0) return new JSArray();
            Object src = args[0];
            if (src instanceof JSArray ja) return new JSArray(new ArrayList<>(ja.elements));
            if (src instanceof String s) {
                JSArray arr = new JSArray();
                for (char c : s.toCharArray()) arr.elements.add(String.valueOf(c));
                return arr;
            }
            return new JSArray();
        });
        arrayObj.set("of", (NativeFunction) args -> new JSArray(Arrays.asList(args)));
        global.define("Array", arrayObj);

        // Object
        JSObject objectConstructor = new JSObject();
        objectConstructor.set("keys", (NativeFunction) args -> {
            if (args.length == 0 || args[0] == null) return new JSArray();
            JSObject obj = args[0] instanceof JSObject ? (JSObject) args[0] : new JSObject();
            JSArray result = new JSArray();
            if (obj instanceof JSArray ja) {
                for (int i = 0; i < ja.elements.size(); i++) result.elements.add(String.valueOf(i));
            } else {
                result.elements.addAll(obj.props.keySet());
            }
            return result;
        });
        objectConstructor.set("values", (NativeFunction) args -> {
            if (args.length == 0 || args[0] == null) return new JSArray();
            JSObject obj = (JSObject) args[0];
            JSArray result = new JSArray();
            if (obj instanceof JSArray ja) {
                result.elements.addAll(ja.elements);
            } else {
                result.elements.addAll(obj.props.values());
            }
            return result;
        });
        objectConstructor.set("entries", (NativeFunction) args -> {
            if (args.length == 0 || args[0] == null) return new JSArray();
            JSObject obj = (JSObject) args[0];
            JSArray result = new JSArray();
            if (obj instanceof JSArray ja) {
                for (int i = 0; i < ja.elements.size(); i++) {
                    JSArray pair = new JSArray();
                    pair.elements.add(String.valueOf(i));
                    pair.elements.add(ja.elements.get(i));
                    result.elements.add(pair);
                }
            } else {
                for (var entry : obj.props.entrySet()) {
                    JSArray pair = new JSArray();
                    pair.elements.add(entry.getKey());
                    pair.elements.add(entry.getValue());
                    result.elements.add(pair);
                }
            }
            return result;
        });
        objectConstructor.set("assign", (NativeFunction) args -> {
            if (args.length == 0) return UNDEFINED;
            JSObject target = args[0] instanceof JSObject ? (JSObject) args[0] : new JSObject();
            for (int i = 1; i < args.length; i++) {
                if (args[i] instanceof JSObject src) src.props.forEach(target::set);
            }
            return target;
        });
        objectConstructor.set("freeze", (NativeFunction) args -> args.length > 0 ? args[0] : UNDEFINED);
        global.define("Object", objectConstructor);

        // JSON
        JSObject json = new JSObject();
        json.set("stringify", (NativeFunction) args -> jsonStringify(args.length > 0 ? args[0] : UNDEFINED));
        json.set("parse", (NativeFunction) args -> jsonParse(args.length > 0 ? jsToString(args[0]) : "null"));
        global.define("JSON", json);

        // Date
        global.define("Date", (NativeFunction) args -> {
            JSObject date = new JSObject();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            date.set("getFullYear", (NativeFunction) a -> (double) now.getYear());
            date.set("getMonth", (NativeFunction) a -> (double) now.getMonthValue() - 1);
            date.set("getDate", (NativeFunction) a -> (double) now.getDayOfMonth());
            date.set("getDay", (NativeFunction) a -> (double) now.getDayOfWeek().getValue() % 7);
            date.set("getHours", (NativeFunction) a -> (double) now.getHour());
            date.set("getMinutes", (NativeFunction) a -> (double) now.getMinute());
            date.set("getSeconds", (NativeFunction) a -> (double) now.getSecond());
            date.set("getTime", (NativeFunction) a -> (double) System.currentTimeMillis());
            date.set("toLocaleDateString", (NativeFunction) a -> now.toLocalDate().toString());
            date.set("toLocaleTimeString", (NativeFunction) a -> now.toLocalTime().toString());
            date.set("toLocaleString", (NativeFunction) a -> now.toString());
            date.set("toString", (NativeFunction) a -> now.toString());
            return date;
        });

        // setTimeout/setInterval stubs (no-op for synchronous execution)
        global.define("setTimeout", (NativeFunction) args -> { return 0.0; });
        global.define("clearTimeout", (NativeFunction) args -> UNDEFINED);
        global.define("setInterval", (NativeFunction) args -> 0.0);
        global.define("clearInterval", (NativeFunction) args -> UNDEFINED);

        // Error constructor
        global.define("Error", (NativeFunction) args -> {
            JSObject err = new JSObject();
            err.set("message", args.length > 0 ? jsToString(args[0]) : "");
            err.set("name", "Error");
            return err;
        });
        global.define("TypeError", (NativeFunction) args -> {
            JSObject err = new JSObject();
            err.set("message", args.length > 0 ? jsToString(args[0]) : "");
            err.set("name", "TypeError");
            return err;
        });
        global.define("RangeError", (NativeFunction) args -> {
            JSObject err = new JSObject();
            err.set("message", args.length > 0 ? jsToString(args[0]) : "");
            err.set("name", "RangeError");
            return err;
        });
    }

    // ─── Execute ────────────────────────────────────────────────────────────────

    public void execute(Node.Program program) {
        // First pass: hoist function declarations
        for (Node stmt : program.body) {
            if (stmt instanceof Node.FuncDecl fd && fd.name != null) {
                global.define(fd.name, new JSFunction(fd.name, fd.params, fd.body, global, fd.isArrow));
            }
        }
        for (Node stmt : program.body) {
            execStatement(stmt, global);
        }
    }

    private Object execStatement(Node node, Environment env) {
        if (node instanceof Node.Program p) {
            for (Node stmt : p.body) { Object r = execStatement(stmt, env); if (r == BREAK_SIGNAL || r == CONTINUE_SIGNAL) return r; }
            return UNDEFINED;
        }
        if (node instanceof Node.BlockStmt b) {
            Environment blockEnv = new Environment(env);
            // hoist functions in block
            for (Node stmt : b.body) {
                if (stmt instanceof Node.FuncDecl fd && fd.name != null) {
                    blockEnv.define(fd.name, new JSFunction(fd.name, fd.params, fd.body, blockEnv, fd.isArrow));
                }
            }
            for (Node stmt : b.body) {
                Object r = execStatement(stmt, blockEnv);
                if (r == BREAK_SIGNAL || r == CONTINUE_SIGNAL) return r;
            }
            return UNDEFINED;
        }
        if (node instanceof Node.VarDecl vd) {
            Object value = vd.init != null ? eval(vd.init, env) : UNDEFINED;
            env.define(vd.name, value);
            return UNDEFINED;
        }
        if (node instanceof Node.VarDestructureDecl vdd) {
            Object obj = eval(vdd.init, env);
            for (String name : vdd.names) {
                Object v = UNDEFINED;
                if (obj instanceof JSObject jo) v = jo.get(name);
                env.define(name, v);
            }
            return UNDEFINED;
        }
        if (node instanceof Node.ArrayDestructureDecl add) {
            Object arr = eval(add.init, env);
            for (int i = 0; i < add.names.size(); i++) {
                String name = add.names.get(i);
                if (name == null) continue;
                Object v = UNDEFINED;
                if (arr instanceof JSArray ja && i < ja.elements.size()) v = ja.elements.get(i);
                env.define(name, v);
            }
            return UNDEFINED;
        }
        if (node instanceof Node.FuncDecl fd) {
            if (fd.name != null) {
                env.define(fd.name, new JSFunction(fd.name, fd.params, fd.body, env, fd.isArrow));
            }
            return UNDEFINED;
        }
        if (node instanceof Node.ExprStmt es) { eval(es.expr, env); return UNDEFINED; }
        if (node instanceof Node.IfStmt is) {
            if (isTruthy(eval(is.condition, env))) return execStatement(is.consequent, env);
            else if (is.alternate != null) return execStatement(is.alternate, env);
            return UNDEFINED;
        }
        if (node instanceof Node.WhileStmt ws) {
            while (isTruthy(eval(ws.condition, env))) {
                Object r = execStatement(ws.body, env);
                if (r == BREAK_SIGNAL) break;
                // CONTINUE_SIGNAL: just continue to next iteration
            }
            return UNDEFINED;
        }
        if (node instanceof Node.DoWhileStmt dws) {
            do {
                Object r = execStatement(dws.body, env);
                if (r == BREAK_SIGNAL) break;
            } while (isTruthy(eval(dws.condition, env)));
            return UNDEFINED;
        }
        if (node instanceof Node.ForStmt fs) {
            Environment forEnv = new Environment(env);
            if (fs.init != null) execStatement(fs.init, forEnv);
            while (fs.test == null || isTruthy(eval(fs.test, forEnv))) {
                Object r = execStatement(fs.body, forEnv);
                if (r == BREAK_SIGNAL) break;
                if (fs.update != null) eval(fs.update, forEnv);
            }
            return UNDEFINED;
        }
        if (node instanceof Node.ForOfStmt fos) {
            Object iterable = eval(fos.iterable, env);
            List<Object> items = toIterable(iterable);
            for (Object item : items) {
                Environment loopEnv = new Environment(env);
                loopEnv.define(fos.varName, item);
                Object r = execStatement(fos.body, loopEnv);
                if (r == BREAK_SIGNAL) break;
            }
            return UNDEFINED;
        }
        if (node instanceof Node.ForInStmt fis) {
            Object obj = eval(fis.object, env);
            List<String> keys = new ArrayList<>();
            if (obj instanceof JSArray ja) {
                for (int i = 0; i < ja.elements.size(); i++) keys.add(String.valueOf(i));
            } else if (obj instanceof JSObject jo) {
                keys.addAll(jo.props.keySet());
            }
            for (String key : keys) {
                Environment loopEnv = new Environment(env);
                loopEnv.define(fis.varName, key);
                Object r = execStatement(fis.body, loopEnv);
                if (r == BREAK_SIGNAL) break;
            }
            return UNDEFINED;
        }
        if (node instanceof Node.SwitchStmt ss) {
            Object disc = eval(ss.discriminant, env);
            boolean matched = false;
            for (Node.SwitchCase sc : ss.cases) {
                if (!matched && sc.test != null) {
                    Object testVal = eval(sc.test, env);
                    matched = strictEquals(disc, testVal);
                } else if (sc.test == null && !matched) {
                    matched = true;
                }
                if (matched) {
                    for (Node stmt : sc.body) {
                        Object r = execStatement(stmt, env);
                        if (r == BREAK_SIGNAL) return UNDEFINED;
                    }
                }
            }
            return UNDEFINED;
        }
        if (node instanceof Node.ReturnStmt rs) {
            Object val = rs.value != null ? eval(rs.value, env) : UNDEFINED;
            throw new ReturnException(val);
        }
        if (node instanceof Node.BreakStmt) return BREAK_SIGNAL;
        if (node instanceof Node.ContinueStmt) return CONTINUE_SIGNAL;
        if (node instanceof Node.ThrowStmt ts) {
            throw new ThrowException(eval(ts.value, env));
        }
        if (node instanceof Node.TryCatchStmt tcs) {
            try {
                execStatement(tcs.tryBlock, env);
            } catch (ThrowException te) {
                if (tcs.catchBlock != null) {
                    Environment catchEnv = new Environment(env);
                    if (tcs.catchParam != null) catchEnv.define(tcs.catchParam, te.value);
                    execStatement(tcs.catchBlock, catchEnv);
                }
            } catch (RuntimeException re) {
                if (tcs.catchBlock != null) {
                    Environment catchEnv = new Environment(env);
                    if (tcs.catchParam != null) {
                        JSObject err = new JSObject();
                        err.set("message", re.getMessage() != null ? re.getMessage() : "RuntimeError");
                        catchEnv.define(tcs.catchParam, err);
                    }
                    execStatement(tcs.catchBlock, catchEnv);
                }
            } finally {
                if (tcs.finallyBlock != null) execStatement(tcs.finallyBlock, env);
            }
            return UNDEFINED;
        }
        return UNDEFINED;
    }

    // ─── Eval ──────────────────────────────────────────────────────────────────

    public Object eval(Node node, Environment env) {
        if (node instanceof Node.Literal lit) return lit.value;

        if (node instanceof Node.TemplateLiteral tl) {
            return evalTemplateLiteral(tl.raw, env);
        }

        if (node instanceof Node.Identifier id) {
            String name = id.name;
            if (name.equals("undefined")) return UNDEFINED;
            if (name.equals("Infinity")) return Double.POSITIVE_INFINITY;
            if (name.equals("NaN")) return Double.NaN;
            return env.get(name);
        }

        if (node instanceof Node.TypeofExpr te) {
            Object val;
            try { val = eval(te.operand, env); }
            catch (Exception e) { return "undefined"; }
            return jsTypeof(val);
        }

        if (node instanceof Node.BinaryExpr be) {
            Object left = eval(be.left, env);
            Object right = eval(be.right, env);
            return evalBinary(be.op, left, right);
        }

        if (node instanceof Node.LogicalExpr le) {
            Object left = eval(le.left, env);
            if (le.op.equals("&&")) return isTruthy(left) ? eval(le.right, env) : left;
            if (le.op.equals("||")) return isTruthy(left) ? left : eval(le.right, env);
            if (le.op.equals("??")) return (left == null || left == UNDEFINED) ? eval(le.right, env) : left;
            return UNDEFINED;
        }

        if (node instanceof Node.UnaryExpr ue) return evalUnary(ue, env);

        if (node instanceof Node.AssignExpr ae) return evalAssign(ae, env);

        if (node instanceof Node.TernaryExpr te) {
            return isTruthy(eval(te.condition, env)) ? eval(te.consequent, env) : eval(te.alternate, env);
        }

        if (node instanceof Node.ArrayExpr ae) {
            JSArray arr = new JSArray();
            for (Node el : ae.elements) {
                if (el instanceof Node.SpreadExpr se) {
                    Object spread = eval(se.argument, env);
                    if (spread instanceof JSArray sa) arr.elements.addAll(sa.elements);
                    else if (spread instanceof String s) { for (char c : s.toCharArray()) arr.elements.add(String.valueOf(c)); }
                } else {
                    arr.elements.add(eval(el, env));
                }
            }
            return arr;
        }

        if (node instanceof Node.ObjectExpr oe) {
            JSObject obj = new JSObject();
            for (var entry : oe.properties) {
                String key = jsToString(eval(entry.getKey(), env));
                Object val = eval(entry.getValue(), env);
                obj.set(key, val);
            }
            return obj;
        }

        if (node instanceof Node.MemberExpr me) {
            Object obj = eval(me.object, env);
            String key;
            if (me.computed) {
                Object keyVal = eval(me.property, env);
                key = (keyVal instanceof Double d && d == Math.floor(d)) ? String.valueOf(d.intValue()) : jsToString(keyVal);
            } else {
                key = ((Node.Literal) me.property).value.toString();
            }
            return getMember(obj, key, env);
        }

        if (node instanceof Node.CallExpr ce) {
            return evalCall(ce, env);
        }

        if (node instanceof Node.NewExpr ne) {
            return evalNew(ne, env);
        }

        if (node instanceof Node.FuncDecl fd) {
            JSFunction fn = new JSFunction(fd.name, fd.params, fd.body, env, fd.isArrow);
            if (fd.name != null) env.define(fd.name, fn);
            return fn;
        }

        if (node instanceof Node.SpreadExpr se) {
            return eval(se.argument, env);
        }

        throw new RuntimeException("Cannot eval node: " + node.getClass().getSimpleName());
    }

    // ─── Member access ─────────────────────────────────────────────────────────

    private Object getMember(Object obj, String key, Environment env) {
        if (obj == null || obj == UNDEFINED) {
            throw new RuntimeException("Cannot read properties of " + (obj == null ? "null" : "undefined") + " (reading '" + key + "')");
        }
        if (obj instanceof String s) return getStringMethod(s, key, env);
        if (obj instanceof Double d) return getNumberMethod(d, key);
        if (obj instanceof JSArray ja) {
            Object arrMethod = getArrayMethod(ja, key, env);
            if (arrMethod != UNDEFINED) return arrMethod;
            return ja.get(key);
        }
        if (obj instanceof JSObject jo) return jo.get(key);
        if (obj instanceof Boolean) {
            if (key.equals("toString")) return (NativeFunction) args -> obj.toString();
        }
        return UNDEFINED;
    }

    // ─── Call ──────────────────────────────────────────────────────────────────

    private Object evalCall(Node.CallExpr ce, Environment env) {
        // Special handling for method calls
        if (ce.callee instanceof Node.MemberExpr me) {
            Object thisVal = eval(me.object, env);
            String methodName;
            if (me.computed) {
                Object keyVal = eval(me.property, env);
                methodName = (keyVal instanceof Double d && d == Math.floor(d)) ? String.valueOf(d.intValue()) : jsToString(keyVal);
            } else {
                methodName = ((Node.Literal) me.property).value.toString();
            }
            Object[] args = evalArgs(ce.args, env);
            return callMethod(thisVal, methodName, args, env);
        }

        Object callee = eval(ce.callee, env);
        Object[] args = evalArgs(ce.args, env);
        return callFunction(callee, args, env, UNDEFINED);
    }

    private Object[] evalArgs(List<Node> argNodes, Environment env) {
        List<Object> args = new ArrayList<>();
        for (Node argNode : argNodes) {
            if (argNode instanceof Node.SpreadExpr se) {
                Object spread = eval(se.argument, env);
                if (spread instanceof JSArray ja) args.addAll(ja.elements);
                else args.add(spread);
            } else {
                args.add(eval(argNode, env));
            }
        }
        return args.toArray();
    }

    public Object callFunction(Object fn, Object[] args, Environment env, Object thisVal) {
        if (fn instanceof NativeFunction nf) {
            if (args.length == 0) args = new Object[]{};
            return nf.call(args);
        }
        if (fn instanceof JSFunction jsf) {
            Environment funcEnv = new Environment(jsf.closure);
            funcEnv.define("this", thisVal);
            funcEnv.define("arguments", new JSArray(Arrays.asList(args)));
            // Bind params
            for (int i = 0; i < jsf.params.size(); i++) {
                String param = jsf.params.get(i);
                if (param.startsWith("...")) {
                    // rest param
                    JSArray rest = new JSArray();
                    for (int j = i; j < args.length; j++) rest.elements.add(args[j]);
                    funcEnv.define(param.substring(3), rest);
                    break;
                }
                funcEnv.define(param, i < args.length ? args[i] : UNDEFINED);
            }
            // Hoist function declarations inside body
            if (jsf.body instanceof Node.BlockStmt bs) {
                for (Node stmt : bs.body) {
                    if (stmt instanceof Node.FuncDecl fd && fd.name != null) {
                        funcEnv.define(fd.name, new JSFunction(fd.name, fd.params, fd.body, funcEnv, fd.isArrow));
                    }
                }
            }
            try {
                execStatement(jsf.body, funcEnv);
                return UNDEFINED;
            } catch (ReturnException re) {
                return re.value;
            }
        }
        if (fn == null || fn == UNDEFINED) throw new RuntimeException("TypeError: " + fn + " is not a function");
        throw new RuntimeException("TypeError: " + jsToString(fn) + " is not a function");
    }

    private Object callMethod(Object thisVal, String method, Object[] args, Environment env) {
        if (thisVal instanceof String s) return callStringMethod(s, method, args, env);
        if (thisVal instanceof JSArray ja) return callArrayMethod(ja, method, args, env);
        if (thisVal instanceof JSObject jo) {
            Object fn = jo.get(method);
            if (fn instanceof NativeFunction nf) return nf.call(args);
            if (fn instanceof JSFunction jsf) return callFunction(jsf, args, env, thisVal);
            throw new RuntimeException("TypeError: " + method + " is not a function on " + jsToString(thisVal));
        }
        if (thisVal instanceof Double d) return callNumberMethod(d, method, args);
        if (thisVal instanceof Boolean b) {
            if (method.equals("toString")) return b.toString();
        }
        throw new RuntimeException("TypeError: Cannot call " + method + " on " + jsToString(thisVal));
    }

    private Object evalNew(Node.NewExpr ne, Environment env) {
        Object callee = eval(ne.callee, env);
        Object[] args = evalArgs(ne.args, env);
        
        if (callee instanceof NativeFunction nf) {
            return nf.call(args);
        }
        if (callee instanceof JSFunction jsf) {
            JSObject instance = new JSObject();
            instance.set("constructor", jsf);
            callFunction(jsf, args, env, instance);
            return instance;
        }
        throw new RuntimeException("TypeError: " + jsToString(callee) + " is not a constructor");
    }

    // ─── String methods ────────────────────────────────────────────────────────

    private Object getStringMethod(String s, String key, Environment env) {
        return switch (key) {
            case "length" -> (double) s.length();
            case "charAt" -> (NativeFunction) args -> {
                int i = (int) toNumber(args[0]);
                return (i >= 0 && i < s.length()) ? String.valueOf(s.charAt(i)) : "";
            };
            case "charCodeAt" -> (NativeFunction) args -> {
                int i = args.length > 0 ? (int) toNumber(args[0]) : 0;
                return (i >= 0 && i < s.length()) ? (double) s.charAt(i) : Double.NaN;
            };
            case "indexOf" -> (NativeFunction) args -> {
                String search = args.length > 0 ? jsToString(args[0]) : "undefined";
                int from = args.length > 1 ? (int) toNumber(args[1]) : 0;
                return (double) s.indexOf(search, from);
            };
            case "lastIndexOf" -> (NativeFunction) args -> {
                String search = jsToString(args[0]);
                return (double) s.lastIndexOf(search);
            };
            case "includes" -> (NativeFunction) args -> s.contains(jsToString(args[0]));
            case "startsWith" -> (NativeFunction) args -> {
                int from = args.length > 1 ? (int) toNumber(args[1]) : 0;
                return s.substring(from).startsWith(jsToString(args[0]));
            };
            case "endsWith" -> (NativeFunction) args -> s.endsWith(jsToString(args[0]));
            case "slice" -> (NativeFunction) args -> {
                int start = args.length > 0 ? normalizeIndex((int) toNumber(args[0]), s.length()) : 0;
                int end = args.length > 1 ? normalizeIndex((int) toNumber(args[1]), s.length()) : s.length();
                if (start > end) return "";
                return s.substring(Math.max(0, start), Math.min(s.length(), end));
            };
            case "substring" -> (NativeFunction) args -> {
                int start = args.length > 0 ? Math.max(0, (int) toNumber(args[0])) : 0;
                int end = args.length > 1 ? Math.max(0, (int) toNumber(args[1])) : s.length();
                if (start > end) { int tmp = start; start = end; end = tmp; }
                return s.substring(Math.min(start, s.length()), Math.min(end, s.length()));
            };
            case "toUpperCase", "toLocaleUpperCase" -> (NativeFunction) args -> s.toUpperCase();
            case "toLowerCase", "toLocaleLowerCase" -> (NativeFunction) args -> s.toLowerCase();
            case "trim" -> (NativeFunction) args -> s.trim();
            case "trimStart", "trimLeft" -> (NativeFunction) args -> s.stripLeading();
            case "trimEnd", "trimRight" -> (NativeFunction) args -> s.stripTrailing();
            case "split" -> (NativeFunction) args -> {
                JSArray result = new JSArray();
                if (args.length == 0) { result.elements.add(s); return result; }
                String sep = jsToString(args[0]);
                int limit = args.length > 1 ? (int) toNumber(args[1]) : Integer.MAX_VALUE;
                if (sep.isEmpty()) {
                    for (char c : s.toCharArray()) { if (result.elements.size() >= limit) break; result.elements.add(String.valueOf(c)); }
                } else {
                    String[] parts = s.split(java.util.regex.Pattern.quote(sep), -1);
                    for (String p : parts) { if (result.elements.size() >= limit) break; result.elements.add(p); }
                }
                return result;
            };
            case "replace" -> (NativeFunction) args -> {
                String search = jsToString(args[0]);
                Object repl = args[1];
                String replacement = jsToString(repl);
                return s.replaceFirst(java.util.regex.Pattern.quote(search), java.util.regex.Matcher.quoteReplacement(replacement));
            };
            case "replaceAll" -> (NativeFunction) args -> {
                String search = jsToString(args[0]);
                String replacement = jsToString(args[1]);
                return s.replace(search, replacement);
            };
            case "repeat" -> (NativeFunction) args -> s.repeat((int) toNumber(args[0]));
            case "padStart" -> (NativeFunction) args -> {
                int len = (int) toNumber(args[0]);
                String pad = args.length > 1 ? jsToString(args[1]) : " ";
                if (s.length() >= len) return s;
                StringBuilder sb = new StringBuilder();
                while (sb.length() + s.length() < len) sb.append(pad);
                return sb.substring(0, len - s.length()) + s;
            };
            case "padEnd" -> (NativeFunction) args -> {
                int len = (int) toNumber(args[0]);
                String pad = args.length > 1 ? jsToString(args[1]) : " ";
                if (s.length() >= len) return s;
                StringBuilder sb = new StringBuilder(s);
                while (sb.length() < len) sb.append(pad);
                return sb.substring(0, len);
            };
            case "concat" -> (NativeFunction) args -> {
                StringBuilder sb = new StringBuilder(s);
                for (Object a : args) sb.append(jsToString(a));
                return sb.toString();
            };
            case "at" -> (NativeFunction) args -> {
                int i = (int) toNumber(args[0]);
                if (i < 0) i = s.length() + i;
                return (i >= 0 && i < s.length()) ? String.valueOf(s.charAt(i)) : UNDEFINED;
            };
            case "toString", "valueOf" -> (NativeFunction) args -> s;
            default -> UNDEFINED;
        };
    }

    private Object callStringMethod(String s, String method, Object[] args, Environment env) {
        Object m = getStringMethod(s, method, env);
        if (m instanceof NativeFunction nf) return nf.call(args);
        throw new RuntimeException("TypeError: " + method + " is not a function");
    }

    // ─── Number methods ────────────────────────────────────────────────────────

    private Object getNumberMethod(Double d, String key) {
        return switch (key) {
            case "toFixed" -> (NativeFunction) args -> {
                int digits = args.length > 0 ? (int) toNumber(args[0]) : 0;
                return String.format("%." + digits + "f", d);
            };
            case "toString" -> (NativeFunction) args -> {
                int radix = args.length > 0 ? (int) toNumber(args[0]) : 10;
                if (radix == 10) return numberToString(d);
                return Long.toString(d.longValue(), radix);
            };
            case "toLocaleString" -> (NativeFunction) args -> numberToString(d);
            case "toPrecision" -> (NativeFunction) args -> {
                int prec = args.length > 0 ? (int) toNumber(args[0]) : 1;
                return String.format("%." + prec + "g", d);
            };
            default -> UNDEFINED;
        };
    }

    private Object callNumberMethod(Double d, String method, Object[] args) {
        Object m = getNumberMethod(d, method);
        if (m instanceof NativeFunction nf) return nf.call(args);
        throw new RuntimeException(method + " is not a function on number");
    }

    // ─── Array methods ─────────────────────────────────────────────────────────

    private Object getArrayMethod(JSArray ja, String key, Environment env) {
        return switch (key) {
            case "push" -> (NativeFunction) args -> { for (Object a : args) ja.elements.add(a); return (double) ja.elements.size(); };
            case "pop" -> (NativeFunction) args -> ja.elements.isEmpty() ? UNDEFINED : ja.elements.remove(ja.elements.size() - 1);
            case "shift" -> (NativeFunction) args -> ja.elements.isEmpty() ? UNDEFINED : ja.elements.remove(0);
            case "unshift" -> (NativeFunction) args -> { for (int i = args.length - 1; i >= 0; i--) ja.elements.add(0, args[i]); return (double) ja.elements.size(); };
            case "join" -> (NativeFunction) args -> ja.join(args.length > 0 ? jsToString(args[0]) : ",");
            case "reverse" -> (NativeFunction) args -> { Collections.reverse(ja.elements); return ja; };
            case "sort" -> (NativeFunction) args -> {
                if (args.length > 0 && args[0] instanceof JSFunction compareFn) {
                    ja.elements.sort((a, b) -> {
                        Object r = callFunction(compareFn, new Object[]{a, b}, env, UNDEFINED);
                        return (int) Math.signum(toNumber(r));
                    });
                } else if (args.length > 0 && args[0] instanceof NativeFunction nf) {
                    ja.elements.sort((a, b) -> {
                        Object r = nf.call(new Object[]{a, b});
                        return (int) Math.signum(toNumber(r));
                    });
                } else {
                    ja.elements.sort((a, b) -> jsToString(a).compareTo(jsToString(b)));
                }
                return ja;
            };
            case "slice" -> (NativeFunction) args -> {
                int len = ja.elements.size();
                int start = args.length > 0 ? normalizeIndex((int) toNumber(args[0]), len) : 0;
                int end = args.length > 1 ? normalizeIndex((int) toNumber(args[1]), len) : len;
                start = Math.max(0, Math.min(start, len));
                end = Math.max(0, Math.min(end, len));
                return new JSArray(ja.elements.subList(start, Math.max(start, end)));
            };
            case "splice" -> (NativeFunction) args -> UNDEFINED; // handled correctly in callArrayMethod
            case "concat" -> (NativeFunction) args -> {
                JSArray result = new JSArray(new ArrayList<>(ja.elements));
                for (Object a : args) {
                    if (a instanceof JSArray sa) result.elements.addAll(sa.elements);
                    else result.elements.add(a);
                }
                return result;
            };
            case "indexOf" -> (NativeFunction) args -> {
                Object target = args[0];
                int from = args.length > 1 ? (int) toNumber(args[1]) : 0;
                for (int i = from; i < ja.elements.size(); i++) {
                    if (strictEquals(ja.elements.get(i), target)) return (double) i;
                }
                return -1.0;
            };
            case "lastIndexOf" -> (NativeFunction) args -> {
                Object target = args[0];
                for (int i = ja.elements.size() - 1; i >= 0; i--) {
                    if (strictEquals(ja.elements.get(i), target)) return (double) i;
                }
                return -1.0;
            };
            case "includes" -> (NativeFunction) args -> {
                Object target = args[0];
                for (Object el : ja.elements) if (strictEquals(el, target)) return true;
                return false;
            };
            case "find" -> (NativeFunction) args -> {
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object el = ja.elements.get(i);
                    Object r = callFunction(args[0], new Object[]{el, (double) i, ja}, env, UNDEFINED);
                    if (isTruthy(r)) return el;
                }
                return UNDEFINED;
            };
            case "findIndex" -> (NativeFunction) args -> {
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object el = ja.elements.get(i);
                    Object r = callFunction(args[0], new Object[]{el, (double) i, ja}, env, UNDEFINED);
                    if (isTruthy(r)) return (double) i;
                }
                return -1.0;
            };
            case "map" -> (NativeFunction) args -> {
                JSArray result = new JSArray();
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object el = ja.elements.get(i);
                    result.elements.add(callFunction(args[0], new Object[]{el, (double) i, ja}, env, UNDEFINED));
                }
                return result;
            };
            case "filter" -> (NativeFunction) args -> {
                JSArray result = new JSArray();
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object el = ja.elements.get(i);
                    Object r = callFunction(args[0], new Object[]{el, (double) i, ja}, env, UNDEFINED);
                    if (isTruthy(r)) result.elements.add(el);
                }
                return result;
            };
            case "reduce" -> (NativeFunction) args -> {
                Object acc = args.length > 1 ? args[1] : (ja.elements.isEmpty() ? UNDEFINED : ja.elements.get(0));
                int start = args.length > 1 ? 0 : 1;
                for (int i = start; i < ja.elements.size(); i++) {
                    acc = callFunction(args[0], new Object[]{acc, ja.elements.get(i), (double) i, ja}, env, UNDEFINED);
                }
                return acc;
            };
            case "reduceRight" -> (NativeFunction) args -> {
                int last = ja.elements.size() - 1;
                Object acc = args.length > 1 ? args[1] : (ja.elements.isEmpty() ? UNDEFINED : ja.elements.get(last--));
                for (int i = last; i >= 0; i--) {
                    acc = callFunction(args[0], new Object[]{acc, ja.elements.get(i), (double) i, ja}, env, UNDEFINED);
                }
                return acc;
            };
            case "forEach" -> (NativeFunction) args -> {
                for (int i = 0; i < ja.elements.size(); i++) {
                    callFunction(args[0], new Object[]{ja.elements.get(i), (double) i, ja}, env, UNDEFINED);
                }
                return UNDEFINED;
            };
            case "some" -> (NativeFunction) args -> {
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object r = callFunction(args[0], new Object[]{ja.elements.get(i), (double) i, ja}, env, UNDEFINED);
                    if (isTruthy(r)) return true;
                }
                return false;
            };
            case "every" -> (NativeFunction) args -> {
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object r = callFunction(args[0], new Object[]{ja.elements.get(i), (double) i, ja}, env, UNDEFINED);
                    if (!isTruthy(r)) return false;
                }
                return true;
            };
            case "flat" -> (NativeFunction) args -> {
                int depth = args.length > 0 ? (int) toNumber(args[0]) : 1;
                JSArray result = new JSArray();
                flattenInto(result.elements, ja.elements, depth);
                return result;
            };
            case "flatMap" -> (NativeFunction) args -> {
                JSArray mapped = new JSArray();
                for (int i = 0; i < ja.elements.size(); i++) {
                    Object r = callFunction(args[0], new Object[]{ja.elements.get(i), (double) i, ja}, env, UNDEFINED);
                    if (r instanceof JSArray ra) mapped.elements.addAll(ra.elements);
                    else mapped.elements.add(r);
                }
                return mapped;
            };
            case "fill" -> (NativeFunction) args -> {
                Object value = args.length > 0 ? args[0] : UNDEFINED;
                int start = args.length > 1 ? (int) toNumber(args[1]) : 0;
                int end = args.length > 2 ? (int) toNumber(args[2]) : ja.elements.size();
                for (int i = start; i < end && i < ja.elements.size(); i++) ja.elements.set(i, value);
                return ja;
            };
            case "keys" -> (NativeFunction) args -> {
                JSArray result = new JSArray();
                for (int i = 0; i < ja.elements.size(); i++) result.elements.add((double) i);
                return result;
            };
            case "values" -> (NativeFunction) args -> new JSArray(new ArrayList<>(ja.elements));
            case "at" -> (NativeFunction) args -> {
                int i = (int) toNumber(args[0]);
                if (i < 0) i = ja.elements.size() + i;
                return (i >= 0 && i < ja.elements.size()) ? ja.elements.get(i) : UNDEFINED;
            };
            case "toString" -> (NativeFunction) args -> ja.join(",");
            default -> UNDEFINED;
        };
    }

    private void flattenInto(List<Object> target, List<Object> source, int depth) {
        for (Object el : source) {
            if (depth > 0 && el instanceof JSArray nested) {
                flattenInto(target, nested.elements, depth - 1);
            } else {
                target.add(el);
            }
        }
    }

    private Object callArrayMethod(JSArray ja, String method, Object[] args, Environment env) {
        // Handle splice properly here
        if (method.equals("splice")) {
            int len = ja.elements.size();
            int start = args.length > 0 ? normalizeIndex((int) toNumber(args[0]), len) : 0;
            start = Math.max(0, Math.min(start, len));
            int deleteCount = args.length > 1 ? Math.min((int) toNumber(args[1]), len - start) : len - start;
            deleteCount = Math.max(0, deleteCount);
            JSArray removed = new JSArray(new ArrayList<>(ja.elements.subList(start, start + deleteCount)));
            for (int i = 0; i < deleteCount; i++) ja.elements.remove(start);
            for (int i = 2; i < args.length; i++) ja.elements.add(start + (i - 2), args[i]);
            return removed;
        }
        Object m = getArrayMethod(ja, method, env);
        if (m instanceof NativeFunction nf) return nf.call(args);
        // Check props
        Object fn = ja.props.get(method);
        if (fn instanceof JSFunction jsf) return callFunction(jsf, args, env, ja);
        if (fn instanceof NativeFunction nf) return nf.call(args);
        throw new RuntimeException("TypeError: " + method + " is not a function");
    }

    // ─── Unary / Binary / Assign ───────────────────────────────────────────────

    private Object evalUnary(Node.UnaryExpr ue, Environment env) {
        if (ue.op.equals("++") || ue.op.equals("--")) {
            Object current = eval(ue.operand, env);
            double num = toNumber(current);
            double updated = ue.op.equals("++") ? num + 1 : num - 1;
            assignTo(ue.operand, updated, env);
            return ue.prefix ? updated : num;
        }
        Object val = eval(ue.operand, env);
        return switch (ue.op) {
            case "-" -> -toNumber(val);
            case "+" -> toNumber(val);
            case "!" -> !isTruthy(val);
            case "~" -> (double) ~((long) toNumber(val));
            default -> UNDEFINED;
        };
    }

    private void assignTo(Node target, Object value, Environment env) {
        if (target instanceof Node.Identifier id) {
            env.set(id.name, value);
        } else if (target instanceof Node.MemberExpr me) {
            Object obj = eval(me.object, env);
            String key;
            if (me.computed) {
                Object keyVal = eval(me.property, env);
                key = (keyVal instanceof Double d && d == Math.floor(d)) ? String.valueOf(d.intValue()) : jsToString(keyVal);
            } else {
                key = ((Node.Literal) me.property).value.toString();
            }
            if (obj instanceof JSObject jo) jo.set(key, value);
        }
    }

    private Object evalAssign(Node.AssignExpr ae, Environment env) {
        if (ae.op.equals("=")) {
            Object val = eval(ae.value, env);
            assignTo(ae.target, val, env);
            return val;
        }
        Object current = eval(ae.target, env);
        Object right = eval(ae.value, env);
        Object result = switch (ae.op) {
            case "+=" -> jsAdd(current, right);
            case "-=" -> toNumber(current) - toNumber(right);
            case "*=" -> toNumber(current) * toNumber(right);
            case "/=" -> toNumber(current) / toNumber(right);
            case "%=" -> toNumber(current) % toNumber(right);
            default -> UNDEFINED;
        };
        assignTo(ae.target, result, env);
        return result;
    }

    private Object evalBinary(String op, Object left, Object right) {
        return switch (op) {
            case "+" -> jsAdd(left, right);
            case "-" -> toNumber(left) - toNumber(right);
            case "*" -> toNumber(left) * toNumber(right);
            case "/" -> toNumber(left) / toNumber(right);
            case "%" -> toNumber(left) % toNumber(right);
            case "**" -> Math.pow(toNumber(left), toNumber(right));
            case "===" -> strictEquals(left, right);
            case "!==" -> !strictEquals(left, right);
            case "==" -> looseEquals(left, right);
            case "!=" -> !looseEquals(left, right);
            case "<" -> toNumber(left) < toNumber(right);
            case ">" -> toNumber(left) > toNumber(right);
            case "<=" -> toNumber(left) <= toNumber(right);
            case ">=" -> toNumber(left) >= toNumber(right);
            case "&" -> (double) ((long) toNumber(left) & (long) toNumber(right));
            case "|" -> (double) ((long) toNumber(left) | (long) toNumber(right));
            case "^" -> (double) ((long) toNumber(left) ^ (long) toNumber(right));
            case "instanceof" -> false; // simplified
            case "in" -> {
                if (right instanceof JSObject jo) yield jo.has(jsToString(left));
                yield false;
            }
            default -> UNDEFINED;
        };
    }

    // ─── Template literals ─────────────────────────────────────────────────────

    private Object evalTemplateLiteral(String raw, Environment env) {
        // raw contains \u0000 as separator, \u0001 as expr delimiters
        StringBuilder result = new StringBuilder();
        String[] parts = raw.split("\u0000", -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            // Each part may have embedded expressions delimited by \u0001
            String[] subparts = part.split("\u0001", -1);
            for (int j = 0; j < subparts.length; j++) {
                if (j % 2 == 0) {
                    result.append(subparts[j]);
                } else {
                    // This is an expression
                    try {
                        Lexer l = new Lexer(subparts[j]);
                        Parser p = new Parser(l.tokenize());
                        Node expr = p.parse().body.isEmpty() ? new Node.Literal("") : 
                            (p.parse().body.get(0) instanceof Node.ExprStmt es ? es.expr : new Node.Literal(""));
                        // Re-parse just the expression
                        Lexer l2 = new Lexer(subparts[j]);
                        Parser p2 = new Parser(l2.tokenize());
                        Node.Program prog = p2.parse();
                        if (!prog.body.isEmpty() && prog.body.get(0) instanceof Node.ExprStmt exprStmt) {
                            result.append(jsToString(eval(exprStmt.expr, env)));
                        }
                    } catch (Exception e) {
                        result.append(subparts[j]);
                    }
                }
            }
        }
        return result.toString();
    }

    // ─── Type coercion helpers ─────────────────────────────────────────────────

    public static double toNumber(Object val) {
        if (val == null) return 0;
        if (val == UNDEFINED) return Double.NaN;
        if (val instanceof Double d) return d;
        if (val instanceof Boolean b) return b ? 1 : 0;
        if (val instanceof String s) {
            s = s.trim();
            if (s.isEmpty()) return 0;
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { return Double.NaN; }
        }
        if (val instanceof JSArray ja) {
            if (ja.elements.size() == 0) return 0;
            if (ja.elements.size() == 1) return toNumber(ja.elements.get(0));
            return Double.NaN;
        }
        return Double.NaN;
    }

    public static boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val == UNDEFINED) return false;
        if (val instanceof Boolean b) return b;
        if (val instanceof Double d) return !d.isNaN() && d != 0;
        if (val instanceof String s) return !s.isEmpty();
        return true; // objects, arrays, functions are truthy
    }

    private static Object jsAdd(Object left, Object right) {
        if (left instanceof String || right instanceof String) {
            return jsToString(left) + jsToString(right);
        }
        if (left instanceof JSArray || right instanceof JSArray) {
            return jsToString(left) + jsToString(right);
        }
        return toNumber(left) + toNumber(right);
    }

    public static boolean strictEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a == UNDEFINED && b == UNDEFINED) return true;
        if (a == UNDEFINED || b == UNDEFINED) return false;
        if (a instanceof Double da && b instanceof Double db) {
            if (da.isNaN() || db.isNaN()) return false;
            return da.doubleValue() == db.doubleValue();
        }
        return a.equals(b);
    }

    private static boolean looseEquals(Object a, Object b) {
        if (strictEquals(a, b)) return true;
        if ((a == null || a == UNDEFINED) && (b == null || b == UNDEFINED)) return true;
        if (a instanceof Double && b instanceof String) return toNumber(a) == toNumber(b);
        if (a instanceof String && b instanceof Double) return toNumber(a) == toNumber(b);
        if (a instanceof Boolean) return looseEquals(toNumber(a), b);
        if (b instanceof Boolean) return looseEquals(a, toNumber(b));
        return false;
    }

    public static String jsToString(Object val) {
        if (val == null) return "null";
        if (val == UNDEFINED) return "undefined";
        if (val instanceof Boolean b) return b.toString();
        if (val instanceof Double d) return numberToString(d);
        if (val instanceof String s) return s;
        if (val instanceof JSArray ja) return ja.toString();
        if (val instanceof JSObject jo) return jo.toString();
        if (val instanceof JSFunction fn) return fn.toString();
        if (val instanceof NativeFunction) return "[native function]";
        return val.toString();
    }

    public static String numberToString(double d) {
        if (Double.isNaN(d)) return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
        if (d == Math.floor(d) && Math.abs(d) < 1e15) {
            long l = (long) d;
            return Long.toString(l);
        }
        // Use JavaScript-style number formatting
        String s = Double.toString(d);
        // Remove trailing zeros after decimal that Double.toString might add weirdly
        if (s.contains("E")) {
            // Scientific notation - format like JS
            // e.g., 1.5E10 -> 15000000000
            try {
                java.math.BigDecimal bd = new java.math.BigDecimal(d);
                String plain = bd.stripTrailingZeros().toPlainString();
                if (!plain.contains(".")) return plain;
                // strip trailing zeros
                plain = plain.replaceAll("0+$", "").replaceAll("\\.$", "");
                return plain;
            } catch (Exception e) { return s; }
        }
        return s;
    }

    private static String consoleFormat(Object val) {
        if (val instanceof Boolean || val instanceof Double || val instanceof String) return jsToString(val);
        return jsToString(val);
    }

    private static String jsTypeof(Object val) {
        if (val == UNDEFINED) return "undefined";
        if (val == null) return "object";
        if (val instanceof Boolean) return "boolean";
        if (val instanceof Double) return "number";
        if (val instanceof String) return "string";
        if (val instanceof JSFunction || val instanceof NativeFunction) return "function";
        if (val instanceof JSObject) return "object";
        return "undefined";
    }

    private static int normalizeIndex(int idx, int len) {
        return idx < 0 ? Math.max(0, len + idx) : idx;
    }

    private static List<Object> toIterable(Object val) {
        if (val instanceof JSArray ja) return new ArrayList<>(ja.elements);
        if (val instanceof String s) {
            List<Object> chars = new ArrayList<>();
            for (char c : s.toCharArray()) chars.add(String.valueOf(c));
            return chars;
        }
        if (val instanceof JSObject jo) return new ArrayList<>(jo.props.values());
        return List.of();
    }

    // ─── JSON ──────────────────────────────────────────────────────────────────

    private static String jsonStringify(Object val) {
        if (val == null) return "null";
        if (val == UNDEFINED) return null;
        if (val instanceof Boolean b) return b.toString();
        if (val instanceof Double d) return numberToString(d);
        if (val instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        if (val instanceof JSArray ja) {
            return "[" + ja.elements.stream().map(e -> {
                String r = jsonStringify(e);
                return r == null ? "null" : r;
            }).collect(Collectors.joining(",")) + "]";
        }
        if (val instanceof JSObject jo) {
            return "{" + jo.props.entrySet().stream()
                .filter(e -> {
                    String v = jsonStringify(e.getValue());
                    return v != null;
                })
                .map(e -> "\"" + e.getKey() + "\":" + jsonStringify(e.getValue()))
                .collect(Collectors.joining(",")) + "}";
        }
        return null;
    }

    private static Object jsonParse(String json) {
        json = json.trim();
        if (json.equals("null")) return null;
        if (json.equals("true")) return true;
        if (json.equals("false")) return false;
        if (json.startsWith("\"")) return json.substring(1, json.length() - 1).replace("\\\"", "\"");
        try { return Double.parseDouble(json); } catch (Exception ignored) {}
        // Simplified - just return a basic object for now
        return new JSObject();
    }
}
