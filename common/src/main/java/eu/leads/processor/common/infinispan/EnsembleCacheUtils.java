package eu.leads.processor.common.infinispan;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.context.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vagvaz on 3/7/15.
 */
public class EnsembleCacheUtils {

   static Logger log  = LoggerFactory.getLogger(EnsembleCacheUtils.class);
   public static void putToCache(BasicCache cache1, Object key, Object value){
      boolean isok = false;
      RemoteCache cache = (RemoteCache)cache1;
      while(!isok) {
         try {
           if(cache != null) {
              if(key == null || value == null){
                 log.error("SERIOUS PROBLEM with key/value null key: " + (key==null) + " value " + (value==null) );
                 if(key != null)
                 {
                    log.error("key " + key.toString());
                 }
                 if(value != null){
                    log.error("value: " + value);
                 }
                 isok = true;
                 continue;
              }
             cache.put(key,value);

              log.error("Successful " + key);
              isok = true;
           }
           else {
             log.error("CACHE IS NULL IN PUT TO CACHE for " + key.toString() + " " + value.toString());
             isok = true;
           }
         }catch (Exception e) {
            isok = false;
            log.error("PUT TO CACHE " + e.getMessage() + " " + key);
            try {
               Thread.sleep(5);
            } catch (InterruptedException e1) {
               e1.printStackTrace();
            }
            System.err.println("PUT TO CACHE " + e.getMessage());
         }
      }
   }

  public static <KOut> void putIfAbsentToCache(BasicCache cache, KOut key, KOut value) {
      putToCache(cache,key,value);
//    boolean isok = false;
//    while(!isok) {
//      try {
//        cache.put(key, value);
//        isok =true;
//      } catch (Exception e) {
//        isok = false;
//        System.err.println("PUT TO CACHE " + e.getMessage());
//      }
//    }
  }
}
