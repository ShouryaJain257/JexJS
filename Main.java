package com.thunder.js;

import java.io.*;
import java.nio.file.*;
import java.util.List;

/**
 * JS Thunder Runtime — A JavaScript interpreter written in Java.
 * 
 * Thunder Hackathon 2.0 submission.
 * 
 * Usage:
 *   java -cp . com.thunder.js.Main <file.js>
 *   java -cp . com.thunder.js.Main              (reads from stdin)
 *   echo "console.log(42)" | java -cp . com.thunder.js.Main
 */
public class Main {

    public static void main(String[] args) {
        String source;
        try {
            if (args.length > 0) {
                // Read from file
                source = Files.readString(Path.of(args[0]));
            } else {
                // Read from stdin
                source = new String(System.in.readAllBytes());
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
            System.exit(1);
            return;
        }

        run(source);
    }

    public static void run(String source) {
        try {
            // 1. Lex
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            // 2. Parse
            Parser parser = new Parser(tokens);
            Node.Program ast = parser.parse();

            // 3. Interpret
            Interpreter interpreter = new Interpreter();
            interpreter.execute(ast);

            // 4. Output
            String output = interpreter.getOutput();
            if (!output.isEmpty()) {
                // Print without trailing newline (our output already has \n per line)
                System.out.print(output);
            }

        } catch (Exception e) {
            System.err.println("Runtime Error: " + e.getMessage());
            if (System.getenv("JS_DEBUG") != null) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
