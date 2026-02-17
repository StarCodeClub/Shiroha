package moe.starcraft.shiroha.utils;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.Priority;
import com.ishland.c2me.base.common.scheduler.LockTokenImpl;
import com.ishland.c2me.base.common.scheduler.ScheduledTask;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.flowsched.executor.LockToken;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class FlowSchedScheduledTask2CUTask implements PrioritisedExecutor.PrioritisedTask {
    private static final int STATE_CREATED = 0;
    private static final int STATE_QUEUED = 1;
    private static final int STATE_CANCELLED = 2;

    private volatile int state;
    private volatile Priority priority;

    private final ScheduledTask<?> task;
    private final SchedulingManager worker;
    private final ChunkPos pos;

    @Contract("_, _, _, _ -> new")
    private static <T> @NotNull ScheduledTask<T> createFlowSchedTaskOf(@NotNull ChunkPos target, int radius, SchedulingManager schedulingManager, Runnable action) {
        ObjectArrayList<LockToken> lockTargets = new ObjectArrayList<>((2 * radius + 1) * (2 * radius + 1) + 1);
        for (int x = target.x - radius; x <= target.x + radius; x++)
            for (int z = target.z - radius; z <= target.z + radius; z++)
                lockTargets.add(new LockTokenImpl(schedulingManager.getId(), ChunkPos.asLong(x, z), LockTokenImpl.Usage.WORLDGEN));

        return new ScheduledTask<>(
                target.toLong(),
                () -> {
                    action.run();
                    return CompletableFuture.completedFuture(null);
                },
                lockTargets.toArray(LockToken[]::new));
    }

    public FlowSchedScheduledTask2CUTask(Runnable task, ChunkPos pos, int radius, SchedulingManager worker) {
        this(createFlowSchedTaskOf(pos, radius, worker, task), pos, worker, Priority.NORMAL);
    }

    public FlowSchedScheduledTask2CUTask(Runnable task, ChunkPos pos, int radius, SchedulingManager worker, Priority priority) {
        this(createFlowSchedTaskOf(pos, radius, worker, task), pos, worker, priority);
    }

    private FlowSchedScheduledTask2CUTask(ScheduledTask<?> task, ChunkPos pos, SchedulingManager worker, Priority priority) {
        this.task = task;
        this.pos = pos;
        this.worker = worker;
        this.priority = priority;
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
            this.task.setPriority(this.priority.priority);
            this.worker.enqueue(this.task);
            this.worker.updatePriority(this.pos, this.priority.priority);
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
            this.worker.updatePriority(this.pos, this.priority.priority);
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
