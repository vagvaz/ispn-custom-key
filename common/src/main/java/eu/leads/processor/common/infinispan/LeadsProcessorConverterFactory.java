package eu.leads.processor.common.infinispan;


import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverterFactory;

/**
 * Created by vagvaz on 9/29/14.
 */
public class LeadsProcessorConverterFactory implements CacheEventConverterFactory{
//public class LeadsProcessorConverterFactory implements ConverterFactory {



    @Override
     public <K, V, C> CacheEventConverter<K, V, C> getConverter(Object[] objects) {
        return new LeadsProcessorConverter();
    }
}
