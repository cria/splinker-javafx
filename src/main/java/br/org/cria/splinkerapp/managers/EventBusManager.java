package br.org.cria.splinkerapp.managers;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.EventBus;

public class EventBusManager {

    private static Map<String, EventBus> events = new HashMap<String, EventBus>();
    

    public static EventBus getEvent(String eventName) {
        var event = events.get(eventName);
        if (event == null) 
        {
            addEvent(eventName);
            event = getEvent(eventName);
        }
        return event;
    }
    public static void addEvent(String eventName)
    {
        events.put(eventName, new EventBus(eventName));
    }

    private EventBusManager() {} // Prevent instantiation
}

