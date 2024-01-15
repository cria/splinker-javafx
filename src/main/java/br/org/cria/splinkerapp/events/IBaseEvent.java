package br.org.cria.splinkerapp.events;

import javafx.concurrent.Task;

public interface IBaseEvent {
    
    Task getTask() throws Exception;
}
