function isAlphaNumericChar(ch){
    let condition1 = (ch>='A' && ch<='Z');
    let condition2 = (ch>='a' && ch<='z');
    let condition3 = (ch>='0' && ch<='9');

    if(condition1 || condition2 || condition3)  return true;

    return false;
}
function isPalindromeString(str){
    let arr = str.split("");
    let n = arr.length;
    let i = 0;
    let j = n-1;

    while(i<j){
        let c1 = arr[i];
        let c2 = arr[j];

        if(!isAlphaNumericChar(c1)){
            i++;
            continue;
        }
        if(!isAlphaNumericChar(c2)){
            j--;
            continue;
        }
        if(c1.toLowerCase()!=c2.toLowerCase())  return false;

        i++;
        j--;
    }
    return true;
}

let str = "racecar";

if(isPalindromeString(str)){
    console.log(str,'is a palindrome')
}
else{
    console.log(str,'is not a palindrome')
}
