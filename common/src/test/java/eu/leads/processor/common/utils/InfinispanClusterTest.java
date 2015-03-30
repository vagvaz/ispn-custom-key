package eu.leads.processor.common.utils;

import eu.leads.processor.common.StringConstants;

import eu.leads.processor.common.infinispan.CacheManagerFactory;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;
import junit.framework.TestCase;

import java.util.Map;

public class InfinispanClusterTest extends TestCase {
    public static void main(String[] args) {
        LQPConfiguration.initialize();
       InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
       Map<String,String> map = manager.getPersisentCache(StringConstants.QUERIESCACHE);
       Map<String,String> map2 = manager.getPersisentCache("default.webpages");
               map2.put("1","11");
               map2.put("1","11");
               map.put("1","11");
               map.put("22","222");
       InfinispanManager manager2 = CacheManagerFactory.createCacheManager();
       map2 = manager2.getPersisentCache("default.webpages");
       map = manager2.getPersisentCache(StringConstants.QUERIESCACHE);

               map2.put("2333","223");
               map.put("d2","22");
               map.put("2f","22");
        PrintUtilities.printMap(map);
        PrintUtilities.printMap(map2);

       //        InfinispanCluster cluster = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        ////        cluster.initialize();
        //        InfinispanManager man = cluster.getManager();
        //
        //        ConcurrentMap map = man.getPersisentCache("testCache");
        //        map.put("1","11");
        //        map.put("2","22");
        //        InfinispanCluster cluster2 = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        //        PrintUtilities.printMap(map);
        //        ConcurrentMap map2 = cluster2.getManager().getPersisentCache("testCache");
        //        map2.put("3","33");
        //        PrintUtilities.printMap(map2);
        //        System.out.println("cl");
        //        PrintUtilities.printList(cluster.getManager().getMembers());
        //        System.out.println("cl2");
        //        PrintUtilities.printList(cluster2.getManager().getMembers());
    }

    public void testGetManager() throws Exception {
        //        LQPConfiguration.initialize();
        //        InfinispanCluster cluster = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        ////        cluster.initialize();
        //        InfinispanManager man = cluster.getManager();
        //
        //        ConcurrentMap map = man.getPersisentCache("testCache");
        //        map.put("1","11");
        //        map.put("2","22");
        //        InfinispanCluster cluster2 = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        //        PrintUtilities.printMap(map);
        //        ConcurrentMap map2 = cluster2.getManager().getPersisentCache("testCache");
        //        map2.put("3","33");
        //        PrintUtilities.printMap(map2);
        //        System.out.println("cl");
        //        PrintUtilities.printList(cluster.getManager().getMembers());
        //        System.out.println("cl2");
        //        PrintUtilities.printList(cluster2.getManager().getMembers());
    }
}
