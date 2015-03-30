package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.plugins.PluginPackage;
import eu.leads.processor.common.utils.FSUtilities;
import eu.leads.processor.common.utils.storage.LeadsStorage;
import eu.leads.processor.common.utils.storage.LeadsStorageFactory;
import eu.leads.processor.conf.ConfigurationUtilities;
import eu.leads.processor.plugins.EventType;
import eu.leads.processor.plugins.PluginInterface;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.*;

import static eu.leads.processor.plugins.EventType.*;

/**
 * Created by vagvaz on 9/29/14.
 */
public class PluginRunnerFilter implements CacheEventFilter,Serializable {



  private JsonObject conf;
  private String configString;
  transient private final EmbeddedCacheManager manager;
  transient private ClusterInfinispanManager imanager;
  transient private Cache pluginsCache;
  transient Cache targetCache;
  transient String targetCacheName;
  transient Logger log = LoggerFactory.getLogger(PluginRunnerFilter.class) ;
  transient PluginInterface plugin;
  transient String pluginsCacheName;
  transient String pluginName;
  transient List<EventType> type;
  transient  boolean isInitialized = false;
  transient LeadsStorage storageLayer = null;
  transient String user;
  public PluginRunnerFilter(EmbeddedCacheManager manager,String confString){
    this.manager = manager;
    this.configString = confString;
    this.conf = new JsonObject(configString);
    imanager = new ClusterInfinispanManager(manager);


    initialize();
  }

  private void initialize() {
    isInitialized = true;
    pluginsCacheName = conf.getString("activePluginCache");//StringConstants.PLUGIN_ACTIVE_CACHE);

    pluginName = conf.getString("pluginName");
    user = conf.getString("user");
    JsonArray types = conf.getArray("types");
    //InferTypes
    type = new ArrayList<EventType>(3);
    if(types != null ) {
      Iterator<Object> iterator = types.iterator();
      if (iterator.hasNext()) {
        type.add( EventType.valueOf((String)iterator.next()));
      }
    }

    if(type.size() == 0){
      type.add(CREATED);
      type.add(REMOVED);
      type.add(MODIFIED);
    }

    pluginsCache = (Cache) imanager.getPersisentCache(pluginsCacheName);
    log = LoggerFactory.getLogger( "PluginRunner."+pluginName+":"+ pluginsCacheName);
    String storagetype = this.conf.getString("storageType");
    Properties storageConfiguration = new Properties();
    byte[] storageConfBytes =  this.conf.getBinary("storageConfiguration");
    ByteArrayInputStream bais = new ByteArrayInputStream(storageConfBytes);
    try {
      storageConfiguration.load(bais);
      storageLayer = LeadsStorageFactory.getInitializedStorage(storagetype,storageConfiguration);
    } catch (IOException e) {
      e.printStackTrace();
    }
    targetCacheName = conf.getString("targetCache");
    targetCache = (Cache) imanager.getPersisentCache(targetCacheName);
    initializePlugin(pluginsCache,pluginName,user);
    System.err.println("Initialized plugin " + pluginName + " on " + targetCacheName);
  }


  private void initializePlugin(Cache cache, String plugName, String user) {
//    String jarFileName = null;
//    if (plugName.equals("eu.leads.processor.plugins.pagerank.PagerankPlugin")) {
//      //            ConfigurationUtilities
//      //                    .addToClassPath(System.getProperty("java.io.tmpdir") + "/leads/plugins/" + "pagerank-plugin-1.0-SNAPSHOT-jar-with-dependencies.jar");
//      jarFileName = System.getProperty("java.io.tmpdir") + "/leads/plugins/" + "pagerank-plugin-1.0-SNAPSHOT-jar-with-dependencies.jar";
//    } else if (plugName.equals("eu.leads.processor.plugins.sentiment.SentimentAnalysisPlugin")) {
//      //            ConfigurationUtilities
//      //                    .addToClassPath(System.getProperty("java.io.tmpdir") + "/leads/plugins/" + "sentiment-plugin-1.0-SNAPSHOT-jar-with-dependencies.jar");
//      jarFileName = System.getProperty("java.io.tmpdir") + "/leads/plugins/" + "sentiment-plugin-1.0-SNAPSHOT-jar-with-dependencies.jar";
//    } else {
//      byte[] jarAsBytes = (byte[]) cache.get(plugName + ":jar");
//      FSUtilities.flushPluginToDisk(plugName + ".jar", jarAsBytes);
//
//      //            ConfigurationUtilities
//      //                    .addToClassPath(System.getProperty("java.io.tmpdir") + "/leads/plugins/" + plugin
//      //                            + ".jar");
//      jarFileName = System.getProperty("java.io.tmpdir") + "/leads/plugins/" + plugName
//                      + ".jar";
//    }
    PluginPackage pluginPackage = (PluginPackage) cache.get(targetCacheName+":"+plugName+user);
    String tmpdir = System.getProperties().getProperty("java.io.tmpdir")+"/"+StringConstants
                                                                          .TMPPREFIX+"/runningPlugins/"+ UUID
                                                                                                     .randomUUID()
                                                                                    .toString()+"/";
    String  jarFileName = tmpdir+pluginPackage.getClassName()+".jar";
    storageLayer.download("plugins/"+plugName,jarFileName);
    ClassLoader classLoader = null;
    try {
      classLoader = ConfigurationUtilities.getClassLoaderFor(jarFileName);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }


    ConfigurationUtilities.addToClassPath(jarFileName);
//      .addToClassPath(System.getProperty("java.io.tmpdir") + "/leads/plugins/" + plugName
//                        + ".jar");

//    byte[] config = (byte[]) cache.get(plugName + ":conf");
    byte[] config = pluginPackage.getConfig();
    FSUtilities.flushToTmpDisk(tmpdir + plugName + "-conf.xml", config);
    XMLConfiguration pluginConfig = null;
    try {
      pluginConfig =
        new XMLConfiguration(tmpdir + plugName + "-conf.xml");
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
//    String className = (String) cache.get(plugName + ":className");
    String className = pluginPackage.getClassName();
    if (className != null && !className.equals("")) {
      try {
        Class<?> plugClass =
          Class.forName(className, true, classLoader);
        Constructor<?> con = plugClass.getConstructor();
        plugin = (PluginInterface) con.newInstance();
        plugin.initialize(pluginConfig, imanager);

      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } else {
      log.error("Could not find the name for " + plugName);
    }
  }


  @Override
  public boolean accept(Object key, Object oldValue, Metadata oldMetadata, Object newValue,
                         Metadata newMetadata,
                         org.infinispan.notifications.cachelistener.filter.EventType eventType) {
    if(!isInitialized)
      initialize();
    String o1 = (String)key;
    String value = (String)newValue;
    switch (eventType.getType()) {
      case CACHE_ENTRY_CREATED:
        if(type.contains(CREATED))
          plugin.created(key, value, targetCache);
        break;
      case CACHE_ENTRY_REMOVED:
        if(type.contains(REMOVED))
          plugin.removed(key, value, targetCache);
        break;
      case CACHE_ENTRY_MODIFIED:
        if(type.contains(MODIFIED))
          plugin.modified(key, value, targetCache);
        break;
      default:
        break;
    }
    return false;
  }
}
