//Reversing the Array

function reverseArray(arr){
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

    return arr;
}

let arr = [1,2,3,4,5];
let revArr = reverseArray(arr);
console.log(revArr);
