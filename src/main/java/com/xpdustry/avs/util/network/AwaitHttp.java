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

import arc.func.Cons;
import arc.func.ConsT;
import arc.struct.StringMap;
import arc.util.Http;


public class AwaitHttp extends Http {
  public static int readWriteTimeout = 2000; // in ms
  
  // region with headers
  
  public static Http.HttpRequest setHeaders(Http.HttpRequest request, StringMap headers) {
    if (headers != null) request.headers = headers;
    return request;
  }
  
  public static void get(String url, StringMap headers, ConsT<HttpResponse, Exception> callback){
    setHeaders(get(url), headers).timeout(readWriteTimeout).block(callback);
  }
  
  public static void get(String url, StringMap headers, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    setHeaders(get(url), headers).timeout(readWriteTimeout).error(error).block(callback);
  }

  public static void post(String url, StringMap headers, ConsT<HttpResponse, Exception> callback){
    setHeaders(post(url), headers).timeout(readWriteTimeout).block(callback);
  }
  
  public static void post(String url, StringMap headers, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    setHeaders(post(url), headers).timeout(readWriteTimeout).error(error).block(callback);
  }    
  
  public static void post(String url, StringMap headers, String content, ConsT<HttpResponse, Exception> callback){
    setHeaders(post(url, content), headers).timeout(readWriteTimeout).block(callback);
  }
  
  public static void post(String url, StringMap headers, String content, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    setHeaders(post(url, content), headers).timeout(readWriteTimeout).error(error).block(callback);
  }
  
  // end region
  // region simple

  public static void get(String url, ConsT<HttpResponse, Exception> callback){
    get(url).timeout(readWriteTimeout).block(callback);
  }
  
  public static void get(String url, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    get(url).timeout(readWriteTimeout).error(error).block(callback);
  }

  public static void post(String url, ConsT<HttpResponse, Exception> callback){
    post(url).timeout(readWriteTimeout).block(callback);
  }
  
  public static void post(String url, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    post(url).timeout(readWriteTimeout).error(error).block(callback);
  }    
  
  public static void post(String url, String content, ConsT<HttpResponse, Exception> callback){
    post(url, content).timeout(readWriteTimeout).block(callback);
  }
  
  public static void post(String url, String content, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    post(url, content).timeout(readWriteTimeout).error(error).block(callback);
  }
  
  // end region
}
