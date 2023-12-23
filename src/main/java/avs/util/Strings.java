/*
 * This file is part of Simple Blacklist. The plugin securing your server against unwanted nicknames.
 *
 * MIT License
 *
 * Copyright (c) 2023 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package avs.util;
import arc.struct.Seq;


public class Strings extends arc.util.Strings {
  public static String rJust(String str, int newLenght) { return rJust(str, newLenght, " "); }
  public static String rJust(String str, int newLenght, String filler) {
    if (filler.length() == 0) return str; // Cannot fill, so return initial string
    return repeat(filler, (newLenght-str.length())/filler.length())+filler.substring(0, (newLenght-str.length())%filler.length())+str;
  }
  public static Seq<String> rJust(Seq<String> list, int newLenght) { return rJust(list, newLenght, " "); }
  public static Seq<String> rJust(Seq<String> list, int newLenght, String filler) {
    return list.map(str -> rJust(str, newLenght, filler));
  }

  public static String lJust(String str, int newLenght) { return lJust(str, newLenght, " "); }
  public static String lJust(String str, int newLenght, String filler) {
    if (filler.length() == 0) return str; // Cannot fill, so return initial string
    return str+repeat(filler, (newLenght-str.length())/filler.length())+filler.substring(0, (newLenght-str.length())%filler.length());
  }
  public static Seq<String> lJust(Seq<String> list, int newLenght) { return lJust(list, newLenght, " "); }
  public static Seq<String> lJust(Seq<String> list, int newLenght, String filler) {
    return list.map(str -> lJust(str, newLenght, filler));
  }

  public static String mJust(String left, String right, int newLenght) { return mJust(left, right, newLenght, " "); }
  public static String mJust(String left, String right, int newLenght, String filler) {
    if (filler.length() == 0) return left+right; // Cannot fill, so return concatened sides
    int s = newLenght-left.length()-right.length();
    return left+repeat(filler, s/filler.length())+filler.substring(0, s%filler.length())+right;
  }
  public static Seq<String> mJust(Seq<String> left, Seq<String> right, int newLenght) { return mJust(left, right, newLenght, " "); }
  public static Seq<String> mJust(Seq<String> left, Seq<String> right, int newLenght, String filler) {
    Seq<String> arr = new Seq<>(Integer.max(left.size, right.size));
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++) arr.add(mJust(left.get(i), right.get(i), newLenght, filler));
    // Fill the rest
    for (; i<left.size; i++) arr.add(lJust(left.get(i), newLenght, filler));
    for (; i<right.size; i++) arr.add(rJust(right.get(i), newLenght, filler));
    
    return arr;
  }

  public static String repeat(String str, int count) {
    String result = "";
    while (count-- > 0) result+=str;
    return result;
  }

  public static int bestLength(Iterable<? extends String> list) {
    int best = 0;
  
    for (String i : list) {
      if (i.length() > best) best = i.length();
    }

    return best;
  }

  public static String normalise(String str) {
    return stripGlyphs(stripColors(str));
  }

  public static long binary2integer(boolean... list) {
    int out = 0;

    for (int i=0; i<list.length; i++) {
      out |= list[i] ? 1 : 0;
      out <<= 1;
    }
    
    return out >> 1;
  }

  public static boolean[] integer2binary(long number) { return integer2binary(number, 0); }
  public static boolean[] integer2binary(long number, int bits) {
    // Check value because 0 have a negative size  
    if (number == 0) return new boolean[bits == 0 ? 1 : bits];
      
    int size = bits < 1 ? (int) (Math.log(number)/Math.log(2)+1) : bits;
    boolean[] out = new boolean[size];
    
    while (size-- > 0) {
      out[size] = (number & 1) != 0;
      number >>= 1;
    }

    return out;
  }
  
  public static boolean isTrue(String str) {
    switch (str.toLowerCase()) {
      case "1": case "true": case "on": 
      case "enabled": case "activated":
               return true;
      default: return false;
    }
  }
  
  public static boolean isFalse(String str) {
    switch (str.toLowerCase()) {
      case "0": case "false": case "off": 
      case "disabled": case "inactivated":
               return true;
      default: return false;
    }
  }
  
  public static boolean isVersionAtLeast(String currentVersion, String newVersion) {
    if (currentVersion.startsWith("v")) currentVersion = currentVersion.substring(1);
    if (newVersion.startsWith("v")) newVersion = newVersion.substring(1);
    
    int dot = currentVersion.indexOf('.');
    int major1 = parseInt(dot == -1 ? currentVersion : currentVersion.substring(0, dot), 0);
    int minor1 = dot == -1 ? 0 : parseInt(currentVersion.substring(dot + 1), 0);
    dot = newVersion.indexOf('.');
    int major2 = parseInt(dot == -1 ? newVersion : newVersion.substring(0, dot), 0);
    int minor2 = dot == -1 ? 0 : parseInt(newVersion.substring(dot + 1), 0);
    
    return major2 > major1 || (major2 == major1 && minor2 > minor1);
  }
}
