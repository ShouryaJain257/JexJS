#!/bin/bash
# run_tests.sh - Run all 5 test cases and verify expected output

cd "$(dirname "$0")/.."

if [ ! -d "out" ]; then
    bash build.sh
fi

PASS=0
FAIL=0

run_test() {
    local num=$1
    local file=$2
    local expected=$3
    local actual
    actual=$(java -cp out com.thunder.js.Main "$file" 2>/dev/null)
    if [ "$actual" = "$expected" ]; then
        echo "✅ TC$num PASS"
        PASS=$((PASS+1))
    else
        echo "❌ TC$num FAIL"
        echo "   Expected: $(echo "$expected" | head -3)"
        echo "   Got:      $(echo "$actual" | head -3)"
        FAIL=$((FAIL+1))
    fi
}

run_test 1 tests/tc1.js "7 is Odd"
run_test 2 tests/tc2.js "*
**
***
****
*****"
run_test 3 tests/tc3.js "true
false"
run_test 4 tests/tc4.js "Original: 1, 2, 3, 4, 5
Reversed: 5, 4, 3, 2, 1"
run_test 5 tests/tc5.js "racecar is a Palindrome"

echo ""
echo "Results: $PASS/5 passed"
[ $FAIL -eq 0 ] && echo "🏆 All tests passed!" || echo "⚠️  $FAIL test(s) failed"
