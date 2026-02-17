package moe.starcraft.shiroha.utils;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.Priority;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.flowsched.executor.SimpleTask;

public class FlowSchedRunnableTask2CUTask implements PrioritisedExecutor.PrioritisedTask {
    private static final int STATE_CREATED = 0;
    private static final int STATE_QUEUED = 1;
    private static final int STATE_CANCELLED = 2;

    private volatile int state;
    private volatile Priority priority;

    private final SimpleTask task;
    private final SchedulingManager worker;

    public FlowSchedRunnableTask2CUTask(Runnable task, SchedulingManager worker, Priority priority) {
        this.worker = worker;
        this.priority = priority;
        this.task = new SimpleTask(task, this.priority.priority);
    }

    public FlowSchedRunnableTask2CUTask(Runnable task, SchedulingManager worker) {
        this(task, worker, Priority.NORMAL);
    }

    @Override
    public PrioritisedExecutor getExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean queue() {
        synchronized (this) {
            if (this.state > STATE_CREATED) {
                return false;
            }

            this.state = STATE_QUEUED;
            this.worker.getWorker().schedule(this.task);
            return true;
        }
    }

    @Override
    public boolean isQueued() {
        return this.state == STATE_QUEUED;
    }

    @Override
    public boolean cancel() {
        synchronized (this) {
            if (this.state == STATE_CANCELLED || this.state == STATE_QUEUED) {
                return false;
            }

            this.state = STATE_CANCELLED;
            return true;
        }
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }

    @Override
    public boolean setPriority(Priority priority) {
        synchronized (this) {
            if (this.state == STATE_CANCELLED || this.state == STATE_QUEUED) {
                return false;
            }

            this.priority = priority;
            this.worker.getWorker().notifyPriorityChange(this.task);
            return true;
        }
    }

    @Override
    public boolean setPrioritySubOrderStream(Priority priority, long subOrder, long stream) {
        return this.setPriority(priority);
    }

    @Override
    public boolean raisePriority(Priority priority) {
        return this.setPriority(priority);
    }

    @Override
    public boolean lowerPriority(Priority priority) {
        return this.setPriority(priority);
    }

    @Override
    public boolean execute() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSubOrder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setSubOrder(long subOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean raiseSubOrder(long subOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean lowerSubOrder(long subOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setStream(long stream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrioritisedExecutor.PriorityState getPriorityState() {
        throw new UnsupportedOperationException();
    }
}
