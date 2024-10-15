/*
 * This file is part of Simple Blacklist. The plugin securing your server against unwanted nicknames.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
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

package com.xpdustry.avs.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter.OutputType;


public class Strings extends arc.util.Strings {
  public static String rJust(String str, int newLenght) { return rJust(str, newLenght, " "); }
  /** Justify string to the right. E.g. "&emsp; right" */
  public static String rJust(String str, int newLenght, String filler) {
    if (filler.length() == 0) return str; // Cannot fill, so return initial string
    return filler.repeat((newLenght-str.length())/filler.length())+filler.substring(0, (newLenght-str.length())%filler.length())+str;
  }
  public static Seq<String> rJust(Seq<String> list, int newLenght) { return rJust(list, newLenght, " "); }
  public static Seq<String> rJust(Seq<String> list, int newLenght, String filler) {
    return list.map(str -> rJust(str, newLenght, filler));
  }

  public static String lJust(String str, int newLenght) { return lJust(str, newLenght, " "); }
  /** Justify string to the left. E.g. "left &emsp;" */
  public static String lJust(String str, int newLenght, String filler) {
    if (filler.length() == 0) return str; // Cannot fill, return initial string
    return str+filler.repeat((newLenght-str.length())/filler.length())+filler.substring(0, (newLenght-str.length())%filler.length());
  }
  public static Seq<String> lJust(Seq<String> list, int newLenght) { return lJust(list, newLenght, " "); }
  public static Seq<String> lJust(Seq<String> list, int newLenght, String filler) {
    return list.map(str -> lJust(str, newLenght, filler));
  }

  public static String mJust(String left, String right, int newLenght) { return mJust(left, right, newLenght, " "); }
  /** Justify string at middle. E.g. "left &emsp; right" */
  public static String mJust(String left, String right, int newLenght, String filler) {
    if (filler.length() == 0) return left+right; // Cannot fill, so return concatened sides
    int s = newLenght-left.length()-right.length();
    return left+filler.repeat(s/filler.length())+filler.substring(0, s%filler.length())+right;
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
  
  /** @return whether the specified string mean true */
  public static boolean isTrue(String str) {
    switch (str.toLowerCase()) {
      case "1": case "true": case "on": 
      case "enable": case "activate":
               return true;
      default: return false;
    }
  }
  
  /** @return whether the specified string mean false */
  public static boolean isFalse(String str) {
    switch (str.toLowerCase()) {
      case "0": case "false": case "off": 
      case "disable": case "desactivate":
               return true;
      default: return false;
    }
  }
  
  /** @return whether {@code newVersion} string is greater than {@code currentVersion} string, e.g. "v146" > "120.1" */
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
  
  /** Modified version of {@link arc.util.I18NBundle#toFileHandle()} */
  public static String locale2String(Locale locale) {
    if (locale != null && !locale.equals(Locale.ROOT)) {
      String l = convertOldISOCodes(locale.getLanguage());
      String c = locale.getCountry();
      String v = locale.getVariant();
      boolean el = l.isEmpty();
      boolean ec = c.isEmpty();
      boolean ev = v.isEmpty();
      
      if (!(el && ec && ev)) {
        if (!ev) return l + '_' + c + '_' + v;
        if (!ec) return l + '_' + c;
        return l;      
      }
    }
    
    return "";
  }
  
  public static Locale string2Locale(String tag) {
    if (tag == null) return null;
    if (tag.isEmpty()) return Locale.ROOT;

    String[] codes = tag.replace('-', '_').split("_");
    return codes.length == 1 ? new Locale(codes[0]) : 
           codes.length == 2 ? new Locale(codes[0], codes[1]) :
                               new Locale(codes[0], codes[1], codes[2]);
  }
  
  /** {@link sun.util.locale.BaseLocale#convertOldISOCodes(String)} */
  public static String convertOldISOCodes(String language) {
    return switch (language) {
      case "iw" -> "he";
      case "in" -> "id";
      case "ji" -> "yi";
      default -> language;
    };
  }
  
  public static String jsonPrettyPrint(JsonValue object, OutputType outputType) {
    StringWriter out = new StringWriter();
    try { jsonPrettyPrint(object, out, outputType, 0); } 
    catch (IOException ignored) { return ""; }
    return out.toString();
  }
  
  public static void jsonPrettyPrint(JsonValue object, Writer writer, OutputType outputType) throws IOException {
    jsonPrettyPrint(object, writer, outputType, 0);
  }
  
  /** 
   * Re-implementation of {@link JsonValue#prettyPrint(OutputType, Writer)}, 
   * because the ident isn't correct and the max object size before new line, is too big.
   */
  public static void jsonPrettyPrint(JsonValue object, Writer writer, OutputType outputType, int indent) throws IOException {
    if (object.isObject()) {
      if (object.child == null) writer.append("{}");
      else {
        indent++;
        boolean newLines = needNewLine(object, 2);
        writer.append(newLines ? "{\n" : "{ ");
        for (JsonValue child = object.child; child != null; child = child.next) {
          if(newLines) writer.append("  ".repeat(indent));
          writer.append(outputType.quoteName(child.name));
          writer.append(": ");
          jsonPrettyPrint(child, writer, outputType, indent);
          if((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',');
          writer.append(newLines ? '\n' : ' ');
        }
        if(newLines) writer.append("  ".repeat(indent - 1));
        writer.append('}');
      }
    } else if (object.isArray()) {
      if (object.child == null) writer.append("[]");
      else {
        indent++;
        boolean newLines = needNewLine(object, 4);
        writer.append(newLines ? "[\n" : "[ ");
        for (JsonValue child = object.child; child != null; child = child.next) {
          if (newLines) writer.append("  ".repeat(indent));
          jsonPrettyPrint(child, writer, outputType, indent);
          if ((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',');
          writer.append(newLines ? '\n' : ' ');
        }
        if (newLines) writer.append("  ".repeat(indent - 1));
        writer.append(']');
      }
    } else if(object.isString()) writer.append(outputType.quoteValue(object.asString()));
    else if(object.isDouble()) writer.append(Double.toString(object.asDouble()));
    else if(object.isLong()) writer.append(Long.toString(object.asLong()));
    else if(object.isBoolean()) writer.append(Boolean.toString(object.asBoolean()));
    else if(object.isNull())  writer.append("null");
    else throw new arc.util.serialization.SerializationException("Unknown object type: " + object);
  }
  
  private static boolean needNewLine(JsonValue object, int maxChildren){
    for(JsonValue child = object.child; child != null; child = child.next) 
      if(child.isObject() || child.isArray() || maxChildren-- <= 0) return true;
    return false;
  }
  
  
  public static Fi getFiChild(Fi parent, String path) {
    if (parent == null) throw new NullPointerException("parent cannot be null");
    if (path == null || path.isEmpty()) return parent;
    Fi folder = new Fi(path);
    return folder.file().isAbsolute() ? folder : parent.child(path);
  }
  
  private static Pattern placeholders = Pattern.compile("(\\{[0-9]+[, a-z]*[#]?\\})"),
                         escapeBraces = Pattern.compile("((?:\\{{2})+)");

  /** 
   * Do the same job as {@link arc.util.TextFormatter}, but with regex, and can add colors around placeholders.
   * 
   * @apiNote if {@code leftColor} and {@code leftColor} are empty or null, will format without colors
   */
  public static String formatWithColor(MessageFormat format, String text, String leftColor, String rightColor, Object... args) {
    text = escapeBraces.matcher(text.replace("'", "''")).replaceAll("'$1'");
    if (leftColor == null) leftColor = "";
    if (rightColor == null) rightColor = "";
    if (!leftColor.isEmpty() || !rightColor.isEmpty()) 
      text = placeholders.matcher(text).replaceAll(leftColor + "$1" + rightColor);
    format.applyPattern(text);
    return format.format(args);
  }
  
  /** Convert the object into a string. If the string is empty, return double-qotes (""). */
  public static String objToStr(Object o) {
    String str = String.valueOf(o);
    if (str.isBlank()) str = "\"\"";
    return str;
  }
  
  public static <T> String listToSentence(Iterable<T> list) { return listToSentence(list, String::valueOf); }
  /** Convert a list to a human readable sentence. E.g. [1, 2, 3, 4, 5] -> "1, 2, 3, 4 and 5" */
  public static <T> String listToSentence(Iterable<T> list, arc.func.Func<T, String> stringifier) {
    java.util.Iterator<T> iter = list.iterator();
    if (!iter.hasNext()) return "";
    
    StringBuilder builder = new StringBuilder(stringifier.get(iter.next()));
    
    while (iter.hasNext()) {
      T tmp = iter.next();
      builder.append(iter.hasNext() ? ", " : " and ");
      builder.append(stringifier.get(tmp));
    }
    
    return builder.toString();
  }
  
  /** Simple method to validate a path, is by using {@link java.nio.file.Paths#get(String, String...)} */
  public static boolean isValidPath(String path) {
    if (path == null || path.isBlank()) return false;

    try { new java.io.File(path).getCanonicalPath(); }
    catch (Exception e) { return false; }
    return true;
  }
}
