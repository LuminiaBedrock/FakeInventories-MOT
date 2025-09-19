package me.iwareq.fakeinventories.util;

import cn.nukkit.Server;
import cn.nukkit.scheduler.TaskHandler;
import me.iwareq.fakeinventories.FakeInventories;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FakeInvDispatcher {

    private final Map<Integer, TaskHandler> taskHandlers = new HashMap<>();
    private final Map<Integer, AtomicInteger> taskAttempts = new HashMap<>();

    public void run(int windowId, Runnable callback) {
        if (this.taskHandlers.containsKey(windowId)) {
            return;
        }
        this.taskHandlers.put(windowId, Server.getInstance().getScheduler().scheduleRepeatingTask(
                FakeInventories.getInstance(),
                () -> {
                    callback.run();

                    AtomicInteger taskAttempt = this.taskAttempts.computeIfAbsent(windowId, w -> new AtomicInteger());
                    if (taskAttempt.incrementAndGet() >= 20) {
                        this.cancel(windowId);
                    }
                }, 1)
        );
    }

    public void cancel(int windowId) {
        this.taskAttempts.remove(windowId);
        TaskHandler taskHandler = this.taskHandlers.get(windowId);
        if (taskHandler != null) {
            taskHandler.cancel();
        }
    }

    public boolean isActive() {
        return !this.taskHandlers.isEmpty();
    }
}
