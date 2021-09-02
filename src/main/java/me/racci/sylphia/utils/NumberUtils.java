package me.racci.sylphia.utils;

import java.util.TreeMap;

public class NumberUtils {

	private NumberUtils() { }

	private static final TreeMap<Integer, String> map = new TreeMap<>();

	static {
		map.put(1000000, "M");
		map.put(900000, "CM");
		map.put(500000, "D");
		map.put(100000, "C");
		map.put(90000, "XC");
		map.put(50000, "L");
		map.put(10000, "X");
		map.put(9000, "MX");
		map.put(5000, "v");
		map.put(1000, "M");
		map.put(900, "CM");
		map.put(500, "D");
		map.put(400, "CD");
		map.put(100, "C");
		map.put(90, "XC");
		map.put(50, "L");
		map.put(40, "XL");
		map.put(10, "X");
		map.put(9, "IX");
		map.put(5, "V");
		map.put(4, "IV");
		map.put(1, "I");
	}

	public static String toRoman(int number) {
		if (number > 0) {
			int l =  map.floorKey(number);
			if ( number == l ) {
				return map.get(number);
			}
			return map.get(l) + toRoman(number-l);
		}
		else {
			return String.valueOf(number);
		}
	}

}
