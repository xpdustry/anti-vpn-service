package com.xpdustry.avs.service.providers.type;

import com.xpdustry.avs.misc.address.AddressValidity;


public class AddressProviderReply {
  public final String address;
  public AddressValidity validity;
  public ReplyType type = ReplyType.NOT_FOUND;
  
  public AddressProviderReply(String address) {
    this.address = address;
  }
  
  public boolean resultFound() {
    return type == ReplyType.FOUND;
  }
  
  public boolean hasResult() {
    return validity != null;
  }
  
  public void setResult(AddressValidity result) {
    validity = result;
    type = result == null ? ReplyType.NOT_FOUND : ReplyType.FOUND;
  }
  
  
  public static enum ReplyType {
    FOUND, NOT_FOUND, ERROR, UNAVAILABLE;
  }
}
