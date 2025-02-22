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

import com.xpdustry.avs.util.logging.Logger;
import com.xpdustry.avs.util.network.AdvancedHttp;


public class VersionChecker {
  private static final Logger logger = new Logger("Updater");
  
  public static String keyToFind = "tag_name";
  public static String repoLinkFormat = "https://github.com/@/releases/latest";
  public static String repoApiLinkFormat = mindustry.Vars.ghApi + "/repos/@/releases/latest";
  /** Whether to log update status to user */
  public static boolean promptStatus = true;
  
  /** 
   * Check for update using the "version" and "repo" properties 
   * in the mod/plugin definition (plugin.json, mod.json). <br><br>
   * The github repo must be formatted like that "{@code <username>/<repo-name>}".<br>
   * The version can be formatted like that "{@code 146.2}" and can starts with "{@code v}", 
   * but must not contains letters, like "{@code beta}" or "{@code -dev}".
   * 
   * @return the update state
   */
  public static UpdateState checkAndPromptToUpgrade(mindustry.mod.Mods.ModMeta mod) {
    if (promptStatus) logger.info("avs.updater.checking");
    
    // Check the repo and the current version
    if (mod.repo == null || mod.repo.isEmpty()) {
      if (promptStatus) logger.warn("avs.updater.no-repo-found");
      return UpdateState.missing;
    } else if (mod.version == null || mod.version.isEmpty()) {
      if (promptStatus) logger.warn("avs.updater.no-version-found");
      return UpdateState.missing;
    }
    
    // Make the request
    AdvancedHttp.Reply reply = AdvancedHttp.get(Strings.format(repoApiLinkFormat, mod.repo));
    
    if (reply.isError()) {
      String message = reply.error != null ? reply.error.toString() : reply.httpStatus + ": " + reply.message;
      if (promptStatus) logger.err("avs.updater.error", message);
      return UpdateState.error;
    } else if (reply.content.isBlank()) {
      if (promptStatus) logger.err("avs.updater.empty");
      return UpdateState.error;
    }
    
    // Extract the version
    String tagName;
    try { tagName = new arc.util.serialization.JsonReader().parse(reply.content).getString(keyToFind); } 
    catch (Exception e) {
      if (promptStatus) {
        logger.err("avs.updater.no-tagname-found");
        logger.err("avs.general-error", e.getLocalizedMessage());
      }
      return UpdateState.error;
    }
    
    // Compare the version
    if (Strings.isVersionAtLeast(mod.version, tagName)) {
      if (promptStatus) {
        logger.info("avs.updater.found", tagName, mod.version);
        logger.info("avs.updater.link", Strings.format(repoLinkFormat, mod.repo));
      }
      return UpdateState.outdated;
    } else {
      if (promptStatus) logger.info("avs.updater.up-to-date");
      return UpdateState.uptodate;
    }
  }
  
  
  public static enum UpdateState {
    /** "version" or/and "repo" properties are missing in the mod/plugin definition. */
    missing,
    /** Error while checking for updates. */
    error, 
    /** No new updates found, it's the latest version. */
    uptodate,
    /** An update was found, the mod/plugin needs to be upgraded. */
    outdated
  }
}
