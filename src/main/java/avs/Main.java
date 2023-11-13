package avs;

import java.util.concurrent.ThreadPoolExecutor;

import arc.Core;
import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.util.Threads;
import mindustry.Vars;
import mindustry.net.NetConnection;
import mindustry.net.Packets.ConnectPacket;

import avs.service.AntiVpnService;
import avs.service.IPValidity;
import avs.util.Logger;
import avs.util.Strings;
import avs.util.PVars;


public class Main extends mindustry.mod.Plugin {
  private static int threadPoolSize = 100;
  private static ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Threads.boundedExecutor("IPValidator", threadPoolSize);
  private static String message = "Your ip is flagged as VPN!"; // Message for player
  private static boolean mode = false, // just kick or ban
      enabled = true,
      printIP = true;

  @SuppressWarnings("unchecked")
  public void loadContent() {
    PVars.cacheFolder = Vars.modDirectory.child(Vars.mods.getMod(getClass()).name).child("cache");
    loadSettings();
    AntiVpnService.loadSettins();
    
    // Set size of thread pool
    // This is intended to limit the number of connections at the same time, 
    // It gives a little protection against ddos attacks
    try { threadPool.setMaximumPoolSize(threadPoolSize); }
    // avoid the case of the value has been manually modified in server settings
    catch (IllegalArgumentException e) {}; 
    
    // Takes control of the Connect Packet listener to wrap it
    // And be able to make the client wait while verifying his IP
    try {
      // Get listeners by changing variable as public instead of private
      final java.lang.reflect.Field field = mindustry.net.Net.class.getDeclaredField("serverListeners");
      field.setAccessible(true);
      final Cons2<NetConnection, Object> originalListener = ((ObjectMap<Class<?>, Cons2<NetConnection, Object>>) field.get(null)).get(ConnectPacket.class);

      // Wrap the original listener
      Vars.net.handleServer(ConnectPacket.class, (con, packet) -> {
        // Rewrite some part of the initial listener
        if(con.kicked) return;

        if(con.address.startsWith("steam:")){
            packet.uuid = con.address.substring("steam:".length());
        }
        
        // Just for visual in console
        con.uuid = packet.uuid;
        
        // And submit a new task in the thread pool to avoid blocking the server
        try {
          threadPool.submit(() -> {
            // Check the IP
            IPValidity reply = AntiVpnService.checkIP(con.address);
      
            if (reply.isVPN) {
              con.kick(message + (printIP ? "[lightgray](IP: {0})[]" : ""), 0);
              return;
            }
            
            // Now run the original listener
            if (originalListener != null) Core.app.post(() -> originalListener.get(con, packet));         
          });          
        } catch (java.util.concurrent.RejectedExecutionException e) {
          // To many connection at same time, kick the player and invite it to retry connection
          Logger.debug("Failed to start ip check for client @ [@]", con.address, con.uuid);
          con.kick(mindustry.net.Packets.KickReason.serverRestarting, 0);
        }
      });

    } catch (final ReflectiveOperationException | SecurityException err) {
      Logger.err("A security manager is present in this java version! Cannot put the blacklist listeners first in events list.");
      Logger.err("Please remove the security manager if you want a first priority execution for backlist listener");
      threadPool.shutdown();
      Logger.warn("");
      Logger.warn("Anti VPN Service has been disabled due to previous errors");
    }
  }

  public void init() {

  }

  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    // Command only for server console
    handler.register("anti-vpn", "[help|arg0] [arg1...]", "", args -> {

    });
  }

  public static void loadSettings() {
    if (Core.settings.has("avs-settings")) {
      boolean[] settings = Strings.integer2binary(Core.settings.getInt("avs-settings"));

      // Avoid errors when adding new settings
      try {
        enabled = settings[1];
        printIP = settings[2];
      } catch (IndexOutOfBoundsException e) {
        saveSettings();
      }
    }
  }

  public static void saveSettings() {
    Core.settings.put("avs-settings", 
        Strings.binary2integer(true, enabled, printIP));
  }
}
