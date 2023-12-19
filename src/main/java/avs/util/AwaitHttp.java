package avs.util;

import arc.func.Cons;
import arc.func.ConsT;


public class AwaitHttp extends arc.util.Http {
  public static HttpRequest request(AwaitHttp.HttpMethod method, String url){
    if(url == null || url.isEmpty()) throw new NullPointerException("url cannot be null or empty.");
    return new HttpRequest(method) {
      @Override
      public void submit(arc.func.ConsT<HttpResponse, Exception> success){
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
