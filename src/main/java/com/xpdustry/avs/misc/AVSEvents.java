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

package com.xpdustry.avs.misc;

import java.time.Duration;

import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.service.providers.type.*;
import com.xpdustry.avs.util.network.AdvancedHttp;

import mindustry.net.NetConnection;
import mindustry.net.Packets.ConnectPacket;


public class AVSEvents {
  public static class AVSResetEvent {}
  
  public static class AVSLoadingEvent {}
  public static class AVSLoadingFailedEvent {}
  public static class AVSLoadedEvent {}
  
  public static class ProvidersLoadedEvent {}
  public static class ProvidersSavedEvent {}
  
  public static class ProviderEnabledEvent {
    public final AddressProvider provider;
    
    public ProviderEnabledEvent(AddressProvider provider) {
      this.provider = provider;
    }
  }
  public static class ProviderDisabledEvent {
    public final AddressProvider provider;
    
    public ProviderDisabledEvent(AddressProvider provider) {
      this.provider = provider;
    }
  }
  
  public static class ProviderLoadingEvent {
    public final AddressProvider provider;
    
    public ProviderLoadingEvent(AddressProvider provider) {
      this.provider = provider;
    }
  }
  public static class ProviderReloadingEvent {
    public final AddressProvider provider;
    
    public ProviderReloadingEvent(AddressProvider provider) {
      this.provider = provider;
    }
  }
  public static class ProviderSavingEvent {
    public final AddressProvider provider;
    
    public ProviderSavingEvent(AddressProvider provider) {
      this.provider = provider;
    }
  }
  
  public static class CloudProviderRefreshingEvent {
    public final CloudDownloadedProvider provider;
    
    public CloudProviderRefreshingEvent(CloudDownloadedProvider provider) {
      this.provider = provider;
    }
  }
  public static class CloudProviderRefreshedEvent {
    public final CloudDownloadedProvider provider;
    
    public CloudProviderRefreshedEvent(CloudDownloadedProvider provider) {
      this.provider = provider;
    }
  }
  
  
  public static class EditableProviderAddedAddressEvent {
    public final EditableAddressProvider provider;
    public final AddressValidity address;
    public final boolean added;
    
    public EditableProviderAddedAddressEvent(EditableAddressProvider provider, AddressValidity address, 
                                      boolean added) {
      this.provider = provider;
      this.address = address;
      this.added = added;
    }
  }
  public static class EditableProviderRemovedAddressEvent {
    public final EditableAddressProvider provider;
    public final AddressValidity address;
    public final boolean removed;
    
    public EditableProviderRemovedAddressEvent(EditableAddressProvider provider, AddressValidity address, 
                                      boolean added) {
      this.provider = provider;
      this.address = address;
      this.removed = added;
    }
  }
  public static class EditableProviderCleaningAddressesEvent {
    public final EditableAddressProvider provider;
    
    public EditableProviderCleaningAddressesEvent(EditableAddressProvider provider) {
      this.provider = provider;
    }
  }
  
  public static class OnlineProviderAddedTokenEvent {
    public final OnlineServiceProvider provider;
    public final String token;
    
    public OnlineProviderAddedTokenEvent(OnlineServiceProvider provider, String token) {
      this.provider = provider;
      this.token = token;
    }
  }
  public static class OnlineProviderRemovedTokenEvent {
    public final OnlineServiceProvider provider;
    public final String token;
    
    public OnlineProviderRemovedTokenEvent(OnlineServiceProvider provider, String token) {
      this.provider = provider;
      this.token = token;
    }
  }

  public static class ClientCheckEvent {
    public final NetConnection con;
    public final ConnectPacket packet;
    
    public ClientCheckEvent(NetConnection con, ConnectPacket packet) {
      this.con = con;
      this.packet = packet;
    }
  }
  public static class ClientCheckFailedEvent {
    public final NetConnection con;
    public final ConnectPacket packet;
    public final Throwable error;
    
    public ClientCheckFailedEvent(NetConnection con, ConnectPacket packet, Throwable error) {
      this.con = con;
      this.packet = packet;
      this.error = error;
    }
  }
  
  public static class ClientRejectedEvent {
    public final NetConnection con;
    public final ConnectPacket packet;
    public final boolean becauseBusy;
    public final @arc.util.Nullable AddressProviderReply reply;
    
    public ClientRejectedEvent(NetConnection con, ConnectPacket packet, 
                               boolean becauseBusy, AddressProviderReply reply) {
      this.con = con;
      this.packet = packet;
      this.becauseBusy = becauseBusy;
      this.reply = reply;
    }
  }
  public static class ClientAcceptedEvent {
    public final NetConnection con;
    public final ConnectPacket packet;
    public final AddressProviderReply reply;
    
    public ClientAcceptedEvent(NetConnection con, ConnectPacket packet, AddressProviderReply reply) {
      this.con = con;
      this.packet = packet;
      this.reply = reply;
    }
  }
  
  public static class ClientConnectEvent {
    public final NetConnection con;
    public final ConnectPacket packet;
    
    public ClientConnectEvent(NetConnection con, ConnectPacket packet) {
      this.con = con;
      this.packet = packet;
    }
  }
  
  public static class AddressCheckStartedEvent {
    public final String address;
    
    public AddressCheckStartedEvent(String address) {
      this.address = address;
    }
  }
  public static class AddressCheckFinishedEvent {
    public final String address;
    public final AddressProviderReply reply;
    
    public AddressCheckFinishedEvent(String address, AddressProviderReply reply) {
      this.address = address;
      this.reply = reply;
    }
  }
  
  public static class ProviderCheckingAddressEvent {
    public final AddressProvider provider;
    public final String address;
    
    public ProviderCheckingAddressEvent(AddressProvider provider, String address) {
      this.provider = provider;
      this.address = address;
    }
  }
  public static class ProviderCheckedAddressEvent {
    public final AddressProvider provider;
    public final String address;
    public final AddressProviderReply reply;
    
    public ProviderCheckedAddressEvent(AddressProvider provider, String address, 
                                             AddressProviderReply reply) {
      this.provider = provider;
      this.address = address;
      this.reply = reply;
    }
  }
  public static class ProviderCheckingAddressFailedEvent {
    public final AddressProvider provider;
    public final String address;
    public final Throwable error;
    
    public ProviderCheckingAddressFailedEvent(AddressProvider provider, String address, Throwable error) {
      this.provider = provider;
      this.address = address;
      this.error = error;
    }
  }
  
  public static class OnlineProviderServiceRequest {
    public final OnlineServiceProvider provider;
    public final AdvancedHttp.Reply reply;
    
    public OnlineProviderServiceRequest(OnlineServiceProvider provider, AdvancedHttp.Reply reply) {
      this.provider = provider;
      this.reply = reply;
    }
  }
  
  public static class OnlineProviderTokenNowUnavailable {
    public final OnlineServiceProvider provider;
    public final String token;
    public final Duration cooldown;
    
    public OnlineProviderTokenNowUnavailable(OnlineServiceProvider provider, String token, Duration cooldown) {
      this.provider = provider;
      this.token = token;
      this.cooldown = cooldown;
    }
  }
  public static class OnlineProviderTokenNowAvailable {
    public final OnlineServiceProvider provider;
    public final String token;
    
    public OnlineProviderTokenNowAvailable(OnlineServiceProvider provider, String token) {
      this.provider = provider;
      this.token = token;
    }
  }
  
  public static class OnlineProviderServiceNowUnavailable {
    public final OnlineServiceProvider provider;
    public final Duration cooldown;
    
    public OnlineProviderServiceNowUnavailable(OnlineServiceProvider provider, Duration cooldown) {
      this.provider = provider;
      this.cooldown = cooldown;
    }
  }
  public static class OnlineProviderServiceNowAvailable {
    public final OnlineServiceProvider provider;
    
    public OnlineProviderServiceNowAvailable(OnlineServiceProvider provider) {
      this.provider = provider;
    }
  }
  
  public static class CloudAutoRefresherStartedEvent {}
  public static class CloudAutoRefresherDoneEvent {
    public final boolean errors; 
    
    public CloudAutoRefresherDoneEvent(boolean errors) {
      this.errors = errors;
    }
  }
  
}
