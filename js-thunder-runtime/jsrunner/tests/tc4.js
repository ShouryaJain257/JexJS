//Reversing the string
function reverseString(str){
    let arr = str.split("");
    let n = arr.length;
    let start = 0;
    let end = n-1;

    while(start<end){
        let temp = arr[start];
        arr[start] = arr[end];
        arr[end] = temp;
        start++;
        end--;
    }

    return arr.join("");
}

let revStr = reverseString('Hello');
console.log(revStr);
