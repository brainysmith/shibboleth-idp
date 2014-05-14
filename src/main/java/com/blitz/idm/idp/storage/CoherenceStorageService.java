package com.blitz.idm.idp.storage;

import org.opensaml.util.storage.StorageService;

/**
 * A Oracle Coherence-based implementation of {@link StorageService}.
 *
 * @param <KeyType>   object type of the keys
 * @param <ValueType> object type of the values
 */
public class CoherenceStorageService<KeyType, ValueType> { /*implements StorageService<KeyType, ValueType> {

    private static final String COHERENCE_CACHE_CONFIG = "coherence-cache-config.xml";

    *//**
     * Class logger.
     *//*
    private static final Logger log = LoggerFactory.getLogger(CoherenceStorageService.class);

    *//**
     * Backing map.
     *//*
    private Set<String> cachePartitions;

    *//**
     * Constructor.
     *//*
    public CoherenceStorageService() {
        CacheFactory.setConfigurableCacheFactory(CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(COHERENCE_CACHE_CONFIG,
                        CoherenceStorageService.class.getClassLoader()));
        cachePartitions = new HashSet<String>();
        cachePartitions.add(CacheEntryManager.LOGIN_CTX_CACHE_NAME);
        cachePartitions.add(CacheEntryManager.LOGOUT_CTX_CACHE_NAME);
        cachePartitions.add(CacheEntryManager.SESSION_CTX_CACHE_NAME);
        cachePartitions.add(CacheEntryManager.REPLAY_CTX_CACHE_NAME);
        cachePartitions.add(CacheEntryManager.TRANSIENT_ID_CACHE_NAME);
        cachePartitions.add(CacheEntryManager.ATTRIBUTES_CACHE_NAME);
    }

    *//**
     * {@inheritDoc}
     *//*
    public boolean contains(String partition, Object key) {
        return (partition != null
                && key != null
                && getPartition(partition) != null
                && getPartition(partition).containsKey(key));
    }

    *//**
     * {@inheritDoc}
     *//*
    @SuppressWarnings("unchecked")
    public ValueType get(String partition, Object key) {
        if (partition == null || key == null || getPartition(partition) == null) {
            return null;
        }
        Object value;
        try {
            value = getPartition(partition).get(key);
        } catch (RuntimeException e) {
            log.error("Error while getting coherence named cache {} entry with key {} : {}",
                    new Object[]{partition, key, e.getMessage()} );
            throw e;
        }
        return (ValueType) value;
    }

    *//**
     * {@inheritDoc}
     *//*
    public Iterator<KeyType> getKeys(String partition) {
        if (partition == null || getPartition(partition) == null) {
            return null;
        }
        return this.new PartitionEntryIterator(partition);
    }

    *//**
     * {@inheritDoc}
     *//*
    public Iterator<String> getPartitions() {
        return this.new PartitionIterator();
    }

    *//**
     * {@inheritDoc}
     *//*
    @SuppressWarnings("unchecked")
    public ValueType put(String partition, KeyType key, ValueType value) {
        if (partition == null || key == null || getPartition(partition) == null) {
            return null;
        }
        long lifeTimeInMillis = DateTimeUtil.getLifeTimeInMillis(((AbstractExpiringObject) value).getExpirationTime());
        try {
            if (lifeTimeInMillis > 0) {
                return (ValueType) getPartition(partition).put(key, value, lifeTimeInMillis);
            }
        } catch (RuntimeException e) {
            log.error("Error while getting coherence named cache {} entry with key {} : {}",
                    new Object[]{partition, key, e.getMessage()});
            throw  e;
        }
        return null;
    }

    *//**
     * {@inheritDoc}
     *//*
    @SuppressWarnings("unchecked")
    public ValueType remove(String partition, KeyType key) {
        if (partition == null || key == null || getPartition(partition) == null) {
            return null;
        }
        Object value;
        try {
            value = getPartition(partition).remove(key);
        } catch (RuntimeException e) {
            log.error("Error while removing coherence named cache {} entry with key {} : {}",
                    new Object[]{partition, key, e.getMessage()});
            throw e;
        }
        return (ValueType) value;
    }

    private NamedCache getPartition(String partition) {
        NamedCache partitionCache = CacheFactory.getCache(partition);
        if (partitionCache == null) {
            log.error("Coherence named cache {} not found", partition);
            return null;
        }
        return partitionCache;
    }

    *//**
     * An iterator over the partitions of the storage service.
     *//*
    public class PartitionIterator implements Iterator<String> {

        *//**
         * Iterator over the partitions in the backing store.
         *//*
        private Iterator<String> partitionItr;

        *//**
         * Current partition.
         *//*
        private String currentParition;

        *//**
         * Constructor.
         *//*
        public PartitionIterator() {
            partitionItr = cachePartitions.iterator();
        }

        *//**
         * {@inheritDoc}
         *//*
        public boolean hasNext() {
            return partitionItr.hasNext();
        }

        *//**
         * {@inheritDoc}
         *//*
        public String next() {
            currentParition = partitionItr.next();
            return currentParition;
        }

        *//**
         * {@inheritDoc}
         *//*
        public void remove() {
            Iterator<KeyType> partitionEntries = getKeys(currentParition);
            while (partitionEntries.hasNext()) {
                partitionEntries.next();
                partitionEntries.remove();
            }
            CacheFactory.releaseCache(CacheFactory.getCache(currentParition));
        }
    }

    *//**
     * An iterator over the entries of a partition of the storage service.
     *//*
    public class PartitionEntryIterator implements Iterator<KeyType> {

        *//**
         * Partition on which we are operating.
         *//*
        private String partition;

        *//**
         * Iterator of keys within the partition.
         *//*
        private Iterator<KeyType> keysItr;

        *//**
         * Current key within the iteration.
         *//*
        private KeyType currentKey;

        *//**
         * Constructor.
         *
         * @param partition partition upon which this iterator operates
         *//*
        @SuppressWarnings("unchecked")
        public PartitionEntryIterator(String partition) {
            this.partition = partition;
            keysItr = CacheFactory.getCache(partition).keySet().iterator();
        }

        *//**
         * {@inheritDoc}
         *//*
        public boolean hasNext() {
            return keysItr.hasNext();
        }

        *//**
         * {@inheritDoc}
         *//*
        public KeyType next() {
            currentKey = keysItr.next();
            return currentKey;
        }

        *//**
         * {@inheritDoc}
         *//*
        public void remove() {
            CoherenceStorageService.this.remove(partition, currentKey);
        }
    }*/
}