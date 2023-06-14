package br.org.cria.splinkerapp.services.interfaces;

import java.util.List;

public interface IDarwinCoreArchiveService {
    List<String> getDarwinCoreFields();
    boolean generateTXTFile();
    boolean generateZIPFile();
    boolean readDataFromSource();
}
