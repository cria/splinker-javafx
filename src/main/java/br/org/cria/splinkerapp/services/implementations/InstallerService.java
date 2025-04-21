package br.org.cria.splinkerapp.services.implementations;

/**
 * Serviço para compartilhar informações do instalador entre controllers
 */
public class InstallerService {

    // Armazena o caminho do arquivo de instalação
    private static String installerPath;

    /**
     * Armazena o caminho do instalador para uso posterior
     * @param path Caminho completo para o arquivo do instalador
     */
    public static void setInstallerPath(String path) {
        installerPath = path;
    }

    /**
     * Obtém o caminho do instalador
     * @return Caminho completo para o arquivo do instalador
     */
    public static String getInstallerPath() {
        return installerPath;
    }

    /**
     * Verifica se um caminho de instalador válido foi definido
     * @return true se um caminho válido existe, false caso contrário
     */
    public static boolean hasValidInstallerPath() {
        return installerPath != null && !installerPath.trim().isEmpty();
    }

    /**
     * Limpa o caminho do instalador
     */
    public static void clearInstallerPath() {
        installerPath = null;
    }
}