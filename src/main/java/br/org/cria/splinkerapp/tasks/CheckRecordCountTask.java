package br.org.cria.splinkerapp.tasks;

import java.nio.file.Files;
import java.nio.file.Paths;

import br.org.cria.splinkerapp.services.implementations.DarwinCoreArchiveService;
import javafx.concurrent.Task;

public class CheckRecordCountTask extends Task<Boolean> {

    DarwinCoreArchiveService service;

    public CheckRecordCountTask(DarwinCoreArchiveService service) {
        this.service = service;
    }

    @Override
    protected Boolean call() throws Exception {

        var filePath = service.getTxtFilePath();
        var path = Paths.get(filePath);
        // -1 pois a contagem inclui o cabeçalho com o nome das colunas
        var currentRowCount = Files.lines(path).count() - 1;
        var lastRowCount = service.getDataSet().getLastRowCount();
        var hasRecordCountDecreased = lastRowCount > currentRowCount;

        return hasRecordCountDecreased;
    }

}
