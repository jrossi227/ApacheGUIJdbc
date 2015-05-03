package net.apachegui.locks;


import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockManager {

    private static Logger log = Logger.getLogger(LockManager.class);

    private static LockManager instance;

    private HashMap<String, FileLockTracker> fileLockTrackers;
    private HashMap<String, ReentrantReadWriteLock> jvmLocks;

    private LockManager() {
        fileLockTrackers = new HashMap<String, FileLockTracker>();
        jvmLocks = new HashMap<String, ReentrantReadWriteLock>();
    }

    public static LockManager getInstance() {
        synchronized (LockManager.class) {
            if(instance == null) {
                synchronized (LockManager.class) {
                    instance = new LockManager();
                }
            }
        }

        return instance;
    }

    private ReentrantReadWriteLock getJvmLockObject(String file) {

        ReentrantReadWriteLock lock = jvmLocks.get(file);
        if (lock == null) {
            lock = jvmLocks.get(file);
            if (lock == null) {
                lock = new ReentrantReadWriteLock();
                jvmLocks.put(file, lock);
            }
        }

        return lock;
    }

    private FileLockTracker getFileLockTrackerObject(String file) {

        FileLockTracker lock = fileLockTrackers.get(file);
        if (lock == null) {
            lock = fileLockTrackers.get(file);
            if (lock == null) {
                lock = new FileLockTracker(file);
                fileLockTrackers.put(file, lock);
            }
        }

        return lock;
    }

    private void unlockFileTrackerLock(String file) {
        FileLockTracker fileLockTracker = getFileLockTrackerObject(file);
        int lockNum = fileLockTracker.getLockNum() -1;
        if(lockNum == 0) {
            try {
                FileLock lock = fileLockTracker.getFileLock();
                if( lock != null ) {
                    lock.release();
                }

                FileChannel channel = fileLockTracker.getChannel();
                if( channel != null) {
                    channel.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        fileLockTracker.setLockNum(lockNum);
    }

    public synchronized void lockRead(String file) throws IOException {
        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.readLock().lock();

        FileLockTracker fileLockTracker = getFileLockTrackerObject(file);
        if(fileLockTracker.getLockNum() == 0) {
            fileLockTracker.setLockNum(1);

            File fileObj = new File(file);
            if (!fileObj.exists()) {
                fileObj.createNewFile();
            }

            Path path = Paths.get(file);
            fileLockTracker.setChannel(FileChannel.open(path, StandardOpenOption.READ));
            fileLockTracker.setFileLock(fileLockTracker.getChannel().lock(0, Long.MAX_VALUE, true));

        } else {
            fileLockTracker.setLockNum(fileLockTracker.getLockNum() + 1);
        }

    }

    public synchronized void unlockRead(String file) {
        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.readLock().unlock();

        unlockFileTrackerLock(file);
    }

    public synchronized void lockWrite(String file) throws IOException {
        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.writeLock().lock();

        FileLockTracker fileLockTracker = getFileLockTrackerObject(file);
        if(fileLockTracker.getLockNum() == 0) {
            fileLockTracker.setLockNum(1);

            File fileObj = new File(file);
            if (!fileObj.exists()) {
                fileObj.createNewFile();
            }

            Path path = Paths.get(file);
            fileLockTracker.setChannel(FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE));
            fileLockTracker.setFileLock(fileLockTracker.getChannel().lock());

        } else if(fileLockTracker.getCurrentOperation() == Operation.READ) {

            try {
                FileLock lock = fileLockTracker.getFileLock();
                if( lock != null ) {
                    lock.release();
                }

                FileChannel channel = fileLockTracker.getChannel();
                if( channel != null) {
                    channel.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            Path path = Paths.get(file);
            fileLockTracker.setChannel(FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE));
            fileLockTracker.setFileLock(fileLockTracker.getChannel().lock());

        } else {
            fileLockTracker.setLockNum(fileLockTracker.getLockNum() + 1);
        }
    }

    public synchronized void unlockWrite(String file) {
        ReentrantReadWriteLock jvmLock = getJvmLockObject(file);
        jvmLock.writeLock().unlock();

        unlockFileTrackerLock(file);
    }


}
