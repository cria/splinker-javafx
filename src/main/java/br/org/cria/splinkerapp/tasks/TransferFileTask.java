package br.org.cria.splinkerapp.tasks;

import java.time.Instant;
import java.time.ZoneId;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.enums.EventTypes;
import br.org.cria.splinkerapp.managers.EventBusManager;
import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Task;

public class TransferFileTask extends Task<Void> {

    DarwinCoreArchiveService service;
    public TransferFileTask(DarwinCoreArchiveService service)
    {
        this.service = service;
    }

    @Override
    protected Void call() throws Exception {
        var now = Instant.now().atZone(ZoneId.systemDefault());
        var message = now.toLocalDateTime() + " - Iniciando a transmissão";
        ApplicationLog.info(message);
        service.transferData();
        return null;
    }
    
}
