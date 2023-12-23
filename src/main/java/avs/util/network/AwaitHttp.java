/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2023 Xpdustry
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

package avs.util.network;

import arc.func.Cons;
import arc.func.ConsT;


public class AwaitHttp extends arc.util.Http {
  // To disable http requests if nedded
  public static boolean disabled = false;
  
  public static HttpRequest request(AwaitHttp.HttpMethod method, String url){
    if(url == null || url.isEmpty()) throw new NullPointerException("url cannot be null or empty.");
    return new HttpRequest(method) {
      @Override
      public void submit(arc.func.ConsT<HttpResponse, Exception> success){
        if (disabled) return;
        try { AwaitHttp.exec.submit(() -> block(success)).get(); } 
        catch (Exception e) { arc.util.Log.err(e); }
      }
    }.url(url);
  }
  
  public static HttpRequest get(String url){
    return request(HttpMethod.GET, url);
  }

  public static void get(String url, ConsT<HttpResponse, Exception> callback){
    get(url).submit(callback);
  }
  
  public static void get(String url, ConsT<HttpResponse, Exception> callback, Cons<Throwable> error){
    get(url).error(error).submit(callback);
  }
    
  public static HttpRequest post(String url, String content){
    return request(AwaitHttp.HttpMethod.POST, url).content(content);
  }
  
  public static HttpRequest post(String url){
    return post(url, (String)null);
  }

  public static void post(String url, ConsT<HttpResponse, Exception> callback){
    post(url).submit(callback);
  }
}
