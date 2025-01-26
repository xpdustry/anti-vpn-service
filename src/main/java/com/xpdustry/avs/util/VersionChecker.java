/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
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

import com.xpdustry.avs.util.network.AdvancedHttp;


public class VersionChecker {
  private static final Logger logger = new Logger("Updater");
  
  public static String keyToFind = "tag_name";
  public static String repoLinkFormat = "https://github.com" + "/@/releases/latest";
  public static String repoApiLinkFormat = mindustry.Vars.ghApi + "/repos/@/releases/latest";
  
  public static void checkAndPromptToUpgrade(String githubRepo, String currentVersion) {
    logger.info("avs.updater.checking");
    
    // Check the githubRepo and the current version
    if (githubRepo == null || githubRepo.isBlank()) {
      logger.warn("avs.updater.no-repo-found");
      return;
    } else if (currentVersion == null || currentVersion.isBlank()) {
      logger.warn("avs.updater.no-version-found");
      return;
    }
    
    // Make the request
    AdvancedHttp.Reply reply = AdvancedHttp.get(Strings.format(repoApiLinkFormat, githubRepo));
    
    if (reply.isError()) {
      String message = reply.error != null ? reply.error.toString() : reply.httpStatus + ": " + reply.message;
      logger.err("avs.updater.error", message);
      return;
    } else if (reply.content.isBlank()) {
      logger.err("avs.updater.reply.empty");
      return;
    }
    
    // Extract the version
    String tagName;
    try { tagName = new arc.util.serialization.JsonReader().parse(reply.content).getString(keyToFind); } 
    catch (Exception e) {
      logger.err("avs.updater.reply.no-tagname-found");
      logger.err("avs.general-error", e.getLocalizedMessage());
      return;
    }
    
    // Compare the version
    if (Strings.isVersionAtLeast(currentVersion, tagName)) {
        logger.info("avs.updater.version.found", tagName, currentVersion);
        logger.info("avs.updater.version.link", Strings.format(repoLinkFormat, githubRepo));
    } else logger.info("avs.updater.version.up-to-date");
  }
}
