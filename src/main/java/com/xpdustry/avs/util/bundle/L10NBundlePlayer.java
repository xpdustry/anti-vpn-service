/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
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

package com.xpdustry.avs.util.bundle;

import arc.func.Boolf;

import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.NetConnection;


/** 
 * Advanced {@link L10NBundle} that add methods for handling players. <br>
 * Taken from the <a href="https://github.com/xzxADIxzx/Useful-Stuffs"> xzxADIxzx's Useful-Stuffs repo</a>. 
 */
public class L10NBundlePlayer extends L10NBundle {
  public static Bundle getBundle(Player player) {
    return player == null ? getDefaultBundle() : getBundle(player.locale);
  }

  public static boolean has(String key, Player player) {
    return getBundle(player).has(key);
  }

  public static String get(String key, Player player) {
    return getBundle(player).get(key);
  }

  public static String format(String key, Player player, Object... values) {
    return getBundle(player).format(key, values);
  }
  
  public static String formatColor(String key, Player player, Object... values) {
    return getBundle(player).formatColor(getDefaultFormatter(), key, "[blue]", "[]", values);
  }
  
  public static String formatColor(String key, Player player, String leftColor, String rightColor, Object... values) {
    return getBundle(player).formatColor(getDefaultFormatter(), key, leftColor, rightColor, values);
  }
  
  public static String format(String key, java.text.MessageFormat formatter, Player player, Object... values) {
    return getBundle(player).format(formatter, key, values);
  }

  // region con
  
  public static void infoMessage(NetConnection con, String locale, String key) {
    Call.infoMessage(con, get(key, locale));
  }

  public static void infoMessage(NetConnection con, String locale, String key, Object... values) {
    Call.infoMessage(con, format(key, locale, values));
  }

  public static void setHud(NetConnection con, String locale, String key) {
    Call.setHudText(con, get(key, locale));
  }

  public static void setHud(NetConnection con, String locale, String key, Object... values) {
    Call.setHudText(con, format(key, locale, values));
  }

  public static void announce(NetConnection con, String locale, String key) {
    Call.announce(con, get(key, locale));
  }

  public static void announce(NetConnection con, String locale, String key, Object... values) {
    Call.announce(con, format(key, locale, values));
  }

  public static void toast(NetConnection con, String locale, int icon, String key) {
    Call.warningToast(con, icon, get(key, locale));
  }

  public static void toast(NetConnection con, String locale, int icon, String key, Object... values) {
    Call.warningToast(con, icon, format(key, locale, values));
  }

  public static void label(NetConnection con, String locale, float duration, float x, float y, String key) {
    Call.label(con, get(key, locale), duration, x, y);
  }

  public static void label(NetConnection con, String locale, float duration, float x, float y, String key, Object... values) {
    Call.label(con, format(key, locale, values), duration, x, y);
  }

  public static void popup(NetConnection con, String locale, float duration, int align, int top, int left, int bottom, int right, String key) {
    Call.infoPopup(con, get(key, locale), duration, align, top, left, bottom, right);
  }

  public static void popup(NetConnection con, String locale, float duration, int align, int top, int left, int bottom, int right, String key, Object... values) {
    Call.infoPopup(con, format(key, locale, values), duration, align, top, left, bottom, right);
  }

  public static void kick(NetConnection con, String locale, String key) {
    con.kick(get(key, locale), 0L);
  }

  public static void kick(NetConnection con, String locale, String key, Object... values) {
    con.kick(format(key, locale, values), 0L);
  }

  public static void kick(NetConnection con, String locale, long duration, String key) {
    con.kick(get(key, locale), duration);
  }

  public static void kick(NetConnection con, String locale, long duration, String key, Object... values) {
    con.kick(format(key, locale, values), duration);
  }

  // endregion  
  // region single

  public static void send(Player player, String key) {
    player.sendMessage(get(key, player));
  }

  public static void send(Player player, String key, Object... values) {
    player.sendMessage(format(key, player, values));
  }

  public static void sendFrom(Player player, Player from, String text, String key) {
    player.sendMessage(format(key, player, text), from, text);
  }

  public static void sendFrom(Player player, Player from, String text, String key, Object... values) {
    player.sendMessage(format(key, player, arc.util.Structs.add(values, text)), from, text);
  }

  public static void infoMessage(Player player, String key) {
    Call.infoMessage(player.con, get(key, player));
  }

  public static void infoMessage(Player player, String key, Object... values) {
    Call.infoMessage(player.con, format(key, player, values));
  }

  public static void setHud(Player player, String key) {
    Call.setHudText(player.con, get(key, player));
  }

  public static void setHud(Player player, String key, Object... values) {
    Call.setHudText(player.con, format(key, player, values));
  }

  public static void announce(Player player, String key) {
    Call.announce(player.con, get(key, player));
  }

  public static void announce(Player player, String key, Object... values) {
    Call.announce(player.con, format(key, player, values));
  }

  public static void toast(Player player, int icon, String key) {
    Call.warningToast(player.con, icon, get(key, player));
  }

  public static void toast(Player player, int icon, String key, Object... values) {
    Call.warningToast(player.con, icon, format(key, player, values));
  }

  public static void label(Player player, float duration, float x, float y, String key) {
    Call.label(player.con, get(key, player), duration, x, y);
  }

  public static void label(Player player, float duration, float x, float y, String key, Object... values) {
    Call.label(player.con, format(key, player, values), duration, x, y);
  }

  public static void popup(Player player, float duration, int align, int top, int left, int bottom, int right, String key) {
    Call.infoPopup(player.con, get(key, player), duration, align, top, left, bottom, right);
  }

  public static void popup(Player player, float duration, int align, int top, int left, int bottom, int right, String key, Object... values) {
    Call.infoPopup(player.con, format(key, player, values), duration, align, top, left, bottom, right);
  }

  public static void kick(Player player, String key) {
    player.kick(get(key, player), 0L);
  }

  public static void kick(Player player, String key, Object... values) {
    player.kick(format(key, player, values), 0L);
  }

  public static void kick(Player player, long duration, String key) {
    player.kick(get(key, player), duration);
  }

  public static void kick(Player player, long duration, String key, Object... values) {
    player.kick(format(key, player, values), duration);
  }
  
  // endregion
  // region group

  public static void send(String key, Object... values) {
    Groups.player.each(player -> send(player, key, values));
  }

  public static void send(Boolf<Player> filter, String key, Object... values) {
    Groups.player.each(filter, player -> send(player, key, values));
  }

  public static void sendFrom(Player from, String text, String key, Object... values) {
    Groups.player.each(player -> sendFrom(player, from, text, key, values));
  }

  public static void sendFrom(Boolf<Player> filter, Player from, String text, String key, Object... values) {
    Groups.player.each(filter, player -> sendFrom(player, from, text, key, values));
  }

  public static void infoMessage(String key, Object... values) {
    Groups.player.each(player -> infoMessage(player, key, values));
  }

  public static void infoMessage(Boolf<Player> filter, String key, Object... values) {
    Groups.player.each(filter, player -> infoMessage(player, key, values));
  }

  public static void setHud(String key, Object... values) {
    Groups.player.each(player -> setHud(player, key, values));
  }

  public static void setHud(Boolf<Player> filter, String key, Object... values) {
    Groups.player.each(filter, player -> setHud(player, key, values));
  }

  public static void announce(String key, Object... values) {
    Groups.player.each(player -> announce(player, key, values));
  }

  public static void announce(Boolf<Player> filter, String key, Object... values) {
    Groups.player.each(filter, player -> announce(player, key, values));
  }

  public static void toast(int icon, String key, Object... values) {
    Groups.player.each(player -> toast(player, icon, key, values));
  }

  public static void toast(Boolf<Player> filter, int icon, String key, Object... values) {
    Groups.player.each(filter, player -> toast(player, icon, key, values));
  }

  public static void label(float duration, float x, float y, String key, Object... values) {
    Groups.player.each(player -> label(player, duration, x, y, key, values));
  }

  public static void label(Boolf<Player> filter, float duration, float x, float y, String key, Object... values) {
    Groups.player.each(filter, player -> label(player, duration, x, y, key, values));
  }

  public static void popup(float duration, int align, int top, int left, int bottom, int right, String key, Object... values) {
    Groups.player.each(player -> popup(player, duration, align, top, left, bottom, right, key, values));
  }

  public static void popup(Boolf<Player> filter, float duration, int align, int top, int left, int bottom, int right, String key, Object... values) {
    Groups.player.each(filter, player -> popup(player, duration, align, top, left, bottom, right, key, values));
  }

  // endregion
}
