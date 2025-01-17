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

package com.xpdustry.avs.command.list;

import arc.util.Timer;


public class ResetCommand extends com.xpdustry.avs.command.Command {
  private static Timer.Task resetTask;
  private static boolean resetRequested = false,
                         resetConfirmed = false;
  
  public ResetCommand() { super("reset"); }

  @Override
  public void run(String[] args, com.xpdustry.avs.util.Logger logger, boolean restrictedMode) {
    if (!com.xpdustry.avs.config.AVSConfig.resetCommandEnabled.getBool() || resetConfirmed) {
      logger.err("avs.command.reset.disabled");
      return;
    } else if (args.length != 0 && !resetRequested) {
      logger.err("avs.command.reset.no-args-pls");
      return;
    }
    //TODO: check if is the same player who started the reset
    
    if (resetRequested) {
      resetRequested = false;
      if (resetTask != null) resetTask.cancel();
      
      if (args.length == 0) logger.err("avs.command.reset.invalid-usage");
      else if (args[0].equals("no")) logger.info("avs.command.reset.canceled");
      else if (!args[0].equals("yes")) logger.err("avs.command.reset.invalid-usage");
      else {
        logger.info("avs.command.reset.confirmed");
        resetConfirmed = true;
        arc.Events.fire(new com.xpdustry.avs.misc.AVSEvents.AVSResetEvent());
      }  

    } else {
      // Warn the user
      logger.infoNormal("");
      logger.info("avs.command.reset.warn1");
      logger.info("avs.command.reset.warn2");
      logger.info("avs.command.reset.warn3");
      resetTask = Timer.schedule(() -> {
        resetRequested = false;
        logger.info("avs.command.reset.canceled");
      }, 10);
      resetRequested = true;
      resetConfirmed = false;
    }
  }
  
  /** Used by {@link com.xpdustry.avs.service.ServiceManager} to check whether the reset is confirmed only by the user. */
  public static boolean isResetConfirmed() {
    return resetConfirmed;
  }
}
