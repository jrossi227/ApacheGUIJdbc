package net.apachegui.locks;


import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockManager {

    private static Logger log = Logger.getLogger(LockManager.class);

    private static LockManager instance = null;

    private ConcurrentHashMap<String, ReentrantReadWriteLock> localLocks;

    private ConcurrentHashMap<String, FileLockTracker> fileLockTrackers;
    private ConcurrentHashMap<String, ReentrantReadWriteLock> jvmLocks;

    private LockManager() {
        fileLockTrackers = new ConcurrentHashMap<String, FileLockTracker>();
        jvmLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
        localLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    }

    public static LockManager getInstance() {
        if(instance == null) {
            synchronized (LockManager.class) {
                if(instance == null) {
                    instance = new LockManager();
                }
            }
        }

        return instance;
    }

    private ReentrantReadWriteLock getLocalLockObject(String file) {

        ReentrantReadWriteLock lock = localLocks.get(file);
        if (lock == null) {
            synchronized (this) {
                lock = localLocks.get(file);
                if (lock == null) {
                    lock = new ReentrantReadWriteLock();
                    localLocks.put(file, lock);
                }
            }
        }

        return lock;
    }

    private ReentrantReadWriteLock getJvmLockObject(String file) {

        ReentrantReadWriteLock lock = jvmLocks.get(file);
        if (lock == null) {
            synchronized (this) {
                lock = jvmLocks.get(file);
                if (lock == null) {
                    lock = new ReentrantReadWriteLock();
                    jvmLocks.put(file, lock);
                }
            }
        }

        return lock;
    }

    private FileLockTracker getFileLockTrackerObject(String file) {

        FileLockTracker lock = fileLockTrackers.get(file);
        if (lock == null) {
            synchronized (this) {
                lock = fileLockTrackers.get(file);
                if (lock == null) {
                    lock = new FileLockTracker(file);
                    fileLockTrackers.put(file, lock);
                }
            }
        }

        return lock;
    }

    private void unlockFile(String file) {
        ReentrantReadWriteLock localLock = getLocalLockObject(file);
        localLock.writeLock().lock();
        try {
            FileLockTracker fileLockTracker = getFileLockTrackerObject(file);
            int lockNum = fileLockTracker.getLockNum() - 1;
            if (lockNum == 0) {
                try {
                    FileLock lock = fileLockTracker.getFileLock();
                    if (lock != null) {
                        lock.release();
                    }

                    FileChannel channel = fileLockTracker.getChannel();
                    if (channel != null) {
                        channel.close();
                    }
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            fileLockTracker.setLockNum(lockNum);
            fileLockTrackers.put(file, fileLockTracker);
        } finally {
            localLock.writeLock().unlock();
        }
    }

    private void lockFile(String file, Operation operation) throws IOException
    {
        ReentrantReadWriteLock localLock = getLocalLockObject(file);
        localLock.writeLock().lock();
        try {
            FileLockTracker fileLockTracker = getFileLockTrackerObject(file);
            if (fileLockTracker.getLockNum() == 0) {
                fileLockTracker.setLockNum(1);

                File fileObj = new File(file);
                if (!fileObj.exists()) {
                    fileObj.createNewFile();
                }

                Path path = Paths.get(file);
                if(operation == Operation.READ) {
                    fileLockTracker.setChannel(FileChannel.open(path, StandardOpenOption.READ));
                    fileLockTracker.setFileLock(fileLockTracker.getChannel().lock(0, Long.MAX_VALUE, true));
                } else {
                    fileLockTracker.setChannel(FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE));
                    fileLockTracker.setFileLock(fileLockTracker.getChannel().lock());
                }
            }
            else {
                fileLockTracker.setLockNum(fileLockTracker.getLockNum() + 1);
            }

            fileLockTrackers.put(file, fileLockTracker);
        } finally {
            localLock.writeLock().unlock();
        }
    }

    public void lockRead(String file) throws IOException {

        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.readLock().lock();

        lockFile(file, Operation.READ);
    }

    public void unlockRead(String file) {
        unlockFile(file);

        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.readLock().unlock();
    }

    public void lockWrite(String file) throws IOException {
        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.writeLock().lock();

        lockFile(file, Operation.WRITE);
    }

    public void unlockWrite(String file) {
        unlockFile(file);

        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.writeLock().unlock();
    }


}
