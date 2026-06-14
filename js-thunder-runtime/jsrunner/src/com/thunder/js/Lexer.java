package com.thunder.js;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private int pos;
    private int line;
    private final List<Token> tokens;

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("let", TokenType.LET),
        Map.entry("const", TokenType.CONST),
        Map.entry("var", TokenType.VAR),
        Map.entry("function", TokenType.FUNCTION),
        Map.entry("return", TokenType.RETURN),
        Map.entry("if", TokenType.IF),
        Map.entry("else", TokenType.ELSE),
        Map.entry("while", TokenType.WHILE),
        Map.entry("for", TokenType.FOR),
        Map.entry("do", TokenType.DO),
        Map.entry("switch", TokenType.SWITCH),
        Map.entry("case", TokenType.CASE),
        Map.entry("default", TokenType.DEFAULT),
        Map.entry("break", TokenType.BREAK),
        Map.entry("continue", TokenType.CONTINUE),
        Map.entry("new", TokenType.NEW),
        Map.entry("this", TokenType.THIS),
        Map.entry("typeof", TokenType.TYPEOF),
        Map.entry("instanceof", TokenType.INSTANCEOF),
        Map.entry("in", TokenType.IN),
        Map.entry("of", TokenType.OF),
        Map.entry("true", TokenType.TRUE),
        Map.entry("false", TokenType.FALSE),
        Map.entry("null", TokenType.NULL),
        Map.entry("undefined", TokenType.UNDEFINED)
    );

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.tokens = new ArrayList<>();
    }

    public List<Token> tokenize() {
        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;
            readToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '\n') { line++; pos++; }
            else if (Character.isWhitespace(c)) { pos++; }
            else if (pos + 1 < source.length() && c == '/' && source.charAt(pos + 1) == '/') {
                // Line comment
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            } else if (pos + 1 < source.length() && c == '/' && source.charAt(pos + 1) == '*') {
                // Block comment
                pos += 2;
                while (pos + 1 < source.length() && !(source.charAt(pos) == '*' && source.charAt(pos + 1) == '/')) {
                    if (source.charAt(pos) == '\n') line++;
                    pos++;
                }
                pos += 2;
            } else {
                break;
            }
        }
    }

    private void readToken() {
        char c = source.charAt(pos);

        // Numbers
        if (Character.isDigit(c) || (c == '.' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1)))) {
            readNumber();
            return;
        }

        // Strings
        if (c == '"' || c == '\'' || c == '`') {
            readString(c);
            return;
        }

        // Identifiers and keywords
        if (Character.isLetter(c) || c == '_' || c == '$') {
            readIdentifier();
            return;
        }

        // Spread / rest
        if (c == '.' && pos + 2 < source.length() && source.charAt(pos + 1) == '.' && source.charAt(pos + 2) == '.') {
            tokens.add(new Token(TokenType.SPREAD, "...", line));
            pos += 3;
            return;
        }

        // Operators and punctuation
        switch (c) {
            case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(", line)); pos++; }
            case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")", line)); pos++; }
            case '{' -> { tokens.add(new Token(TokenType.LBRACE, "{", line)); pos++; }
            case '}' -> { tokens.add(new Token(TokenType.RBRACE, "}", line)); pos++; }
            case '[' -> { tokens.add(new Token(TokenType.LBRACKET, "[", line)); pos++; }
            case ']' -> { tokens.add(new Token(TokenType.RBRACKET, "]", line)); pos++; }
            case ';' -> { tokens.add(new Token(TokenType.SEMICOLON, ";", line)); pos++; }
            case ',' -> { tokens.add(new Token(TokenType.COMMA, ",", line)); pos++; }
            case '.' -> { tokens.add(new Token(TokenType.DOT, ".", line)); pos++; }
            case ':' -> { tokens.add(new Token(TokenType.COLON, ":", line)); pos++; }
            case '?' -> {
                if (peek(1) == '?') { tokens.add(new Token(TokenType.NULLISH, "??", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.QUESTION, "?", line)); pos++; }
            }
            case '+' -> {
                if (peek(1) == '+') { tokens.add(new Token(TokenType.INCREMENT, "++", line)); pos += 2; }
                else if (peek(1) == '=') { tokens.add(new Token(TokenType.PLUS_ASSIGN, "+=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.PLUS, "+", line)); pos++; }
            }
            case '-' -> {
                if (peek(1) == '-') { tokens.add(new Token(TokenType.DECREMENT, "--", line)); pos += 2; }
                else if (peek(1) == '=') { tokens.add(new Token(TokenType.MINUS_ASSIGN, "-=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.MINUS, "-", line)); pos++; }
            }
            case '*' -> {
                if (peek(1) == '*') { tokens.add(new Token(TokenType.POWER, "**", line)); pos += 2; }
                else if (peek(1) == '=') { tokens.add(new Token(TokenType.STAR_ASSIGN, "*=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.STAR, "*", line)); pos++; }
            }
            case '/' -> {
                if (peek(1) == '=') { tokens.add(new Token(TokenType.SLASH_ASSIGN, "/=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.SLASH, "/", line)); pos++; }
            }
            case '%' -> {
                if (peek(1) == '=') { tokens.add(new Token(TokenType.PERCENT_ASSIGN, "%=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.PERCENT, "%", line)); pos++; }
            }
            case '=' -> {
                if (peek(1) == '=' && peek(2) == '=') { tokens.add(new Token(TokenType.STRICT_EQ, "===", line)); pos += 3; }
                else if (peek(1) == '=') { tokens.add(new Token(TokenType.EQ, "==", line)); pos += 2; }
                else if (peek(1) == '>') { tokens.add(new Token(TokenType.ARROW, "=>", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.ASSIGN, "=", line)); pos++; }
            }
            case '!' -> {
                if (peek(1) == '=' && peek(2) == '=') { tokens.add(new Token(TokenType.STRICT_NEQ, "!==", line)); pos += 3; }
                else if (peek(1) == '=') { tokens.add(new Token(TokenType.NEQ, "!=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.NOT, "!", line)); pos++; }
            }
            case '<' -> {
                if (peek(1) == '=') { tokens.add(new Token(TokenType.LTE, "<=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.LT, "<", line)); pos++; }
            }
            case '>' -> {
                if (peek(1) == '=') { tokens.add(new Token(TokenType.GTE, ">=", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.GT, ">", line)); pos++; }
            }
            case '&' -> {
                if (peek(1) == '&') { tokens.add(new Token(TokenType.AND, "&&", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.BITAND, "&", line)); pos++; }
            }
            case '|' -> {
                if (peek(1) == '|') { tokens.add(new Token(TokenType.OR, "||", line)); pos += 2; }
                else { tokens.add(new Token(TokenType.BITOR, "|", line)); pos++; }
            }
            case '^' -> { tokens.add(new Token(TokenType.BITXOR, "^", line)); pos++; }
            case '~' -> { tokens.add(new Token(TokenType.BITNOT, "~", line)); pos++; }
            default -> pos++; // skip unknown chars
        }
    }

    private char peek(int offset) {
        int p = pos + offset;
        return p < source.length() ? source.charAt(p) : '\0';
    }

    private void readNumber() {
        int start = pos;
        // Hex
        if (source.charAt(pos) == '0' && pos + 1 < source.length() && 
            (source.charAt(pos + 1) == 'x' || source.charAt(pos + 1) == 'X')) {
            pos += 2;
            while (pos < source.length() && isHexDigit(source.charAt(pos))) pos++;
        } else {
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
            if (pos < source.length() && source.charAt(pos) == '.') {
                pos++;
                while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
            }
            if (pos < source.length() && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
                pos++;
                if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) pos++;
                while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
            }
        }
        tokens.add(new Token(TokenType.NUMBER, source.substring(start, pos), line));
    }

    private boolean isHexDigit(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private void readString(char quote) {
        pos++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        boolean isTemplate = (quote == '`');
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == quote && !isTemplate) { pos++; break; }
            if (c == '`' && isTemplate) { pos++; break; }
            if (c == '\\') {
                pos++;
                if (pos >= source.length()) break;
                char esc = source.charAt(pos++);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '\\' -> sb.append('\\');
                    case '\'' -> sb.append('\'');
                    case '"' -> sb.append('"');
                    case '`' -> sb.append('`');
                    default -> { sb.append('\\'); sb.append(esc); }
                }
            } else if (isTemplate && c == '$' && pos + 1 < source.length() && source.charAt(pos + 1) == '{') {
                // Template literal interpolation - emit as PLUS concat pattern
                // We'll encode the template string in a special way
                sb.append('\u0000'); // mark end of string part
                pos += 2; // skip ${
                // collect expression until matching }
                int depth = 1;
                StringBuilder expr = new StringBuilder();
                while (pos < source.length() && depth > 0) {
                    char ec = source.charAt(pos);
                    if (ec == '{') depth++;
                    else if (ec == '}') { depth--; if (depth == 0) { pos++; break; } }
                    expr.append(ec);
                    pos++;
                }
                sb.append('\u0001').append(expr).append('\u0001');
            } else {
                if (c == '\n') line++;
                sb.append(c);
                pos++;
            }
        }
        tokens.add(new Token(isTemplate ? TokenType.STRING : TokenType.STRING, sb.toString(), line));
    }

    private void readIdentifier() {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_' || source.charAt(pos) == '$')) {
            pos++;
        }
        String word = source.substring(start, pos);
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
        // Normalize boolean/null keywords
        if (type == TokenType.TRUE) { tokens.add(new Token(TokenType.BOOLEAN, "true", line)); return; }
        if (type == TokenType.FALSE) { tokens.add(new Token(TokenType.BOOLEAN, "false", line)); return; }
        tokens.add(new Token(type, word, line));
    }
}
