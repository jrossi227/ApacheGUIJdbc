package net.apachegui.locks;

import net.apachegui.db.JdbcConnection;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FileLockTracker {

    private String file;
    private FileLock fileLock = null;
    private FileChannel channel = null;
    private int lockNum;
    private Operation currentOperation;

    public FileLockTracker(String file) {
        this.file = file;
        this.lockNum = 0;
        currentOperation = Operation.READ;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public FileLock getFileLock() {
        return fileLock;
    }

    public void setFileLock(FileLock fileLock) {
        this.fileLock = fileLock;
    }

    public FileChannel getChannel() {
        return channel;
    }

    public void setChannel(FileChannel channel) {
        this.channel = channel;
    }

    public int getLockNum() {
        return lockNum;
    }

    public void setLockNum(int lockNum) {
        this.lockNum = lockNum;
    }

    public Operation getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(Operation currentOperation) {
        this.currentOperation = currentOperation;
    }

}
