package com.minecraftloveyou.core;

import java.util.ArrayList;
import java.util.List;

/** 罗马数字工具（附魔等级显示用）。 */
public final class RomanNumeral {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumeral() {
    }

    public static String of(int n) {
        if (n <= 0) {
            return String.valueOf(n);
        }
        if (n >= 4000) {
            return String.valueOf(n); // 超大等级直接数字
        }
        StringBuilder sb = new StringBuilder();
        int v = n;
        for (int i = 0; i < VALUES.length; i++) {
            while (v >= VALUES[i]) {
                sb.append(SYMBOLS[i]);
                v -= VALUES[i];
            }
        }
        return sb.toString();
    }

    public static List<String> dummyForCompile() {
        return new ArrayList<>();
    }
}