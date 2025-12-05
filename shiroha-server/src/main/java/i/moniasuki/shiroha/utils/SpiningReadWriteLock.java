package i.moniasuki.shiroha.utils;

import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import java.lang.invoke.VarHandle;

public class SpiningReadWriteLock {
    private int referenceCount = 0;

    private static final VarHandle REFERENCE_COUNT_HANDLE = ConcurrentUtil.getVarHandle(SpiningReadWriteLock.class, "referenceCount", int.class);

    public void releaseWriteReference() {
        if (!REFERENCE_COUNT_HANDLE.compareAndSet(this, -1, 0)) {
            throw new IllegalStateException("Releasing when not write-locked");
        }
    }

    public void acquireWriteReference() {
        int failureCount = 0;
        for (;;) {
            for (int i = 0; i < failureCount; i++) {
                ConcurrentUtil.backoff();
            }

            final int curr = (int) REFERENCE_COUNT_HANDLE.getVolatile(this);

            if (curr > 0 || curr == -1) {
                failureCount++;
                continue;
            }

            if (!REFERENCE_COUNT_HANDLE.compareAndSet(this, curr, -1)) {
                failureCount++;
                continue;
            }

            break;
        }
    }

    public void releaseReadReference() {
        int failureCount = 0;
        for (;;) {
            for (int i = 0; i < failureCount; i++) {
                ConcurrentUtil.backoff();
            }

            final int curr = (int) REFERENCE_COUNT_HANDLE.getVolatile(this);

            if (curr == -1) {
                throw new IllegalStateException("Cannot release read reference when write locked");
            }

            if (curr == 0) {
                throw new IllegalStateException("Setting reference count down to a value lower than 0!");
            }

            if (!REFERENCE_COUNT_HANDLE.compareAndSet(this, curr, curr - 1)) {
                failureCount++;
                continue;
            }

            break;
        }
    }

    public void acquireReadReference() {
        int failureCount = 0;
        for (;;) {
            for (int i = 0; i < failureCount; i++) {
                ConcurrentUtil.backoff();
            }

            final int curr = (int) REFERENCE_COUNT_HANDLE.getVolatile(this);

            if (curr == -1) {
                failureCount++;
                continue;
            }

            if (!REFERENCE_COUNT_HANDLE.compareAndSet(this, curr, curr + 1)) {
                failureCount++;
                continue;
            }

            break;
        }
    }
}