/*
    Armstrong no. -> equals to sum of its digits raised to the power of number of digits 
*/
function isArmstrong(num) {
    let temp = Math.abs(num);
    let absNum = Math.abs(num);// reference
    let sum = 0;

    const digit_count = function(n){
        let count = 0;
        while(n!=0){
            count += 1;
            n = Math.floor(n/10);
        }
        return count;
    }

    let digits = digit_count(temp);
    temp = absNum;
    
    while (temp > 0) {
        let digit = temp % 10;
        sum += digit ** digits;
        temp = Math.floor(temp / 10);
    }
    return sum === absNum;
}
console.log(isArmstrong(153));
console.log(isArmstrong(-371));
