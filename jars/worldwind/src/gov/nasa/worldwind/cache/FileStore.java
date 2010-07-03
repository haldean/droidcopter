/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.cache;

/**
 * @author Tom Gaskins
 * @version $Id: FileStore.java 13139 2010-02-16 21:13:30Z dcollins $
 */
public interface FileStore
{
    public static final String OS_SPECIFIC_DATA_PATH = "FileStore.OSSpecificDataPathKey";

    java.util.List<? extends java.io.File> getLocations();

    java.io.File getWriteLocation();

    void addLocation(String newPath, boolean isInstall);

    void addLocation(int index, String newPath, boolean isInstall);

    void removeLocation(String path);

    boolean isInstallLocation(String path);

    boolean containsFile(String fileName);

    java.net.URL findFile(String fileName, boolean checkClassPath);

    java.io.File newFile(String fileName);

    void removeFile(java.net.URL url);

    /**
     * Returns an array of strings naming the files discovered directly under a specified file store path name. If the
     * path name is null, files under the store root are searched. This returns null if the path does not exist in the
     * store. Returned names are relative pointers to a file in the store; they are not necessarily a file system path.
     *
     * @param pathName relative path in the file store to search, or null to search the entire file store.
     * @param filter   a file filter.
     *
     * @return an array of file store names. Returns null if the path does not exist in the file store.
     *
     * @throws IllegalArgumentException if the filter is null.
     */
    String[] listFileNames(String pathName, FileStoreFilter filter);

    /**
     * Returns an array of strings naming the files discovered under a specified file store path name. If the path name
     * is null, the entire file store will be searched. Otherwise the file store is recursively searched under the
     * specified path name for files accepted by the specified filter, until the entire path tree is exhausted. This
     * returns null if the path does not exist in the store. Returned names are relative pointers to a file in the
     * store; they are not necessarily a file system path.
     *
     * @param pathName relative path in the file store to search, or null to search the entire file store.
     * @param filter   a file filter.
     *
     * @return an array of file store names. Returns null if the path does not exist in the file store.
     *
     * @throws IllegalArgumentException if the filter is null.
     */
    String[] listAllFileNames(String pathName, FileStoreFilter filter);

    /**
     * Returns an array of strings naming the files discovered under a specified file store path name. If the path name
     * is null, the entire file store will be searched. Otherwise the file store is recursively searched under each
     * branch of the the specified path name until a matching file is found, or that branch is exhausted. Unlinke {@link
     * #listAllFileNames(String, FileStoreFilter)}, This has the effect of locating the top file name under each branch.
     * This returns null if the path does not exist in the store. Returned names are relative pointers to a file in the
     * store; they are not necessarily a file system path.
     *
     * @param pathName relative path in the file store to search, or null to search the entire file store.
     * @param filter   a file filter.
     *
     * @return an array of file store names. Returns null if the path does not exist in the file store.
     *
     * @throws IllegalArgumentException if the filter is null.
     */
    String[] listTopFileNames(String pathName, FileStoreFilter filter);
}
