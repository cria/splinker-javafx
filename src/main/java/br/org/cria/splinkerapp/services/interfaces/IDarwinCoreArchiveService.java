package br.org.cria.splinkerapp.services.interfaces;

public interface IDarwinCoreArchiveService {
    boolean generateTXTFile();
    boolean generateZIPFile();
    boolean readDataFromSource();
}
