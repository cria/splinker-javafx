package br.org.cria.splinkerapp.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import br.org.cria.splinkerapp.ApplicationLog;

public final class LockFileManager {
   private static final String LOCK_FILE_NAME = "%s/spLinker.lock".formatted(System.getProperty("user.dir")) ;
   public static void verifyLockFile()
   {
    final File lockFile = new File(LOCK_FILE_NAME);
    try {
        var fileAlreadyExists = lockFile.exists();
        if (fileAlreadyExists) {
            System.exit(0);
        }
        lockFile.createNewFile();
        // your main application logic goes here
    } catch (IOException e) {
        ApplicationLog.error(e.getLocalizedMessage());
    }
   }

   public static void deleteLockfile() 
   {
        try 
        {
            Files.delete(Path.of(LOCK_FILE_NAME));    
        } 
        catch (Exception e) 
        {
            ApplicationLog.error(e.getLocalizedMessage());
        }
   }
}
