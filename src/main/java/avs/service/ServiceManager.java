package avs.service;

import java.util.concurrent.ThreadPoolExecutor;

import arc.func.Cons2;
import arc.util.Reflect;
import arc.util.Threads;
import arc.util.Time;

import mindustry.Vars;
import mindustry.net.NetConnection;
import mindustry.net.Packets.ConnectPacket;
import mindustry.net.Packets.KickReason;

import avs.config.PVars;
import avs.util.Strings;
import avs.util.DynamicSettings;
import avs.util.Logger;
import avs.util.address.AddressValidity;


public class ServiceManager {
  private static ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Threads.boundedExecutor("IPValidator", PVars.threadPoolSize);
  private static Logger logger = new Logger();
  
  public static boolean registerServerListeners() {
    setPoolSize();
    try { registerVpnCheckerListener(); } 
    catch (RuntimeException err) {
      logger.err("A security manager is present in this java version! Cannot put the blacklist listeners first in events list.");
      logger.err(err);
      logger.warn("");
      logger.warn("Anti VPN Service has been disabled due to previous errors");
      logger.info("Shutting down IP Validator...");
      Threads.await(threadPool);
      return false;
    }
    registerServerExitListener();
    return true;
  }
  
  /* Set size of thread pool */
  public static void setPoolSize() {
    // This is intended to limit the number of connections at the same time, 
    // It gives a little protection against ddos attacks
    try { threadPool.setMaximumPoolSize(PVars.threadPoolSize); }
    // avoid the case of the value has been manually modified in server settings
    catch (IllegalArgumentException e) {}; 
  }
  
  /* Takes control of the Connect Packet listener to wrap it
   * And be able to make the client wait while verifying his IP 
   */
  public static void registerVpnCheckerListener() throws RuntimeException {
    // Get listeners by changing variable as public instead of private
    @SuppressWarnings("unchecked")
    final Cons2<NetConnection, Object> originalListener = ((arc.struct.ObjectMap<Class<?>, Cons2<NetConnection, Object>>) Reflect.get(Vars.net, "serverListeners")).get(ConnectPacket.class);

    // Wrap the original listener
    Vars.net.handleServer(ConnectPacket.class, (con, packet) -> {
      if (!AntiVpnService.operational) {
        logger.warn("Receive connection from client @, but the service is not operational.", con.address);
        con.kick(KickReason.serverRestarting);
        return;
      }
      
      // Rewrite some part of the initial listener
      if(con.kicked) return;
      if(con.address.startsWith("steam:")) packet.uuid = con.address.substring("steam:".length());
      if(packet.uuid == null || packet.usid == null) {
        con.kick(KickReason.idInUse);
        return;
      }
      
      // Just for visual in console
      con.uuid = packet.uuid;
      
      // Also check if the client has been recently kicked
      mindustry.net.Administration.PlayerInfo infos = Vars.netServer.admins.getInfoOptional(con.uuid);
      if(Time.millis() < Math.max(infos == null ? 0L : infos.lastKicked, Vars.netServer.admins.kickedIPs.get(con.address, 0L))){
        con.kick(KickReason.recentKick);
        return;
      } else Vars.netServer.admins.kickedIPs.remove(PVars.kickMessage);
      
      // And submit a new task in the thread pool to avoid blocking the server
      try {
        threadPool.submit(() -> {
          // Check the IP
          AddressValidity reply = AntiVpnService.checkIP(con.address);
    
          if (reply != null && reply.type.isNotValid()) {
            logger.info("Detected a blacklisted ip for client @ [@]!", con.address, con.uuid);
            // Kick the client without duration to avoid creating an empty account, but still register an kick duration
            con.kick(PVars.kickMessage + (PVars.printIP ? Strings.format(PVars.ipFormat, con.address): ""), infos == null ? 0 : PVars.clientRecentKickDuration);
            if (infos == null) 
              Vars.netServer.admins.kickedIPs.put(con.address, Math.max(Vars.netServer.admins.kickedIPs.get(con.address, 0L), Time.millis() + PVars.clientRecentKickDuration));
            return;
          } else logger.info("Check passed for client @ [@]", con.address, con.uuid);
          
          // Now run the original listener
          if (originalListener != null) arc.Core.app.post(() -> originalListener.get(con, packet));         
        });          
      } catch (java.util.concurrent.RejectedExecutionException e) {
        // To many connection at same time, kick the player and invite it to retry connection
        logger.warn("Failed to stats ip for client @ [@]", con.address, con.uuid);
        con.kick(PVars.serverBusyMessage, infos == null ? 0 : PVars.clientRecentKickDuration);
        if (infos == null) 
          Vars.netServer.admins.kickedIPs.put(con.address, Math.max(Vars.netServer.admins.kickedIPs.get(con.address, 0L), Time.millis() + PVars.clientRecentKickDuration));
      }
    });
  }
  
  /* Register an listener when server is existing, to save settings and cache */
  public static void registerServerExitListener() {
    arc.Core.app.addListener(new arc.ApplicationListener() {
      @Override
      public void dispose() {
        logger.info("Waiting for IP checks to be completed...");
        Threads.await(threadPool);
        DynamicSettings.autosaveThread.interrupt();
        logger.info("Saving providers...");
        AntiVpnService.saveProviders();
        logger.info("Saving settings...");
        PVars.saveSettings();
        logger.debug("Writing settings files...");
        DynamicSettings.forceGlobalAutosave();
      }
    });    
  }
}