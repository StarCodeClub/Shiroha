package i.moniasuki.shiroha.utils;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.Priority;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import net.minecraft.server.level.ChunkLevel;

import java.util.concurrent.atomic.AtomicBoolean;

public class FlowSchedRunnableTask2CCTask implements PrioritisedExecutor.PrioritisedTask {
    private final AtomicBoolean queued = new AtomicBoolean(false);
    private final Runnable task;
    private final SchedulingManager worker;

    public FlowSchedRunnableTask2CCTask(Runnable task, SchedulingManager worker) {
        this.task = task;
        this.worker = worker;
    }

    @Override
    public PrioritisedExecutor getExecutor() {
        return null;
    }

    @Override
    public boolean queue() {
        if (!this.queued.compareAndSet(false, true)) {
            return false;
        }

        this.worker.getWorker().schedule(this.task, ChunkLevel.MAX_LEVEL + 1);
        return true;
    }

    @Override
    public boolean isQueued() {
        return this.queued.get();
    }

    @Override
    public boolean cancel() {
        return this.queued.compareAndSet(false, true);
    }

    @Override
    public boolean execute() {
        return false;
    }

    @Override
    public Priority getPriority() {
        return null;
    }

    @Override
    public boolean setPriority(Priority priority) {
        return false;
    }

    @Override
    public boolean raisePriority(Priority priority) {
        return false;
    }

    @Override
    public boolean lowerPriority(Priority priority) {
        return false;
    }

    @Override
    public long getSubOrder() {
        return 0;
    }

    @Override
    public boolean setSubOrder(long subOrder) {
        return false;
    }

    @Override
    public boolean raiseSubOrder(long subOrder) {
        return false;
    }

    @Override
    public boolean lowerSubOrder(long subOrder) {
        return false;
    }

    @Override
    public boolean setPriorityAndSubOrder(Priority priority, long subOrder) {
        return false;
    }
}
