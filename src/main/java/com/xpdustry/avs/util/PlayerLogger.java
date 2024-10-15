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

package com.xpdustry.avs.util;

import com.xpdustry.avs.util.bundle.L10NBundlePlayer;

import arc.util.Log.LogLevel;

import mindustry.gen.Player;


/** Class to redirect logging messages to a player, with the same usage as {@link Logger} */
public class PlayerLogger extends Logger {
  public final Player player;
  
  public PlayerLogger(Player player) {
    this.player = player;
  }
  
  /** Send a message to the player */
  public void send(String text, Object... args) {
    player.sendMessage(Strings.format(text.replace("@", "[blue]@[]"), args));
  }  
  
  @Override
  public void logNormal(LogLevel level, String text, Object... args) {
    if (level == LogLevel.none) return;
    String prefix = level == LogLevel.debug ? "[cyan]" :
                    level == LogLevel.info ? "" :
                    level == LogLevel.warn ? "[orange]" :
                    level == LogLevel.err ? "[scarlet]":
                    "";
    send(prefix + text, args);
  }

  @Override
  public String getKey(String key) {
    return L10NBundlePlayer.get(key, player);
  }
  
  @Override
  public String formatKey(String key, Object... args) {
    return L10NBundlePlayer.formatColor(key, player, args);
  }
}
