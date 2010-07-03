/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.cache;

import gov.nasa.worldwind.util.Logging;

/**
 * @author Eric Dalgliesh
 * @version $Id: BasicMemoryCache.java 7884 2008-11-22 19:44:06Z tgaskins $
 */
public final class BasicMemoryCache implements MemoryCache
{
    private static class CacheEntry implements Comparable<CacheEntry>
    {
        Object key;
        Object clientObject;
        private long lastUsed;
        private long clientObjectSize;

        CacheEntry(Object key, Object clientObject, long clientObjectSize)
        {
            this.key = key;
            this.clientObject = clientObject;
            this.lastUsed = System.nanoTime();
            this.clientObjectSize = clientObjectSize;
        }

        public int compareTo(CacheEntry that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.CacheEntryIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            return this.lastUsed < that.lastUsed ? -1 : this.lastUsed == that.lastUsed ? 0 : 1;
        }

        public String toString()
        {
            return key.toString() + " " + clientObject.toString() + " " + lastUsed + " " + clientObjectSize;
        }
    }

    private java.util.concurrent.ConcurrentHashMap<Object, CacheEntry> entries;
    private java.util.concurrent.CopyOnWriteArrayList<MemoryCache.CacheListener> listeners;
    private Long capacityInBytes;
    private Long currentUsedCapacity;
    private Long lowWater;
    private String name = "";

    /**
     * Constructs a new cache using <code>capacity</code> for maximum size, and <code>loWater</code> for the low water.
     *
     * @param loWater  the low water level
     * @param capacity the maximum capacity
     */
    public BasicMemoryCache(long loWater, long capacity)
    {
        this.entries = new java.util.concurrent.ConcurrentHashMap<Object, CacheEntry>();
        this.listeners = new java.util.concurrent.CopyOnWriteArrayList<MemoryCache.CacheListener>();
        this.capacityInBytes = capacity;
        this.lowWater = loWater;
        this.currentUsedCapacity = (long) 0;
    }

    /**
     * @return the number of objects currently stored in this cache
     */
    public int getNumObjects()
    {
        return this.entries.size();
    }

    /**
     * @return the capacity of the cache in bytes
     */
    public long getCapacity()
    {
        return this.capacityInBytes;
    }

    /**
     * @return the number of bytes that the cache currently holds
     */
    public synchronized long getUsedCapacity()
    {
        return this.currentUsedCapacity;
    }

    /**
     * @return the amount of free space left in the cache (in bytes)
     */
    public synchronized long getFreeCapacity()
    {
        return this.capacityInBytes - this.currentUsedCapacity;
    }

    public void setName(String name)
    {
        this.name = name != null ? name : "";
    }

    public String getName()
    {
        return name;
    }

    /**
     * Adds a  cache listener, MemoryCache listeners are used to notify classes when an item is removed from the cache.
     *
     * @param listener The new <code>CacheListener</code>
     * @throws IllegalArgumentException is <code>listener</code> is null
     */
    public synchronized void addCacheListener(MemoryCache.CacheListener listener)
    {
        if (listener == null)
        {
            String message = Logging.getMessage("BasicMemoryCache.nullListenerAdded");
            Logging.logger().warning(message);
            throw new IllegalArgumentException(message);
        }
        this.listeners.add(listener);
    }

    /**
     * Removes a cache listener, objects using this listener will no longer receive notification of cache events.
     *
     * @param listener The <code>CacheListener</code> to remove
     * @throws IllegalArgumentException if <code>listener</code> is null
     */
    public synchronized void removeCacheListener(MemoryCache.CacheListener listener)
    {
        if (listener == null)
        {
            String message = Logging.getMessage("BasicMemoryCache.nullListenerRemoved");
            Logging.logger().warning(message);
            throw new IllegalArgumentException(message);
        }
        this.listeners.remove(listener);
    }

    /**
     * Sets the new capacity (in bytes) for the cache. When decreasing cache size, it is recommended to check that the
     * lowWater variable is suitable. If the capacity infringes on items stored in the cache, these items are removed.
     * Setting a new low water is up to the user, that is, it remains unchanged and may be higher than the maximum
     * capacity. When the low water level is higher than or equal to the maximum capacity, it is ignored, which can lead
     * to poor performance when adding entries.
     *
     * @param newCapacity the new capacity of the cache.
     */
    public synchronized void setCapacity(long newCapacity)
    {
//        this.makeSpace(this.capacityInBytes - newCapacity);
        this.capacityInBytes = newCapacity;
    }

    /**
     * Sets the new low water level in bytes, which controls how aggresively the cache discards items.
     * <p/>
     * When the cache fills, it removes items until it reaches the low water level.
     * <p/>
     * Setting a high loWater level will increase cache misses, but decrease average add time, but setting a low loWater
     * will do the opposite.
     *
     * @param loWater the new low water level in bytes.
     */
    public synchronized void setLowWater(long loWater)
    {
        if (loWater < this.capacityInBytes && loWater >= 0)
        {
            this.lowWater = loWater;
        }
    }

    /**
     * Returns the low water level in bytes. When the cache fills, it removes items until it reaches the low water
     * level.
     *
     * @return the low water level in bytes.
     */
    public long getLowWater()
    {
        return this.lowWater;
    }

    /**
     * Returns true if the cache contains the item referenced by key. No guarantee is made as to whether or not the item
     * will remain in the cache for any period of time.
     * <p/>
     * This function does not cause the object referenced by the key to be marked as accessed. <code>getObject()</code>
     * should be used for that purpose
     *
     * @param key The key of a specific object
     * @return true if the cache holds the item referenced by key
     * @throws IllegalArgumentException if <code>key</code> is null
     */
    public boolean contains(Object key)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        return this.entries.containsKey(key);
    }

    /**
     * Adds an object to the cache. The add fails if the object or key is null, or if the size is zero, negative or
     * greater than the maximmum capacity
     *
     * @param key              The unique reference key that identifies this object.
     * @param clientObject     The actual object to be cached.
     * @param clientObjectSize The size of the object in bytes.
     * @return returns true if clientObject was added, false otherwise.
     */
    public synchronized boolean add(Object key, Object clientObject, long clientObjectSize)
    {
        if (key == null || clientObject == null || clientObjectSize <= 0 || clientObjectSize > this.capacityInBytes)
        {
            Logging.logger().warning("BasicMemoryCache.CacheItemNotAdded");

            if (clientObjectSize > this.capacityInBytes)
                Logging.logger().warning("BasicMemoryCache.ItemTooLargeForCache");

            return false;
            // the logic behind not throwing an exception is that whether we throw an exception or not,
            // the object won't be added. This doesn't matter because that object could be removed before
            // it is accessed again anyway.
        }

        CacheEntry existing = this.entries.get(key);
        if (existing != null) // replacing
        {
            this.removeEntry(existing);
        }

        if (this.currentUsedCapacity + clientObjectSize > this.capacityInBytes)
        {
            this.makeSpace(clientObjectSize);
        }

        this.currentUsedCapacity += clientObjectSize;
        BasicMemoryCache.CacheEntry entry = new BasicMemoryCache.CacheEntry(key, clientObject, clientObjectSize);
        this.entries.putIfAbsent(entry.key, entry);
        return true;
    }

    public synchronized boolean add(Object key, Cacheable clientObject)
    {
        return this.add(key, clientObject, clientObject.getSizeInBytes());
    }

    /**
     * Remove the object reference by key from the cache. If no object with the corresponding key is found, this method
     * returns immediately.
     *
     * @param key the key of the object to be removed
     * @throws IllegalArgumentException if <code>key</code> is null
     */
    public synchronized void remove(Object key)
    {
        if (key == null)
        {
            Logging.logger().finer("nullValue.KeyIsNull");

            return;
        }

        CacheEntry entry = this.entries.get(key);
        if (entry != null)
            this.removeEntry(entry);
    }

    /**
     * Obtain the object referenced by key without removing it. Apart from adding an object, this is the only way to
     * mark an object as recently used.
     *
     * @param key The key for the object to be found.
     * @return the object referenced by key if it is present, null otherwise.
     * @throws IllegalArgumentException if <code>key</code> is null
     */
    public synchronized Object getObject(Object key)
    {
        if (key == null)
        {
            Logging.logger().finer("nullValue.KeyIsNull");

            return null;
        }

        CacheEntry entry = this.entries.get(key);

        if (entry == null)
            return null;

        entry.lastUsed = System.nanoTime(); // nanoTime overflows once every 292 years
        // which will result in a slowing of the cache
        // until ww is restarted or the cache is cleared.
        return entry.clientObject;
    }

    /**
     * Obtain a list of all the keys in the cache.
     *
     * @return a <code>Set</code> of all keys in the cache.
     */
    public java.util.Set<Object> getKeySet()
    {
        return this.entries.keySet();
    }

    /**
     * Empties the cache.
     */
    public synchronized void clear()
    {
        for (CacheEntry entry : this.entries.values())
        {
            this.removeEntry(entry);
        }
    }

    /**
     * Removes <code>entry</code> from the cache. To remove an entry using its key, use <code>remove()</code>
     *
     * @param entry The entry (as opposed to key) of the item to be removed
     */
    private synchronized void removeEntry(CacheEntry entry)
    {
        // all removal passes through this function,
        // so the reduction in "currentUsedCapacity" and listener notification is done here

        if (this.entries.remove(entry.key) != null) // returns null if entry does not exist
        {
            this.currentUsedCapacity -= entry.clientObjectSize;

            for (MemoryCache.CacheListener listener : this.listeners)
            {
                listener.entryRemoved(entry.key, entry.clientObject);
            }
        }
    }

    /**
     * Makes at least <code>spaceRequired</code> space in the cache. If spaceRequired is less than (capacity-lowWater),
     * makes more space. Does nothing if capacity is less than spaceRequired.
     *
     * @param spaceRequired the amount of space required.
     */
    private void makeSpace(long spaceRequired)
    {
        if (spaceRequired > this.capacityInBytes || spaceRequired < 0)
            return;

        CacheEntry[] timeOrderedEntries = new CacheEntry[this.entries.size()];
        java.util.Arrays.sort(this.entries.values().toArray(timeOrderedEntries));

        int i = 0;
        while (this.getFreeCapacity() < spaceRequired || this.getUsedCapacity() > this.lowWater)
        {
            if (i < timeOrderedEntries.length)
            {
                this.removeEntry(timeOrderedEntries[i++]);
            }
        }
    }

    /**
     * a <code>String</code> representation of this object is returned.&nbsp; This representation consists of maximum
     * size, current used capacity and number of currently cached items.
     *
     * @return a <code>String</code> representation of this object
     */
    @Override
    public synchronized String toString()
    {
        return "MemoryCache " + this.name + " max size = " + this.getCapacity() + " current size = " + this
            .currentUsedCapacity + " number of items: " + this.getNumObjects();
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            // clear doesn't throw any checked exceptions
            // but this is in case of an unchecked exception
            // basically, we don't want to exit without calling super.finalize
            this.clear();
        }
        finally
        {
            super.finalize();
        }
    }
}
