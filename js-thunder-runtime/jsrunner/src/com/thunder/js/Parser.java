package com.thunder.js;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token current() { return tokens.get(pos); }
    private Token peek(int offset) {
        int p = pos + offset;
        return p < tokens.size() ? tokens.get(p) : tokens.get(tokens.size() - 1);
    }
    private Token consume() { return tokens.get(pos++); }
    private Token expect(TokenType type) {
        Token t = current();
        if (t.type != type) throw new RuntimeException("Expected " + type + " but got " + t.type + " ('" + t.value + "') at line " + t.line);
        return consume();
    }
    private boolean check(TokenType type) { return current().type == type; }
    private boolean match(TokenType... types) {
        for (TokenType t : types) { if (check(t)) { consume(); return true; } }
        return false;
    }
    private void skipSemicolon() {
        if (check(TokenType.SEMICOLON)) consume();
    }

    public Node.Program parse() {
        List<Node> body = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            body.add(parseStatement());
        }
        return new Node.Program(body);
    }

    private Node parseStatement() {
        Token t = current();
        return switch (t.type) {
            case LET, CONST, VAR -> parseVarDecl();
            case FUNCTION -> parseFuncDecl();
            case IF -> parseIf();
            case WHILE -> parseWhile();
            case DO -> parseDoWhile();
            case FOR -> parseFor();
            case SWITCH -> parseSwitch();
            case RETURN -> parseReturn();
            case BREAK -> { consume(); skipSemicolon(); yield new Node.BreakStmt(); }
            case CONTINUE -> { consume(); skipSemicolon(); yield new Node.ContinueStmt(); }
            case LBRACE -> parseBlock();
            case SEMICOLON -> { consume(); yield new Node.BlockStmt(List.of()); }
            case THROW -> parseThrow();
            default -> {
                // try/catch
                if (t.type == TokenType.IDENTIFIER && t.value.equals("try")) yield parseTryCatch();
                Node expr = parseExpression();
                skipSemicolon();
                yield new Node.ExprStmt(expr);
            }
        };
    }

    private Node parseVarDecl() {
        String kind = consume().value; // let/const/var
        
        // Destructuring: let { a, b } = obj
        if (check(TokenType.LBRACE)) {
            consume();
            List<String> names = new ArrayList<>();
            while (!check(TokenType.RBRACE)) {
                names.add(expect(TokenType.IDENTIFIER).value);
                if (!match(TokenType.COMMA)) break;
            }
            expect(TokenType.RBRACE);
            expect(TokenType.ASSIGN);
            Node init = parseExpression();
            skipSemicolon();
            return new Node.VarDestructureDecl(kind, names, init);
        }

        // Array destructuring: let [a, b] = arr
        if (check(TokenType.LBRACKET)) {
            consume();
            List<String> names = new ArrayList<>();
            while (!check(TokenType.RBRACKET)) {
                if (check(TokenType.COMMA)) { names.add(null); consume(); continue; }
                names.add(expect(TokenType.IDENTIFIER).value);
                if (!match(TokenType.COMMA)) break;
            }
            expect(TokenType.RBRACKET);
            expect(TokenType.ASSIGN);
            Node init = parseExpression();
            skipSemicolon();
            return new Node.ArrayDestructureDecl(kind, names, init);
        }

        String name = expect(TokenType.IDENTIFIER).value;
        Node init = null;
        if (match(TokenType.ASSIGN)) {
            init = parseExpression();
        }
        skipSemicolon();
        return new Node.VarDecl(kind, name, init);
    }

    private Node parseFuncDecl() {
        consume(); // function
        String name = null;
        if (check(TokenType.IDENTIFIER)) name = consume().value;
        expect(TokenType.LPAREN);
        List<String> params = parseParams();
        expect(TokenType.RPAREN);
        Node body = parseBlock();
        return new Node.FuncDecl(name, params, body, false);
    }

    private List<String> parseParams() {
        List<String> params = new ArrayList<>();
        while (!check(TokenType.RPAREN)) {
            boolean isRest = false;
            if (check(TokenType.SPREAD)) { consume(); isRest = true; }
            if (check(TokenType.IDENTIFIER)) {
                String p = consume().value;
                params.add(isRest ? "..." + p : p);
            }
            // default param
            if (check(TokenType.ASSIGN)) {
                consume(); parseExpression(); // skip default for now
            }
            if (!match(TokenType.COMMA)) break;
        }
        return params;
    }

    private Node parseBlock() {
        expect(TokenType.LBRACE);
        List<Node> body = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            body.add(parseStatement());
        }
        expect(TokenType.RBRACE);
        return new Node.BlockStmt(body);
    }

    private Node parseIf() {
        consume(); // if
        expect(TokenType.LPAREN);
        Node condition = parseExpression();
        expect(TokenType.RPAREN);
        Node consequent = parseStatement();
        Node alternate = null;
        if (check(TokenType.ELSE)) {
            consume();
            alternate = parseStatement();
        }
        return new Node.IfStmt(condition, consequent, alternate);
    }

    private Node parseWhile() {
        consume(); // while
        expect(TokenType.LPAREN);
        Node condition = parseExpression();
        expect(TokenType.RPAREN);
        Node body = parseStatement();
        return new Node.WhileStmt(condition, body);
    }

    private Node parseDoWhile() {
        consume(); // do
        Node body = parseStatement();
        expect(TokenType.WHILE);
        expect(TokenType.LPAREN);
        Node condition = parseExpression();
        expect(TokenType.RPAREN);
        skipSemicolon();
        return new Node.DoWhileStmt(body, condition);
    }

    private Node parseFor() {
        consume(); // for
        expect(TokenType.LPAREN);

        // Check for for..of or for..in
        // Look ahead: for (let x of ...) or for (let x in ...)
        // peek(2) must be OF or IN, not = or ; (which means regular for loop)
        if ((current().type == TokenType.LET || current().type == TokenType.CONST || current().type == TokenType.VAR)
            && peek(1).type == TokenType.IDENTIFIER
            && (peek(2).type == TokenType.OF || peek(2).type == TokenType.IN)) {
            String kind = consume().value;
            String varName = consume().value;
            if (check(TokenType.OF)) {
                consume();
                Node iterable = parseExpression();
                expect(TokenType.RPAREN);
                Node body = parseStatement();
                return new Node.ForOfStmt(kind, varName, iterable, body);
            } else if (check(TokenType.IN)) {
                consume();
                Node obj = parseExpression();
                expect(TokenType.RPAREN);
                Node body = parseStatement();
                return new Node.ForInStmt(kind, varName, obj, body);
            } else {
                // put back - it's a regular for with let decl
                // We need to reconstruct - handle as var decl
                Node init = null;
                Node varInit = null;
                if (match(TokenType.ASSIGN)) varInit = parseExpression();
                init = new Node.VarDecl(kind, varName, varInit);
                expect(TokenType.SEMICOLON);
                Node test = check(TokenType.SEMICOLON) ? null : parseExpression();
                expect(TokenType.SEMICOLON);
                Node update = check(TokenType.RPAREN) ? null : parseExpression();
                expect(TokenType.RPAREN);
                Node body = parseStatement();
                return new Node.ForStmt(init, test, update, body);
            }
        }

        Node init = null;
        if (!check(TokenType.SEMICOLON)) {
            if (current().type == TokenType.LET || current().type == TokenType.CONST || current().type == TokenType.VAR) {
                init = parseVarDeclNoSemi();
            } else {
                init = new Node.ExprStmt(parseExpression());
            }
        }
        expect(TokenType.SEMICOLON);
        Node test = check(TokenType.SEMICOLON) ? null : parseExpression();
        expect(TokenType.SEMICOLON);
        Node update = check(TokenType.RPAREN) ? null : parseExpression();
        expect(TokenType.RPAREN);
        Node body = parseStatement();
        return new Node.ForStmt(init, test, update, body);
    }

    private Node parseVarDeclNoSemi() {
        String kind = consume().value;
        String name = expect(TokenType.IDENTIFIER).value;
        Node init = null;
        if (match(TokenType.ASSIGN)) init = parseExpression();
        return new Node.VarDecl(kind, name, init);
    }

    private Node parseSwitch() {
        consume(); // switch
        expect(TokenType.LPAREN);
        Node discriminant = parseExpression();
        expect(TokenType.RPAREN);
        expect(TokenType.LBRACE);
        List<Node.SwitchCase> cases = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            Node test = null;
            if (check(TokenType.CASE)) {
                consume();
                test = parseExpression();
            } else {
                expect(TokenType.DEFAULT);
            }
            expect(TokenType.COLON);
            List<Node> body = new ArrayList<>();
            while (!check(TokenType.CASE) && !check(TokenType.DEFAULT) && !check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                body.add(parseStatement());
            }
            cases.add(new Node.SwitchCase(test, body));
        }
        expect(TokenType.RBRACE);
        return new Node.SwitchStmt(discriminant, cases);
    }

    private Node parseReturn() {
        consume(); // return
        Node value = null;
        if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            value = parseExpression();
        }
        skipSemicolon();
        return new Node.ReturnStmt(value);
    }

    private Node parseThrow() {
        consume(); // throw
        Node value = parseExpression();
        skipSemicolon();
        return new Node.ThrowStmt(value);
    }

    private Node parseTryCatch() {
        consume(); // try
        Node tryBlock = parseBlock();
        String catchParam = null;
        Node catchBlock = null;
        Node finallyBlock = null;
        if (check(TokenType.IDENTIFIER) && current().value.equals("catch")) {
            consume();
            if (check(TokenType.LPAREN)) {
                consume();
                catchParam = expect(TokenType.IDENTIFIER).value;
                expect(TokenType.RPAREN);
            }
            catchBlock = parseBlock();
        }
        if (check(TokenType.IDENTIFIER) && current().value.equals("finally")) {
            consume();
            finallyBlock = parseBlock();
        }
        return new Node.TryCatchStmt(tryBlock, catchParam, catchBlock, finallyBlock);
    }

    // ─── Expression Parsing (Pratt / precedence climbing) ──────────────────────

    private Node parseExpression() {
        return parseAssignment();
    }

    private Node parseAssignment() {
        Node left = parseTernary();
        Token t = current();
        if (t.type == TokenType.ASSIGN) {
            consume();
            Node right = parseAssignment();
            return new Node.AssignExpr("=", left, right);
        }
        if (t.type == TokenType.PLUS_ASSIGN) { consume(); return new Node.AssignExpr("+=", left, parseAssignment()); }
        if (t.type == TokenType.MINUS_ASSIGN) { consume(); return new Node.AssignExpr("-=", left, parseAssignment()); }
        if (t.type == TokenType.STAR_ASSIGN) { consume(); return new Node.AssignExpr("*=", left, parseAssignment()); }
        if (t.type == TokenType.SLASH_ASSIGN) { consume(); return new Node.AssignExpr("/=", left, parseAssignment()); }
        if (t.type == TokenType.PERCENT_ASSIGN) { consume(); return new Node.AssignExpr("%=", left, parseAssignment()); }
        return left;
    }

    private Node parseTernary() {
        Node condition = parseNullish();
        if (check(TokenType.QUESTION)) {
            consume();
            Node consequent = parseExpression();
            expect(TokenType.COLON);
            Node alternate = parseExpression();
            return new Node.TernaryExpr(condition, consequent, alternate);
        }
        return condition;
    }

    private Node parseNullish() {
        Node left = parseOr();
        while (check(TokenType.NULLISH)) {
            String op = consume().value;
            Node right = parseOr();
            left = new Node.LogicalExpr(op, left, right);
        }
        return left;
    }

    private Node parseOr() {
        Node left = parseAnd();
        while (check(TokenType.OR)) {
            consume();
            Node right = parseAnd();
            left = new Node.LogicalExpr("||", left, right);
        }
        return left;
    }

    private Node parseAnd() {
        Node left = parseEquality();
        while (check(TokenType.AND)) {
            consume();
            Node right = parseEquality();
            left = new Node.LogicalExpr("&&", left, right);
        }
        return left;
    }

    private Node parseEquality() {
        Node left = parseComparison();
        while (true) {
            Token t = current();
            if (t.type == TokenType.STRICT_EQ || t.type == TokenType.STRICT_NEQ
                || t.type == TokenType.EQ || t.type == TokenType.NEQ) {
                String op = consume().value;
                left = new Node.BinaryExpr(op, left, parseComparison());
            } else break;
        }
        return left;
    }

    private Node parseComparison() {
        Node left = parseAdditive();
        while (true) {
            Token t = current();
            if (t.type == TokenType.LT || t.type == TokenType.GT
                || t.type == TokenType.LTE || t.type == TokenType.GTE
                || t.type == TokenType.INSTANCEOF || t.type == TokenType.IN) {
                String op = consume().value;
                left = new Node.BinaryExpr(op, left, parseAdditive());
            } else break;
        }
        return left;
    }

    private Node parseAdditive() {
        Node left = parseMultiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            String op = consume().value;
            left = new Node.BinaryExpr(op, left, parseMultiplicative());
        }
        return left;
    }

    private Node parseMultiplicative() {
        Node left = parsePower();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            String op = consume().value;
            left = new Node.BinaryExpr(op, left, parsePower());
        }
        return left;
    }

    private Node parsePower() {
        Node left = parseUnary();
        if (check(TokenType.POWER)) {
            consume();
            return new Node.BinaryExpr("**", left, parsePower());
        }
        return left;
    }

    private Node parseUnary() {
        if (check(TokenType.NOT)) { consume(); return new Node.UnaryExpr("!", parseUnary(), true); }
        if (check(TokenType.MINUS)) { consume(); return new Node.UnaryExpr("-", parseUnary(), true); }
        if (check(TokenType.PLUS)) { consume(); return new Node.UnaryExpr("+", parseUnary(), true); }
        if (check(TokenType.BITNOT)) { consume(); return new Node.UnaryExpr("~", parseUnary(), true); }
        if (check(TokenType.TYPEOF)) { consume(); return new Node.TypeofExpr(parseUnary()); }
        if (check(TokenType.INCREMENT)) { consume(); return new Node.UnaryExpr("++", parsePostfix(), true); }
        if (check(TokenType.DECREMENT)) { consume(); return new Node.UnaryExpr("--", parsePostfix(), true); }
        if (check(TokenType.NEW)) {
            consume();
            Node callee = parseMember(parsePrimary());
            List<Node> args = new ArrayList<>();
            if (check(TokenType.LPAREN)) {
                consume();
                args = parseArgs();
                expect(TokenType.RPAREN);
            }
            return new Node.NewExpr(callee, args);
        }
        return parsePostfix();
    }

    private Node parsePostfix() {
        Node node = parseCall();
        if (check(TokenType.INCREMENT)) { consume(); return new Node.UnaryExpr("++", node, false); }
        if (check(TokenType.DECREMENT)) { consume(); return new Node.UnaryExpr("--", node, false); }
        return node;
    }

    private Node parseCall() {
        Node node = parseMember(parsePrimary());
        while (true) {
            if (check(TokenType.LPAREN)) {
                consume();
                List<Node> args = parseArgs();
                expect(TokenType.RPAREN);
                node = new Node.CallExpr(node, args);
                // Continue to handle chained calls and member access
                node = parseMember(node);
            } else {
                break;
            }
        }
        return node;
    }

    private Node parseMember(Node obj) {
        while (true) {
            if (check(TokenType.DOT)) {
                consume();
                String prop = current().value; // could be keyword used as property
                consume();
                obj = new Node.MemberExpr(obj, new Node.Literal(prop), false);
            } else if (check(TokenType.LBRACKET)) {
                consume();
                Node prop = parseExpression();
                expect(TokenType.RBRACKET);
                obj = new Node.MemberExpr(obj, prop, true);
            } else if (check(TokenType.LPAREN)) {
                // chained call
                consume();
                List<Node> args = parseArgs();
                expect(TokenType.RPAREN);
                obj = new Node.CallExpr(obj, args);
            } else {
                break;
            }
        }
        return obj;
    }

    private List<Node> parseArgs() {
        List<Node> args = new ArrayList<>();
        while (!check(TokenType.RPAREN) && !check(TokenType.EOF)) {
            if (check(TokenType.SPREAD)) {
                consume();
                args.add(new Node.SpreadExpr(parseExpression()));
            } else {
                args.add(parseExpression());
            }
            if (!match(TokenType.COMMA)) break;
        }
        return args;
    }

    private Node parsePrimary() {
        Token t = current();

        // Template literal
        if (t.type == TokenType.STRING && t.value.contains("\u0000")) {
            consume();
            return new Node.TemplateLiteral(t.value);
        }

        return switch (t.type) {
            case NUMBER -> {
                consume();
                String v = t.value;
                if (v.startsWith("0x") || v.startsWith("0X")) yield new Node.Literal((double) Long.parseLong(v.substring(2), 16));
                yield new Node.Literal(Double.parseDouble(v));
            }
            case STRING -> { consume(); yield new Node.Literal(t.value); }
            case BOOLEAN -> { consume(); yield new Node.Literal(t.value.equals("true")); }
            case NULL -> { consume(); yield new Node.Literal(null); }
            case UNDEFINED -> { consume(); yield new Node.Identifier("undefined"); }
            case THIS -> { consume(); yield new Node.Identifier("this"); }
            case IDENTIFIER -> {
                consume();
                // Arrow function: x => expr or (x) => expr
                if (check(TokenType.ARROW)) {
                    consume();
                    Node body = check(TokenType.LBRACE) ? parseBlock() : new Node.ReturnStmt(parseExpression());
                    yield new Node.FuncDecl(null, List.of(t.value), body, true);
                }
                yield new Node.Identifier(t.value);
            }
            case FUNCTION -> {
                consume();
                String name = null;
                if (check(TokenType.IDENTIFIER)) name = consume().value;
                expect(TokenType.LPAREN);
                List<String> params = parseParams();
                expect(TokenType.RPAREN);
                Node body = parseBlock();
                yield new Node.FuncDecl(name, params, body, false);
            }
            case LPAREN -> {
                consume();
                // Could be arrow function with multiple params: (a, b) => ...
                // Or grouped expression
                if (check(TokenType.RPAREN)) {
                    consume();
                    expect(TokenType.ARROW);
                    Node body = check(TokenType.LBRACE) ? parseBlock() : new Node.ReturnStmt(parseExpression());
                    yield new Node.FuncDecl(null, List.of(), body, true);
                }
                // Save position, try to parse as arrow function params
                int savedPos = pos;
                try {
                    List<String> params = parseParams();
                    if (check(TokenType.RPAREN) && peek(1).type == TokenType.ARROW) {
                        consume(); // )
                        consume(); // =>
                        Node body = check(TokenType.LBRACE) ? parseBlock() : new Node.ReturnStmt(parseExpression());
                        yield new Node.FuncDecl(null, params, body, true);
                    }
                    pos = savedPos;
                } catch (Exception e) {
                    pos = savedPos;
                }
                Node expr = parseExpression();
                expect(TokenType.RPAREN);
                yield expr;
            }
            case LBRACKET -> {
                consume();
                List<Node> elements = new ArrayList<>();
                while (!check(TokenType.RBRACKET) && !check(TokenType.EOF)) {
                    if (check(TokenType.COMMA)) { elements.add(new Node.Literal(null)); consume(); continue; }
                    if (check(TokenType.SPREAD)) { consume(); elements.add(new Node.SpreadExpr(parseExpression())); }
                    else elements.add(parseExpression());
                    if (!match(TokenType.COMMA)) break;
                }
                expect(TokenType.RBRACKET);
                yield new Node.ArrayExpr(elements);
            }
            case LBRACE -> {
                consume();
                List<Map.Entry<Node, Node>> props = new ArrayList<>();
                while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
                    Node key;
                    if (check(TokenType.LBRACKET)) {
                        consume();
                        key = parseExpression();
                        expect(TokenType.RBRACKET);
                    } else if (check(TokenType.STRING)) {
                        key = new Node.Literal(consume().value);
                    } else if (check(TokenType.NUMBER)) {
                        key = new Node.Literal(Double.parseDouble(consume().value));
                    } else {
                        // identifier or keyword as key
                        String k = consume().value;
                        // Shorthand { x } or method { fn() {} }
                        if (check(TokenType.LPAREN)) {
                            // Method
                            consume();
                            List<String> params = parseParams();
                            expect(TokenType.RPAREN);
                            Node body = parseBlock();
                            props.add(Map.entry(new Node.Literal(k), new Node.FuncDecl(k, params, body, false)));
                            if (!match(TokenType.COMMA)) break;
                            continue;
                        }
                        if (!check(TokenType.COLON)) {
                            // Shorthand
                            props.add(Map.entry(new Node.Literal(k), new Node.Identifier(k)));
                            if (!match(TokenType.COMMA)) break;
                            continue;
                        }
                        key = new Node.Literal(k);
                    }
                    expect(TokenType.COLON);
                    Node value = parseExpression();
                    props.add(Map.entry(key, value));
                    if (!match(TokenType.COMMA)) break;
                }
                expect(TokenType.RBRACE);
                yield new Node.ObjectExpr(props);
            }
            default -> throw new RuntimeException("Unexpected token: " + t.type + " ('" + t.value + "') at line " + t.line);
        };
    }
}
