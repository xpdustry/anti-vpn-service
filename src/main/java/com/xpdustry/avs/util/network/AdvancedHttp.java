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

package com.xpdustry.avs.util.network;

import java.net.HttpURLConnection;

import arc.func.Boolf;
import arc.struct.StringMap;
import arc.util.Http.HttpResponse;
import arc.util.Http.HttpStatus;


public class AdvancedHttp {
  /** Control whether the content should be accepted, if the server indicates a 203 status */
  public static boolean allowUntrustedSourceHttpCode = true;

  public static Reply get(String url) { return get(url, null); }
  public static Reply get(String url, StringMap headers) {
    Reply reply = new Reply();
    int retries = 3;
    
    do {
      AwaitHttp.get(url, headers, 
        success -> handleSuccess(reply, success), 
        failure -> handleFailure(reply, failure)); 
    // Retry the request, 3 times max, if a timeout occurs.
    } while ((reply.httpStatus == HttpStatus.REQUEST_TIMEOUT ||
              (reply.error != null && reply.error instanceof java.net.SocketTimeoutException))
            && --retries > 0);

    return reply;
  }
  
  public static Reply post(String url, String content) { return post(url, content, null); }
  public static Reply post(String url, String content, StringMap headers) {
    Reply reply = new Reply();
    
    AwaitHttp.post(url, headers, content, 
      success -> handleSuccess(reply, success), 
      failure -> handleFailure(reply, failure));
    
    return reply;
  }
  
  private static void handleSuccess(Reply toProvide, HttpResponse response) {
    toProvide.httpStatus = response.getStatus();
    toProvide.result = response.getResultAsString().strip();
    if (toProvide.result.isEmpty()) toProvide.status = Status.EMPTY_CONTENT;
    else toProvide.status = Status.getByHttpCode(toProvide.httpStatus.code);
    toProvide.setMessage(response);
  }
  
  private static void handleFailure(Reply toProvide, Throwable error) {
    if (error instanceof AwaitHttp.HttpStatusException) {
      AwaitHttp.HttpStatusException status = (AwaitHttp.HttpStatusException) error;
      toProvide.httpStatus = status.status;
      toProvide.status = Status.getByHttpCode(toProvide.httpStatus.code);
      
      String message = status.response.getResultAsString().strip();
      if (!message.isBlank() && message.length() < 512)
           toProvide.setMessage(message);
      else toProvide.setMessage(status.response);
      
    } else {
      toProvide.error = error;
      toProvide.status = Status.ERROR;
      toProvide.setMessage(error.toString());
    }
  }
  
  
  public static class Reply {
    public String result, message;
    public HttpStatus httpStatus = HttpStatus.UNKNOWN_STATUS;
    public Status status = Status.NONE;
    public Throwable error;
   
    public void setMessage(String status) {
      message = status;
    }
    
    public void setMessage() {
      setMessage(status.name);
    }
    
    public void setMessage(HttpURLConnection reply) {
      String message = null;
      try { message = reply.getResponseMessage(); } 
      catch (java.io.IOException ignored) {}
      
      if (message == null || message.isEmpty()) setMessage();
      else setMessage(message);
    }
    
    public void setMessage(HttpResponse reply) {
      // Why make this private Anuke???
      try {
        HttpURLConnection con = arc.util.Reflect.get(reply, "connection");
        setMessage(con);
      } catch (RuntimeException e) { setMessage(); }
    }
    
    public boolean isError() {
      return error != null || !status.isSuccess();
    }
  }
  
  
  /** Wraps HTTP codes to simplify the error handling. */
  public static enum Status {
    /** no status, counts as success */
    NONE(c -> false),
    /** unknown (-1), invalid (>600) status or for a general error */
    ERROR(c -> c <= -1 || c >= 600), 

    /** 
     * 100 - 200: OK <br>
     * 203: OK, only if {@link JsonHttp#allowUntrustedSourceHttpCode} is {@code true}.
     * Else INVALID_CONTENT.
     */
    OK(c -> (c >= 100 && c <= 200) || (allowUntrustedSourceHttpCode && c == 203)), 
    /** 204 - 206, 208, 304: EMPTY_CONTENT */
    EMPTY_CONTENT(c -> (c >= 204 && c <= 206) || c == 208 || c == 304), 
    /** 
     * 207, 209 - 299: INVALID_CONTENT <br>
     * 203: if {@link JsonHttp#allowUntrustedSourceHttpCode} is {@code false}
     */
    INVALID_CONTENT(c -> c == 207 || (c >= 209 && c <= 299) || (!allowUntrustedSourceHttpCode && c == 203)), 
    
    /** 
     * 300 - 399, not 304: REDIRECT_FAILED <br>
     * @apiNote http 300 codes are handled automatically, so if one of them is gotten, the redirect failed.
     */
    REDIRECT_FAILED(c -> c >= 300 && c <= 399 && c != 304), 
    
    /** 400: BAD_REQUEST */
    BAD_REQUEST(c -> c == 400),
    /** 401, 403, 511: ACCESS_FORBIDDEN */
    ACCESS_FORBIDDEN(c -> c == 401 || c == 403 || c == 511), 
    /** 404: NOT_FOUND */
    NOT_FOUND(c -> c == 404),
    /**
     * 402, 406 - 497, 499: ACCESS_ERROR <br>
     * 408: retry the request, 3 times max
     */
    ACCESS_ERROR(c -> c == 402 || (c >= 406 && c <= 499 && c != 429 && c != 498)),
    /** 498: INVALID_TOKEN */
    INVALID_TOKEN(c -> c == 498),
    /** 429, 509: QUOTA_LIMIT */
    QUOTA_LIMIT(c -> c == 429 || c == 509),
    
    
    /** 500, 502 - 504: SERVER_UNAVAILABLE */
    SERVER_UNAVAILABLE(c -> c == 500 || (c >= 502 && c <= 504)),
    /** 501, 505 - 510: SERVER_ERROR */
    SERVER_ERROR(c -> c == 501 || (c >= 505 && c <= 510)),
    /** 520 - 599: CLOUDFLARE_ERROR */
    CLOUDFLARE_ERROR(c -> c >= 520 && c <= 599);
    
    
    public static final Status[] all = values();
    
    private final Boolf<Integer> check;
    public final String name;
    
    Status(Boolf<Integer> check) {
      this.check = check;
      this.name = arc.util.Strings.capitalize(toString().toLowerCase());
    }
    
    public boolean checkCode(int httpCode) {
      return check.get(httpCode);
    }
    
    public boolean isSuccess() {
      return this == NONE || this == OK;
    }
    
    /** 
     * Fatal errors are all http codes {@code >300} except {@code 304}. <br>
     * Why {@code 300} codes? Because these codes are managed automatically, 
     * so if they are obtained, it's fatal.
     */
    public boolean isFatalError() {
      // to simplify, we will use the ordinal level
      return this.ordinal() >= REDIRECT_FAILED.ordinal();
    }
    
    public static Status getByHttpCode(int httpCode) {
      for (Status s : all) {
        if (s.checkCode(httpCode)) return s;
      }
      return NONE;
    }
  }
}
