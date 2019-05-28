package FreeTry;

import java.util.Scanner;

public class FakeMain {
    public static void main(String[] args) {
    // TODO 自动生成的方法存根
    //二进制转换十进制
    Scanner input1 = new Scanner( System.in );
    System.out.print("Enter a binary number: ");
    String binaryString =input1.nextLine();
    System.out.println("Output toDecimal: "+Integer.parseInt(binaryString,2));
    //十进制转二进制
    Scanner input2 = new Scanner( System.in );
    String temp = Integer.toBinaryString(input2.nextInt());
    //int decimalInt = input2.nextInt();
    System.out.println("Output toBinary: "+/*Integer.toBinaryString(  decimalInt)*/temp);
    }






}
