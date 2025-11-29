package i.moniasuki.shiroha.utils;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class FlowSchedScheduledTask2CCTask implements PrioritisedExecutor.PrioritisedTask {
    private final ScheduledTask<?> internal;
    private final SchedulingManager worker;
    private final AtomicBoolean queued = new AtomicBoolean(false);

    @Contract("_, _, _, _ -> new")
    private static <T> @NotNull ScheduledTask<T> createFlowSchedTaskOf(ChunkPos target, int radius, SchedulingManager schedulingManager, Runnable action) {
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

    public FlowSchedScheduledTask2CCTask(Runnable task, ChunkPos pos, int radius, SchedulingManager worker) {
        this(createFlowSchedTaskOf(pos, radius, worker, task), worker);
    }

    public FlowSchedScheduledTask2CCTask(ScheduledTask<?> internal, SchedulingManager worker) {
        this.internal = internal;
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

        this.worker.enqueue(this.internal);
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
        return true;
    }

    @Override
    public Priority getPriority() {
        return null;
    }

    @Override
    public boolean setPriority(Priority priority) {
        return true;
    }

    @Override
    public boolean raisePriority(Priority priority) {
        return true;
    }

    @Override
    public boolean lowerPriority(Priority priority) {
        return true;
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
