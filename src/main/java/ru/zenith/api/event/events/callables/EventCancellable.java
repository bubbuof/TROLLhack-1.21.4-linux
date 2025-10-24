package ru.zenith.api.event.events.callables;

import lombok.Setter;
import ru.zenith.api.event.events.Cancellable;
import ru.zenith.api.event.events.Event;

public abstract class EventCancellable implements Event, Cancellable {

    @Setter
    private boolean cancelled;

    protected EventCancellable() {
    }

    /**
     * @see com.darkmagician6.eventapi.events.Cancellable.isCancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * @see com.darkmagician6.eventapi.events.Cancellable.setCancelled
     */

    @Override
    public void cancel() {
        cancelled = true;
    }
}