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

package com.xpdustry.avs.command.list;

import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.type.AddressProviderReply;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.logging.Logger;
import com.xpdustry.avs.util.logging.PlayerLogger;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;


public class InfoCommand extends com.xpdustry.avs.command.Command {
  private static String publicAddress;
  private static ObjectMap<String, PlayerInfo> playerInfos;
  
  public InfoCommand() { super("info"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) { 
    Seq<Helper> queries = new Seq<>();
    String query = String.join(" ", args);
    
    logger.info("avs.command.info.wait");
    
    if (args.length == 0) {
      if (logger instanceof PlayerLogger) {
        mindustry.gen.Player player = ((PlayerLogger) logger).player;
        queries.add(new Helper(player.con.address, Vars.netServer.admins.getInfoOptional(player.uuid())));
        
      } else if (publicAddress == null) {
        // Get the public address by using an api and cache the result
        AdvancedHttp.Reply reply = AdvancedHttp.get("http://api.ipify.org");
        if (reply.isError()) {
          logger.err("avs.command.info.public-address-failed");
          return;
        }
        queries.add(new Helper(publicAddress = reply.content, null));
        
      } else queries.add(new Helper(publicAddress, null));

    } else {
      ObjectMap<String, PlayerInfo> infos;
      
      // Try to get access of player infos, else try to get a copy of the list.
      try {
        if (playerInfos == null) 
          playerInfos = arc.util.Reflect.get(Vars.netServer.admins, "playerInfo");   
        infos = playerInfos;
      } catch (RuntimeException e) {
        logger.debug("avs.command.info.player-info-err");
        infos = Vars.netServer.admins.findByName("").toSeq().asMap(i -> i.id);
      }

      // Search name and uuid
      String name = Strings.normalise(query);
      infos.each((id, info) -> {
        if (info.names.contains(n -> Strings.normalise(n).equalsIgnoreCase(name)) ||
            info.id.equals(query))
          queries.addUnique(new Helper(info.lastIP, info));
      });
      
      // Try with IP (only for console)
      if (queries.isEmpty() && !restrictedMode) {
        try { 
          com.xpdustry.avs.misc.address.AddressValidity.checkAddress(query); 
          queries.add(new Helper(query, null));
        } catch (Exception ignored) {}
      }
      
      logger.info("avs.command.info.matches", queries.size);
    }
    
    
    if (!queries.isEmpty()) {
      queries.each(h -> h.reply = AntiVpnService.checkAddressOnline(h.address));
      logger.none();
      for (int i=0; i<queries.size; i++) 
        logger.infoNormal("[" + i + "] " + queries.get(i).toString(logger, !restrictedMode));
    }
  }
  
  
  private static class Helper {
    final String address;
    final PlayerInfo info;
    AddressProviderReply reply;
    
    Helper(String address, PlayerInfo info) {
      this.address = address;
      this.info = info;
    }
    
    String toString(Logger logger, boolean addLocation) {
      StringBuilder builder = new StringBuilder();

      if (info != null) 
        builder.append(logger.formatKey("avs.command.info.print.head", 
                                        Strings.normalise(info.lastName), info.id));
      builder.append('\n');
      
      if (reply == null || !reply.resultFound()) {
       builder.append(logger.formatKey("avs.address-format.address", 
           reply == null ? address : reply.address)).append('\n');
       builder.append(logger.formatKey("avs.address-format.error", 
           reply == null ? "no reply" : reply.type.toString().toLowerCase().replace('_', ' ')))
              .append('\n');
        
      } else reply.validity.toFormattedString(builder, logger, addLocation);

      return builder.toString();
    }
  }
}
