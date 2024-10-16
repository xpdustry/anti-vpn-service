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

package com.xpdustry.avs;

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.util.Logger;

import arc.util.CommandHandler;


public class Main extends mindustry.mod.Plugin {
  private static final Logger logger = new Logger(true);
  
  public void init() {
    long start = System.currentTimeMillis();
    logger.infoNormal("");
    logger.infoNormal("&lg----------------------------------------------------------------");
    // For the first message, no bundle key is used.
    logger.infoNormal("Anti VPN Service (AVS) is loading..."/*"avs.loading.started"*/);
    logger.infoNormal("");

    Loader.load(getClass());

    if (Loader.done()) {
      logger.infoNormal("");
      logger.info("avs.loading.finished", Math.max(1, (System.currentTimeMillis()-start)/1000));
      logger.infoNormal("&lg----------------------------------------------------------------");
      logger.infoNormal("");      
    }
  }

  /** Register any commands to be used on the server side, e.g. from the console. */
  @Override
  public void registerServerCommands(CommandHandler handler) {
    AVSCommandManager.registerServer(handler);
  }

  /** Register any commands to be used on the client side, e.g. sent from an in-game player.. */
  @Override
  public void registerClientCommands(CommandHandler handler){
    AVSCommandManager.registerClient(handler);
  }
}
