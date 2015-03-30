package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfiguration;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfigurationBuilder;
import org.infinispan.remoting.transport.Address;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by vagvaz on 5/23/14.
 */


/**
 * Implementation of InfinispanManager interface This class uses the Distributed Execution of
 * infinispan in order to perform the operations for caches and listeners.
 */
public class ClusterInfinispanManager implements InfinispanManager {


  private Logger log = LoggerFactory.getLogger(this.getClass());
  private EmbeddedCacheManager manager;
  private String configurationFile;
  private HotRodServer server;
  private int serverPort;
  private String host;
  private Configuration defaultConfig = null;
  private Configuration defaultIndexConfig = null;

  /**
   * Constructs a new ClusterInfinispanManager.
   */
  public ClusterInfinispanManager() {
    host = "0.0.0.0";
    serverPort = 11222;
  }

  public ClusterInfinispanManager(EmbeddedCacheManager manager) {
    this.manager = manager;
    initDefaultCacheConfig();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void setConfigurationFile(String configurationFile) {
    this.configurationFile = configurationFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startManager(String configurationFile)  {


    ParserRegistry registry = new ParserRegistry();
    ConfigurationBuilderHolder holder = null;
    ConfigurationBuilder builder = null;
    try {
      if (configurationFile != null && !configurationFile.equals("")) {
        holder = registry.parseFile(configurationFile);

      } else {
        System.err.println("\n\n\nUSING DEFAULT FILE ERROR\n\n");
        holder = registry.parseFile(StringConstants.ISPN_CLUSTER_FILE);
      }
    }catch(IOException e){
      e.printStackTrace();
    }
    manager = new DefaultCacheManager(holder, true);
    //Join Infinispan Cluster
    //      manager.start();
    manager.getCache();
    getPersisentCache("clustered");
    getPersisentCache("pagerankCache");
    getPersisentCache("approx_sum_cache");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".webpages");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".entities");

    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".content");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".page");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".urldirectory");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".urldirectory_ecom");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".page_core");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".keywords");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".resourcepart");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".site");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".adidas_keywords");

    if(LQPConfiguration.getConf().getBoolean("processor.start.hotrod"))
    {
      host = LQPConfiguration.getConf().getString("node.ip");
      startHotRodServer(manager,host, serverPort);
    }

    //I might want to sleep here for a little while
    PrintUtilities.printList(manager.getMembers());



    System.out.println("We have started host:" + host);

  }

  public HotRodServer getServer() {
    return server;
  }

  public void setServer(HotRodServer server) {
    this.server = server;
  }

  public int getServerPort() {
    return serverPort;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  private void startHotRodServer(EmbeddedCacheManager targetManager, String localhost, int port) {
    log.info("Starting HotRod Server");
    System.err.println("Starting HotRod Server");
    serverPort = port;
    server = new HotRodServer();
    boolean isStarted = false;
    while (!isStarted) {
      HotRodServerConfigurationBuilder serverConfigurationBuilder =
        new HotRodServerConfigurationBuilder();
      serverConfigurationBuilder.host(localhost).port(serverPort).defaultCacheName("defaultCache");
      //                 .keyValueFilterFactory("leads-processor-filter-factory",new LeadsProcessorKeyValueFilterFactory(manager))
      //                 .converterFactory("leads-processor-converter-factory",new LeadsProcessorConverterFactory());

      try {
        server.start(serverConfigurationBuilder.build(), targetManager);
        server.addCacheEventConverterFactory("leads-processor-converter-factory", new LeadsProcessorConverterFactory());
        server.addCacheEventFilterFactory("leads-processor-filter-factory", new
                                                                              LeadsProcessorKeyValueFilterFactory(manager));
        isStarted = true;
      } catch (Exception e) {
        System.out.println("Exception e " + e.getClass().getCanonicalName() + e.getMessage());
        //            if(e == null){
        //               System.out.println(port + " " +)
        //            }
        serverPort++;
        isStarted = false;
        try {
          Thread.sleep(300);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EmbeddedCacheManager getCacheManager() {
    return this.manager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stopManager() {
    // for(String cacheName : this.manager.getCacheNames())
    //{
    //    Cache cache= this.manager.getCache(cacheName,false);
    //    if(cache != null && cache.getAdvancedCache().getStatus().equals(ComponentStatus.RUNNING))
    //         cache.stop();
    // }
    this.manager.stop();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConcurrentMap getPersisentCache(String name) {
    if (manager.cacheExists(name))
      manager.getCache(name);
      //            createCache(name,manager.getDefaultCacheConfiguration());
    else {
      createCache(name, manager.getDefaultCacheConfiguration());
    }
    return manager.getCache(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConcurrentMap getPersisentCache(String name, Configuration configuration) {
    if (manager.cacheExists(name))
      manager.getCache(name);
    else {
      createCache(name, configuration);
    }
    return manager.getCache(name);
  }

  @Override
  public ConcurrentMap getIndexedPersistentCache(String name) {
    if(manager.cacheExists(name)){
      return manager.getCache(name);
    }
    return getIndexedPersistentCache(name, getDefaultIndexedCacheConfiguration());
  }


  @Override
  public ConcurrentMap getIndexedPersistentCache(String name, Configuration configuration) {
    if(manager.cacheExists(name)){
      return manager.getCache(name);
    }
    else{
      createIndexedCache(name,configuration);
    }
    return manager.getCache(name);
  }


  private Configuration getDefaultIndexedCacheConfiguration() {
    if(defaultIndexConfig == null){
      initIndexDefaultCacheConfig();
    }
    return defaultIndexConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removePersistentCache(String name) {
    removeCache(name);
  }

  private void removeCache(String name) {
    try {
      //         if (manager.cacheExists(name)) {
      //            if(manager.getCache(name).getStatus().stopAllowed())
      //               manager.getCache(name).stop();
      //         }
      DistributedExecutorService des = new DefaultExecutorService(manager.getCache());
      List<Future<Void>> list = des.submitEverywhere(new StopCacheCallable(name));
      for (Future<Void> future : list) {
        try {
          future.get(); // wait for task to complete
        } catch (InterruptedException e) {
          log.error(e.getClass().toString() + " while removing " + name + " " + e.getMessage());
        } catch (ExecutionException e) {
          log.error(e.getClass().toString() + " while removing " + name +" " + e.getMessage());
        }
      }
      manager.removeCache(name);
    }catch (Exception e){
      log.error("Exception while remove " + name + " " + e.getClass().toString() + " " + e.getMessage());
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Object listener, Cache cache) {
    DistributedExecutorService des = new DefaultExecutorService(cache);
    List<Future<Void>> list = new LinkedList<Future<Void>>();
    //        for (Address a : getMembers()) {

    try {
      list =     des.submitEverywhere(new AddListenerCallable(cache.getName(),listener));
      //                list.add(des.submit(a, new AddListenerCallable(cache.getName(), listener)));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    //        }


    for (Future<Void> future : list) {
      try {
        future.get(); // wait for task to complete
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Object listener, String name) {
    Cache c = (Cache) this.getPersisentCache(name);
    addListener(listener, c);
  }

  //  /** {@inheritDoc} */
  //  @Override
  //  public void addListener(Object listener, String name, KeyFilter filter) {
  //    Cache c = (Cache) this.getPersisentCache(name);
  //    c.addListener(listener, filter);
  //  }
  //
  //  /** {@inheritDoc} */
  //  @Override
  //  public void addListener(Object listener, String name, KeyValueFilter filter, Converter converter) {
  //    Cache c = (Cache) this.getPersisentCache(name);
  //    c.addListener(listener, filter, converter);
  //
  //  }
  //
  //  /** {@inheritDoc} */
  //  @Override
  //  public void addListener(Object listener, Cache cache, KeyFilter filter) {
  //    cache.addListener(listener, filter);
  //  }
  //
  //  /** {@inheritDoc} */
  //  @Override
  //  public void addListener(Object listener, Cache cache, KeyValueFilter filter, Converter converter) {
  ////        cache.addListener(listener,filter,converter);
  //  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Object listener, Cache cache) {
//    DistributedExecutorService des = new DefaultExecutorService(cache);
//    List<Future<Void>> list = new LinkedList<Future<Void>>();
//    for (Address a : getMembers()) {
//      //            des.submitEverywhere(new AddListenerCallable(cache.getName(),listener));
//      try {
//        list.add(des.submit(a, new RemoveListenerCallable(cache.getName(), listener)));
//      } catch (Exception e) {
//        log.error(e.getMessage());
//      }
//    }
//
//
//    for (Future<Void> future : list) {
//      try {
//        future.get(); // wait for task to complete
//      } catch (InterruptedException e) {
//      } catch (ExecutionException e) {
//      }
//    }
    cache.removeListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Object listener, String cacheName) {
    Cache cache = (Cache) getPersisentCache(cacheName);
    removeListener(listener, cache);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Address> getMembers() {
    return manager.getCache("clustered").getAdvancedCache().getRpcManager().getMembers();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Address getMemberName() {
    return manager.getAddress();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isStarted() {
    return manager.getStatus().equals(ComponentStatus.RUNNING);
  }

  private void createCache(String cacheName, Configuration cacheConfiguration) {
    if (!cacheConfiguration.clustering().cacheMode().isClustered()) {
      log.warn("Configuration given for " + cacheName
                 + " is not clustered so using default cluster configuration");
      //            cacheConfiguration = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_ASYNC).async().l1().lifespan(100000L).hash().numOwners(3).build();
    }
    //      if(manager.cacheExists(cacheName))
    //         return;
    if(cacheName.equals("clustered")){
      Configuration cacheConfig = getCacheDefaultConfiguration(cacheName);
      manager.defineConfiguration(cacheName,cacheConfig);
      Cache cache = manager.getCache(cacheName);
    }
    else {
      manager.defineConfiguration(cacheName,getCacheDefaultConfiguration(cacheName));
      DistributedExecutorService des = new DefaultExecutorService(manager.getCache("clustered"));
      List<Future<Void>> list = des.submitEverywhere(new StartCacheCallable(cacheName));
      //
      System.out.println("list " + list.size());
      for (Future<Void> future : list) {
        try {
          future.get(); // wait for task to complete
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }
      }
    }
  }

  private void createIndexedCache(String name, Configuration configuration) {
    manager.defineConfiguration(name,getCacheDefaultConfiguration(name));
    DistributedExecutorService des = new DefaultExecutorService(manager.getCache("clustered"));
    List<Future<Void>> list = des.submitEverywhere(new StartCacheCallable(name,true));
    //
    System.out.println("list " + list.size());
    for (Future<Void> future : list) {
      try {
        future.get(); // wait for task to complete
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      }
    }
  }


  private void initDefaultCacheConfig() {
    if (LQPConfiguration.getConf().getBoolean("leads.processor.infinispan.persistence", true)) { //perssistence
      if (!LQPConfiguration.getConf().getBoolean("leads.processor.infinispan.useLevelDB", false)) {
        defaultConfig = new ConfigurationBuilder().read(manager.getDefaultCacheConfiguration())
                                .clustering()
                                .cacheMode(CacheMode.DIST_SYNC)
                                .hash().numOwners(1)
                                .indexing().index(Index.NONE).transaction().transactionMode
                                                                                    (TransactionMode
                                                                                             .NON_TRANSACTIONAL)
                                .persistence().passivation(true)
                                                       //                                                      .addStore(LevelDBStoreConfigurationBuilder.class)
                                                       //               .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()

                                .addSingleFileStore().location("/tmp/" + manager.getAddress().toString() + "/")
                                .fetchPersistentState(true)
                                .shared(false).purgeOnStartup(false).preload(false).compatibility().enable().expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
                                .build();

      } else { //Use leveldb
        defaultConfig = new ConfigurationBuilder().read(manager.getDefaultCacheConfiguration())
                                .clustering()
                                .cacheMode(CacheMode.DIST_SYNC)
                                .hash().numOwners(1)
                                .indexing().index(Index.NONE).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
                                .persistence().passivation(true)
                                .addStore(LevelDBStoreConfigurationBuilder.class)
                                .location("/tmp/leveldb/data-" + manager.getAddress().toString() + "/")
                                         //                                 .location("/tmp/leveldb/data-foo/" + "/")
                                .expiredLocation("/tmp/leveldb/expired-" + manager.getAddress().toString() + "/")
                                         //                                 .expiredLocation("/tmp/leveldb/expired-foo" + "/")
                                .implementationType(LevelDBStoreConfiguration.ImplementationType.JAVA)
                                .fetchPersistentState(true)
                                .shared(false).purgeOnStartup(false).preload(true).compatibility().enable()
                                .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
                                .build();
      }
    } else { //do not use persistence
      defaultConfig = new ConfigurationBuilder().read(manager.getDefaultCacheConfiguration())
                              .clustering()
                              .cacheMode(CacheMode.DIST_SYNC)
                              .hash().numOwners(1)
                              .indexing().index(Index.NONE).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).compatibility().enable()
                              .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
                              .build();
    }
  }

    private void initIndexDefaultCacheConfig() {
        if(defaultConfig == null){
          initDefaultCacheConfig();
        }
        defaultIndexConfig =  new ConfigurationBuilder().read(defaultConfig).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).indexing().index(Index.ALL).build();
    }



  public Configuration getCacheDefaultConfiguration(String cacheName) {
    Configuration cacheConfig = null;
    if(cacheName.equals("clustered") && cacheName.equals("default")){


      cacheConfig = new ConfigurationBuilder().read(manager.getDefaultCacheConfiguration())
                      .clustering()
                      .cacheMode(CacheMode.DIST_SYNC)
                      .hash().numOwners(2)
                      .indexing().index(Index.LOCAL).transaction().transactionMode(TransactionMode
                                                                                     .NON_TRANSACTIONAL)
                      .persistence()
                         //                            .addStore(LevelDBStoreConfigurationBuilder.class
                         //                            .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()
                      .addSingleFileStore().location("/tmp/" + manager.getAddress().toString()+"/").shared(false).preload(false).compatibility().enable()
                      .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
                      .build();
    }
    else{
      if(defaultConfig == null) {
        initDefaultCacheConfig();
      }
      cacheConfig =  defaultConfig;

    }
    return cacheConfig;
  }

  public Configuration getIndexedCacheDefaultConfiguration(String cacheName) {
    Configuration cacheConfig = null;
    if(cacheName.equals("clustered") && cacheName.equals("default")){


      cacheConfig = new ConfigurationBuilder().read(manager.getDefaultCacheConfiguration())
                            .clustering()
                            .cacheMode(CacheMode.DIST_SYNC)
                            .hash().numOwners(2)
                            .indexing().setProperty("auto-config", "true") .setProperty("default.directory_provider", "ram").index(Index.ALL).transaction().transactionMode(TransactionMode
                                                                                                 .NON_TRANSACTIONAL)
                            .persistence()
                                     //                            .addStore(LevelDBStoreConfigurationBuilder.class
                                     //                            .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()
                            .addSingleFileStore().location("/tmp/" + manager.getAddress().toString()+"/").shared(false).preload(false).compatibility().enable()
                            .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
                            .build();
    }
    else{
      if(defaultIndexConfig == null) {
        initIndexDefaultCacheConfig();
      }
      cacheConfig =  defaultIndexConfig;

    }
    return cacheConfig;
  }


}
