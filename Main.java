package com.company;

public class Main {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        int[][] k1 = new int[100][100];
        int t = 1;
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                k1[i][j]=t++;
            }
        }
        int[][] k2 = new int[100][100];
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                for (int k = 0; k < 100; k++) {
                    k2[i][j] += k1[i][k] * k1[k][j];
                }
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("程序运行时间：" + (end - start) + "ms");
    }
}
