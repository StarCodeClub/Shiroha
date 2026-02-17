package com.ishland.c2me.base.common.scheduler;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.Priority;
import com.ishland.flowsched.executor.ExecutorManager;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import moe.starcraft.shiroha.utils.FlowSchedRunnableTask2CUTask;
import moe.starcraft.shiroha.utils.FlowSchedScheduledTask2CUTask;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

public class SchedulingManager {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final ConcurrentMap<Long, FreeableTaskList> pos2Tasks = new ConcurrentHashMap<>();
    private final Long2IntOpenHashMap prioritiesFromLevel = new Long2IntOpenHashMap() {
        @Override
        protected void rehash(int newN) {
            if (n < newN) {
                super.rehash(newN);
            }
        }
    };
    private final StampedLock prioritiesLock = new StampedLock();
    private final int id = COUNTER.getAndIncrement();
    private volatile ChunkPos currentSyncLoad = null;

    private boolean consolidatingLevelUpdates = false;
    private Queue<Runnable> consolidatedLevelUpdates = new ArrayDeque<>();

    private final Executor executor;
    private final ExecutorManager worker;

    {
        prioritiesFromLevel.defaultReturnValue(MAX_LEVEL);
    }

    public SchedulingManager(Executor executor, ExecutorManager worker) {
        this.executor = executor;
        this.worker = worker;
    }

    public ExecutorManager getWorker() {
        return this.worker;
    }

    public void enqueue(AbstractPosAwarePrioritizedTask task) {
        retry:
        while (true) {
            final long pos = task.getPos();
            final FreeableTaskList locks = this.pos2Tasks.computeIfAbsent(pos, unused -> new FreeableTaskList());
            synchronized (locks) {
                if (locks.freed) continue retry;
                locks.add(task);
            }
            task.setPriority(this.getPriority(pos));
            task.addPostExec(() -> {
                final FreeableTaskList tasks = this.pos2Tasks.get(task.getPos());
                if (tasks != null) {
                    synchronized (tasks) {
                        if (tasks.freed) return;
                        tasks.remove(task);
                        if (tasks.isEmpty()) {
                            tasks.freed = true;
                        }
                    }
                    if (tasks.freed) {
                        this.pos2Tasks.remove(task.getPos());
                    }
                }
            });
            this.worker.schedule(task);
            return;
        }
    }

    public void enqueue(long pos, Runnable command) {
        this.enqueue(new WrappingTask(pos, command));
    }

    public Executor positionedExecutor(long pos) {
        return command -> this.enqueue(pos, command);
    }

    public void updatePriorityFromLevel(long pos, int level) {
        this.executor.execute(() -> {
            updatePriorityFromLevel0(pos, level);
        });
    }

    private void updatePriorityFromLevel0(long pos, int level) {
        if (this.getPriorityFromMap(pos) == level) return;
        final long stamp = this.prioritiesLock.writeLock();
        try {
            if (level < MAX_LEVEL) {
                this.prioritiesFromLevel.put(pos, level);
            } else {
                this.prioritiesFromLevel.remove(pos);
            }
        } finally {
            this.prioritiesLock.unlockWrite(stamp);
        }
        updatePriorityInternal(pos);
    }

    public void updatePriorityFromLevelOnMain(long pos, int level) {
        if (this.consolidatingLevelUpdates) {
            this.consolidatedLevelUpdates.add(() -> updatePriorityFromLevel0(pos, level));
        } else {
            updatePriorityFromLevel(pos, level);
        }
    }

    public void setConsolidatingLevelUpdates(boolean value) {
        this.consolidatingLevelUpdates = value;
        if (!value) {
            if (!this.consolidatedLevelUpdates.isEmpty()) {
                Queue<Runnable> runnables = this.consolidatedLevelUpdates;
                this.consolidatedLevelUpdates = new ArrayDeque<>();
                this.executor.execute(() -> {
                    for (Runnable runnable : runnables) {
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    public void updatePriority(ChunkPos pos, int priority) {
        this.executor.execute(() -> this.updatePriorityInternal(pos.toLong(), priority));
    }

    private void updatePriorityInternal(long pos, int priority) {
        final FreeableTaskList locks = this.pos2Tasks.get(pos);
        if (locks != null) {
            synchronized (locks) {
                if (locks.freed) return;
                for (AbstractPosAwarePrioritizedTask lock : locks) {
                    lock.setPriority(priority);
                    this.worker.notifyPriorityChange(lock);
                }
            }
        }
    }

    private void updatePriorityInternal(long pos) {
        final int priority = getPriority(pos);
        final FreeableTaskList locks = this.pos2Tasks.get(pos);
        if (locks != null) {
            synchronized (locks) {
                if (locks.freed) return;
                for (AbstractPosAwarePrioritizedTask lock : locks) {
                    lock.setPriority(priority);
                    this.worker.notifyPriorityChange(lock);
                }
            }
        }
    }

    private int getPriority(long pos) {
        final int fromLevel = getPriorityFromMap(pos);
        int fromSyncLoad;
        ChunkPos currentSyncLoad1 = currentSyncLoad;
        if (currentSyncLoad1 != null) {
            final int chebyshevDistance = chebyshev(new ChunkPos(pos), currentSyncLoad1);
            if (chebyshevDistance <= 8) {
                fromSyncLoad = chebyshevDistance;
//                System.out.println("dist for chunk [%d,%d] is %d".formatted(currentSyncLoad.x, currentSyncLoad.z, chebyshevDistance));
            } else {
                fromSyncLoad = MAX_LEVEL;
            }
        } else {
            fromSyncLoad = MAX_LEVEL;
        }
        return Math.min(fromLevel, fromSyncLoad);
    }

    private int getPriorityFromMap(long pos) {
        int fromLevel = MAX_LEVEL;
        long stamp = this.prioritiesLock.tryOptimisticRead();
        try {
            fromLevel = this.prioritiesFromLevel.get(pos);
        } catch (Throwable t) {
        }
        if (!this.prioritiesLock.validate(stamp)) {
            stamp = this.prioritiesLock.readLock();
            try {
                fromLevel = this.prioritiesFromLevel.get(pos);
            } finally {
                this.prioritiesLock.unlockRead(stamp);
            }
        }
        return fromLevel;
    }

    public void setCurrentSyncLoad(ChunkPos pos) {
        executor.execute(() -> {
            if (this.currentSyncLoad != null) {
                final ChunkPos lastSyncLoad = this.currentSyncLoad;
                this.currentSyncLoad = null;
                updateSyncLoadInternal(lastSyncLoad);
            }
            if (pos != null) {
                this.currentSyncLoad = pos;
                updateSyncLoadInternal(pos);
            }
        });
    }

    public int getId() {
        return this.id;
    }

    private void updateSyncLoadInternal(ChunkPos pos) {
        long startTime = System.nanoTime();
        for (int xOff = -8; xOff <= 8; xOff++) {
            for (int zOff = -8; zOff <= 8; zOff++) {
                updatePriorityInternal(ChunkPos.asLong(pos.x + xOff, pos.z + zOff));
            }
        }
        long endTime = System.nanoTime();
    }

    private static int chebyshev(ChunkPos a, ChunkPos b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z));
    }

    private static int chebyshev(long a, long b) {
        return Math.max(Math.abs(ChunkPos.getX(a) - ChunkPos.getX(b)), Math.abs(ChunkPos.getZ(a) - ChunkPos.getZ(b)));
    }

    private static class FreeableTaskList extends ObjectArraySet<AbstractPosAwarePrioritizedTask> {

        private boolean freed = false;

    }

    // Luminol - Moonrise extensions
    public PrioritisedExecutor.PrioritisedTask createTask(Runnable runnable, Priority priority) {
        return new FlowSchedRunnableTask2CUTask(runnable, this, priority);
    }

    public PrioritisedExecutor.PrioritisedTask createTask(Runnable runnable) {
        return new FlowSchedRunnableTask2CUTask(runnable, this);
    }

    public PrioritisedExecutor.PrioritisedTask createRadiusTask(Runnable runnable, int x, int z, int radius) {
        return new FlowSchedScheduledTask2CUTask(runnable, new ChunkPos(x, z), radius, this);
    }

    public PrioritisedExecutor.PrioritisedTask createRadiusTask(Runnable runnable, int x, int z, int radius, Priority priority) {
        return new FlowSchedScheduledTask2CUTask(runnable, new ChunkPos(x, z), radius, this, priority);
    }

    public PrioritisedExecutor asCUExecutor() {
        return new PrioritisedExecutor() {
            @Override
            public long getTotalTasksScheduled() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getTotalTasksExecuted() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long generateNextSubOrder() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean executeTask() throws IllegalStateException {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean shutdown() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isShutdown() {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrioritisedTask queueTask(Runnable task) {
                final PrioritisedTask created = SchedulingManager.this.createTask(task);
                created.queue();
                return created;
            }

            @Override
            public PrioritisedTask queueTask(Runnable task, Priority priority) {
                final PrioritisedTask created = SchedulingManager.this.createTask(task, priority);
                created.queue();
                return created;
            }

            @Override
            public PrioritisedTask queueTask(Runnable task, Priority priority, long subOrder, long stream) {
                return SchedulingManager.this.createTask(task, priority);
            }

            @Override
            public PrioritisedTask createTask(Runnable task) {
                return SchedulingManager.this.createTask(task);
            }

            @Override
            public PrioritisedTask createTask(Runnable task, Priority priority) {
                return SchedulingManager.this.createTask(task, priority);
            }

            @Override
            public PrioritisedTask createTask(Runnable task, Priority priority, long subOrder, long stream) {
                return SchedulingManager.this.createTask(task, priority);
            }
        };
    }
}
