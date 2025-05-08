package br.org.cria.splinkerapp.config;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.utils.ModalAlertUtil;
import br.org.cria.splinkerapp.utils.SystemConfigurationUtil;
import io.sentry.Sentry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LockFileManager {
    private static final String LOCK_FILE_NAME = "%s/spLinker.lock".formatted(System.getProperty("user.dir"));

    public static void verifyLockFile() {
        final File lockFile = new File(LOCK_FILE_NAME);
        try {
            if (SystemConfigurationUtil.runInDevelopment() && Files.exists(Path.of(LOCK_FILE_NAME))) {
                deleteLockfile();
            }
            var fileAlreadyExists = lockFile.exists();
            if (fileAlreadyExists) {
                ModalAlertUtil.show("Não é possível rodar mais de uma instãncia do splinker ao mesmo tempo. Feche a outra instância.");
                System.exit(0);
            }
            lockFile.createNewFile();
        } catch (IOException e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
        }
    }

    public static void deleteLockfile() {
        try {
            Files.delete(Path.of(LOCK_FILE_NAME));
        } catch (Exception e) {
            Sentry.captureException(e);
            ApplicationLog.error(e.getLocalizedMessage());
        }
    }
}
