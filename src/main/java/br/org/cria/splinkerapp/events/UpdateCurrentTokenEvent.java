package br.org.cria.splinkerapp.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class UpdateCurrentTokenEvent extends Event{

    public UpdateCurrentTokenEvent(Object source, EventTarget target, EventType<? extends Event> eventType) {
        super(source, target, eventType);
        //TODO Auto-generated constructor stub
    }   
}
