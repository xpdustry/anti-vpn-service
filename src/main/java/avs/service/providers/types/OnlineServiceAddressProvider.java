package avs.service.providers.types;

import java.text.MessageFormat;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Http;

import avs.service.providers.AddressValidity;
import avs.util.PVars;


public abstract class OnlineServiceAddressProvider extends AddressProvider {
  /* 
   * The URL of service to use. 
   * Don't forgot to add '{0}' = the ip, '{1}' = the token (optional), in the URL.
   */
  protected final String url;
 
  protected ObjectMap<String, Integer> tokens = new ObjectMap<>();
  protected boolean hasTokens = false, available = true;
  protected int unavailableTimeout = 0;
  
  public OnlineServiceAddressProvider(String name, String url) { 
    super(name);
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    this.url = url.strip();
    customFolder = PVars.tokensFolder;
  }
  public OnlineServiceAddressProvider(String displayName, String name, String url) { 
    super(displayName, name); 
    if (url == null || url.isBlank()) throw new NullPointerException("url is empty");
    this.url = url.strip();
    customFolder = PVars.tokensFolder;
  }
  
  @Override
  public boolean reload() {
    tokens.clear();
    return load();
  }
  
  //Cache are tokens for online services
  @Override
  protected boolean loadCache() {
    // No tokens needed. skip this
    if (!hasTokens) {
      logger.info("Service loaded");
      return true;
    }
    
    boolean loaded = true;
    Fi tokensFile = getFile();
    Seq<String> tokens_ = new Seq<>();
    
    if (tokensFile.exists() && !tokensFile.isDirectory()) {
      try { tokens_ = Seq.with(tokensFile.readString().split("\n")).map(line -> line.strip()); }
      catch (Exception e) { 
        logger.err("Failed to load tokens file '@'.", tokensFile.path());
        logger.err("Error: " + e.toString());     
        loaded = false;
      }
    } 
    
    tokens_.removeAll(v -> v.isBlank() || v.startsWith("#"));
    tokens_.each(t -> tokens.put(t, 0));
    
    if (!loaded) logger.err("Failed to load tokens! Skipping this service...");
    else if (tokens.isEmpty()) logger.warn("Service requires tokens, but the list is empty.");
    else logger.info("Loaded @ token" + (tokens.size > 1 ? "s" : "") + ".", tokens.size);
    
    return loaded;
  }
  
  // Cache are tokens for online services
  @Override
  protected boolean saveCache() {
    // No tokens needed. skip this
    if (!hasTokens) return true;
    
    Fi tokensFile = getFile();
    
    try {
      tokensFile.writeString("");
      tokens.each((t, v) -> tokensFile.writeString(t + "\n", true));
      
    } catch (Exception e) {
      logger.err("Failed to write tokens file '@'.", tokensFile.path());
      logger.err("Error: @", e.toString());
      return false;
    }
    return true;
  }
  
  public boolean isAvailable() {
    return available;
  }
  
  public boolean useTokens() {
    return hasTokens;
  }
  
  public boolean addToken(String token) {
    if (!hasTokens || tokens.containsKey(token.strip())) return false;
    tokens.put(token.strip(), 0);
    save();
    return true;
  }
  
  public boolean removeToken(String token) {
    if (!hasTokens || !tokens.containsKey(token.strip())) return false;
    tokens.remove(token.strip());
    save();
    return true;
  }
  
  @Override
  public AddressValidity checkIP(String ip) {
    // if service is unavailable for moment, don't use it 
    if (unavailableTimeout > 0 && unavailableTimeout-- > 0) {
      logger.debug("Service unavailable for moment. @ IP checks before availability check", unavailableTimeout);
      return null;
    }
    
    ServiceReply reply = new ServiceReply();
    
    if (hasTokens) {
      if (tokens.isEmpty()) {
        logger.debug("Service requires tokens, but the list is empty.");
        return null;
      }
      
      for (ObjectMap.Entry<String, Integer> token : tokens.entries()) {
        if (token.value > 0) {
          token.value--;
          logger.debug("Token unavailable for moment. @ IP checks before reuse", token.value);
          
        } else {
          reply = request(MessageFormat.format(url, ip, token.key));
          
          if (reply.type == ServiceReplyType.INVALID_TOKEN || 
              reply.type == ServiceReplyType.LIMIT_REACHED) {
            logger.err("Error while checking ip '@'. Token '@'" + 
                (reply.type == ServiceReplyType.LIMIT_REACHED ? "has reached his limit" : " is invalid."), 
              ip, token.key);
            logger.err("Http status: @ '@'", reply.status.code, reply.message);
            token.value = PVars.tokenValdityCheckTimeout;
            logger.warn("Token added in waiting list. It will be reused after @ IP checks", token.value);
            
          } else if (reply.type == ServiceReplyType.UNAVAILABLE ||
                     reply.type == ServiceReplyType.NOT_FOUND) {
            logger.err("Error while checking ip '@'. Service @.", ip, reply.type.toString().toLowerCase().replace('_', ' '));
            logger.err("Http status: @ '@'", reply.status.code, reply.message);
            unavailableTimeout = PVars.serviceValidityTimeout;
            logger.warn("Service added in waiting list. It will be reused after @ IP checks", unavailableTimeout);
            return null;
            
          } else if (reply.type == ServiceReplyType.ERROR) {
            logger.err("Error while checking ip '@'. Unknown error.", ip);
            logger.err("Http status: @ '@'", reply.status.code, reply.message);
            logger.warn("Skipping this token...");
            
          } else break;
        }
      }
      // No valid token
      logger.warn("Cannot check ip, all tokens are in waiting list!");
      
    } else {
      reply = request(MessageFormat.format(url, ip));
      
      if (reply.type != ServiceReplyType.OK) {
        if (reply.type == ServiceReplyType.UNAVAILABLE ||
            reply.type == ServiceReplyType.NOT_FOUND) {
          logger.err("Error while checking ip '@'. Service @.", ip, reply.type.toString().toLowerCase().replace('_', ' '));
          logger.err("Http status: @ '@'", reply.status.code, reply.message);
          unavailableTimeout = PVars.serviceValidityTimeout;
          logger.warn("Service added in waiting list. It will be reused after @ IP checks", unavailableTimeout);
        
        } else {
          logger.err("Error while checking ip '@'.", ip);
          logger.err("Http status: @ '@'", reply.status.code, reply.message);
        }
        
        return null;
      }
    }

    return reply.result;
  }

  private ServiceReply request(String url) {
    ServiceReply reply = new ServiceReply();
    
    Http.get(url, success -> {
      ServiceReply r = new ServiceReply();
      if (success.getStatus() == Http.HttpStatus.OK) {
        try { 
          r = handleReply(success.getResultAsString()); 
          reply.message = r.message;
          reply.result = r.result;
          reply.type = r.type;          
          
        } catch (Exception e) {
          reply.type = ServiceReplyType.ERROR;
          reply.message = e.toString();
        } 
        
      } else {
        reply.type = ServiceReplyType.ERROR;
        reply.message = success.getStatus().toString().replace('_', ' ');
      }
      
      reply.status = success.getStatus();
      
    }, failed -> {
      if (failed instanceof Http.HttpStatusException) {
        Http.HttpStatusException status = (Http.HttpStatusException) failed;
        reply.status = status.status;
        
        if (status.status.code >= 500 || (
            status.status.code >= 401 && status.status.code <= 403)) 
          reply.type = ServiceReplyType.UNAVAILABLE;
        else if (status.status.code == 404) 
          reply.type = ServiceReplyType.NOT_FOUND;
        else if (status.status.code >= 400) 
          reply.type = ServiceReplyType.ERROR;
        
        String message = status.response.getResultAsString();
        if (message.isBlank()) reply.message = status.toString();
        else reply.message = message.strip();
      }
      
      // Error while processing the request so tell the service not found
      reply.type = ServiceReplyType.NOT_FOUND;
      reply.message = failed.toString();
    });
    
    return reply;
  }

  public abstract ServiceReply handleReply(String reply) throws Exception;
  
  
  // To handle service reply outside the lambda
  public static class ServiceReply {
    public @arc.util.Nullable AddressValidity result = null;
    public ServiceReplyType type = ServiceReplyType.ERROR;
    public String message = "";
    public Http.HttpStatus status = Http.HttpStatus.UNKNOWN_STATUS;
  }
  
  public static enum ServiceReplyType {
    OK, ERROR, UNAVAILABLE, NOT_FOUND, INVALID_TOKEN, LIMIT_REACHED
  }
}
