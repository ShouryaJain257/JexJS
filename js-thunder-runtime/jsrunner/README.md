# ⚡ JS Thunder Runtime

> A JavaScript interpreter written from scratch in pure Java — no external libraries, no Node.js, no JS engine. Just Java.

Built for **Thunder Hackathon 2.0: Build Your Own JavaScript**.

---

## 🏗️ Architecture

```
JavaScript Source Code
        │
        ▼
  ┌─────────────┐
  │    Lexer    │  Tokenizes source into a stream of Tokens
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │   Parser    │  Builds an Abstract Syntax Tree (AST)
  └─────────────┘
        │
        ▼
  ┌─────────────┐
  │ Interpreter │  Tree-walk evaluator with Environment-based scoping
  └─────────────┘
        │
        ▼
     stdout
```

### Components

| File | Role |
|------|------|
| `Lexer.java` | Tokenizer — handles all JS syntax including template literals, operators, comments |
| `TokenType.java` + `Token.java` | Token definitions |
| `Parser.java` | Recursive-descent parser producing an AST |
| `Node.java` | All 30+ AST node types |
| `Interpreter.java` | Tree-walk evaluator with all built-ins |
| `Environment.java` | Lexical scope chain |
| `JSFunction.java` | First-class function objects with closure |
| `JSObject.java` | JavaScript object (key-value store with prototype) |
| `JSArray.java` | JavaScript array with numeric indexing + all array methods |
| `NativeFunction.java` | Functional interface for built-in Java-implemented functions |
| `Main.java` | Entry point — reads file or stdin, runs the runtime |

---

## 🚀 Requirements

- **Java 17+** (uses records, switch expressions, text blocks)
- No external libraries required

---

## 🔨 Build

```bash
# Compile all sources
bash build.sh
```

Or manually:
```bash
mkdir -p out
find src -name "*.java" | xargs javac -d out
```

---

## ▶️ Run

```bash
# Run a JavaScript file
java -cp out com.thunder.js.Main myfile.js

# Pipe JS code via stdin
echo 'console.log("Hello, Thunder!")' | java -cp out com.thunder.js.Main

# Use the wrapper script (auto-builds if needed)
bash run.sh myfile.js
```

---

## ✅ Test Cases

Run all 5 official test cases:
```bash
bash tests/run_tests.sh
```

Expected output:
```
✅ TC1 PASS  →  7 is Odd
✅ TC2 PASS  →  * / ** / *** / **** / *****
✅ TC3 PASS  →  true / false
✅ TC4 PASS  →  Original: 1, 2, 3, 4, 5 / Reversed: 5, 4, 3, 2, 1
✅ TC5 PASS  →  racecar is a Palindrome
🏆 All tests passed!
```

---

## 🧠 Supported JavaScript Features

### Variables & Types
- `let`, `const`, `var` declarations
- Destructuring: `let { a, b } = obj` and `let [x, y] = arr`
- Primitives: `number`, `string`, `boolean`, `null`, `undefined`
- Reference types: `object`, `array`, `function`

### Operators
- Arithmetic: `+`, `-`, `*`, `/`, `%`, `**` (power)
- Comparison: `===`, `!==`, `==`, `!=`, `<`, `>`, `<=`, `>=`
- Logical: `&&`, `||`, `!`, `??` (nullish coalescing)
- Assignment: `=`, `+=`, `-=`, `*=`, `/=`, `%=`
- Increment/Decrement: `++`, `--` (prefix and postfix)
- Bitwise: `&`, `|`, `^`, `~`
- Spread: `...`
- Ternary: `condition ? a : b`

### Control Flow
- `if` / `else if` / `else`
- `while`, `do...while`
- `for` (classic), `for...of`, `for...in`
- `switch` / `case` / `default` / `break`
- `break`, `continue`
- `try` / `catch` / `finally`, `throw`

### Functions
- Function declarations, function expressions
- Arrow functions: `x => x * 2`, `(a, b) => a + b`
- Closures and lexical scoping
- Rest parameters: `...args`
- Callback functions
- First-class functions (pass as arguments, return from functions)

### Arrays
Full support for: `push`, `pop`, `shift`, `unshift`, `slice`, `splice`,
`concat`, `includes`, `indexOf`, `lastIndexOf`, `sort`, `reverse`,
`map`, `filter`, `reduce`, `reduceRight`, `forEach`, `find`, `findIndex`,
`some`, `every`, `flat`, `flatMap`, `fill`, `join`, `keys`, `values`, `at`

### Strings
Full support for: `length`, `charAt`, `charCodeAt`, `indexOf`, `lastIndexOf`,
`includes`, `startsWith`, `endsWith`, `slice`, `substring`, `split`,
`replace`, `replaceAll`, `toUpperCase`, `toLowerCase`, `trim`, `trimStart`,
`trimEnd`, `repeat`, `padStart`, `padEnd`, `concat`, `at`

### Objects
- Object literals with shorthand, computed keys, method shorthand
- `Object.keys()`, `Object.values()`, `Object.entries()`, `Object.assign()`
- Property access (dot and bracket notation)

### Built-ins
- `console.log`, `console.error`, `console.warn`
- `Math` — `floor`, `ceil`, `round`, `abs`, `sqrt`, `pow`, `max`, `min`, `random`, `log`, `sin`, `cos`, `tan`, `trunc`, `sign`, `PI`, `E`
- `JSON.stringify`, `JSON.parse`
- `parseInt`, `parseFloat`, `isNaN`, `isFinite`
- `Number`, `String`, `Boolean`, `Array`, `Object` constructors
- `Date` (basic)
- Template literals `` `Hello ${name}` ``
- `typeof`, `instanceof`
- `new` keyword for object construction

---

## 💡 Design Decisions

**Why a pure Java interpreter?**  
Writing our own lexer + parser + interpreter from scratch (rather than shelling out to an existing engine) showcases the deepest understanding of how JavaScript actually works — from character streams all the way to closures and scope chains.

**Recursive-descent parser**  
The parser uses a hand-written recursive descent with proper operator precedence (Pratt-style precedence climbing for expressions).

**Environment chains for lexical scope**  
Each block/function creates a new `Environment` that chains to its parent, correctly implementing JS closures and block scoping.

**Visitor-style tree-walk interpreter**  
The AST evaluator walks the tree recursively, matching node types using Java 21 pattern matching (`instanceof`) for clarity and performance.

---

## 📁 Project Structure

```
jsrunner/
├── src/
│   └── com/thunder/js/
│       ├── Main.java          # Entry point
│       ├── Lexer.java         # Tokenizer
│       ├── Token.java         # Token data class
│       ├── TokenType.java     # Token type enum
│       ├── Parser.java        # AST builder
│       ├── Node.java          # AST node types
│       ├── Interpreter.java   # Tree-walk evaluator
│       ├── Environment.java   # Scope chain
│       ├── JSFunction.java    # Function objects
│       ├── JSObject.java      # Object type
│       ├── JSArray.java       # Array type
│       └── NativeFunction.java
├── tests/
│   ├── tc1.js → tc5.js       # Official test cases
│   └── run_tests.sh          # Test runner
├── build.sh                  # Compile script
├── run.sh                    # Run wrapper
└── README.md
```

---

*Thunder Hackathon 2.0 — Built with ⚡ by [Your Name]*
