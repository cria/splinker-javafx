package br.org.cria.splinkerapp.managers;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.EventBus;

public class EventBusManager {

    private static final Map<String, EventBus> events = new HashMap<>();


    public static EventBus getEvent(String eventName) {
        var event = events.get(eventName);
        if (event == null) {
            addEvent(eventName);
            event = getEvent(eventName);
        }
        return event;
    }

    public static void addEvent(String eventName) {
        events.put(eventName, new EventBus(eventName));
    }

    public static void unregister(Object subscriber) {
        for (EventBus event : events.values()) {
            try {
                event.unregister(subscriber);
            } catch (IllegalArgumentException ignored) {
                // The controller does not subscribe to this event bus.
            }
        }
    }

    private EventBusManager() {
    } // Prevent instantiation
}

