import static org.junit.Assert.assertEquals;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Test;
import br.org.cria.splinkerapp.config.LockFileManager;

public class LockFileManagerTest {

    @Test
    public void createsLockFileTest() {
        LockFileManager.verifyLockFile();
        var file = "%s/spLinker.lock".formatted(System.getProperty("user.dir"));
        var path = Path.of(file);
        var fileExists = Files.exists(path);
        assertEquals(true, fileExists);
      }

      @Test
      public void deletesLockFileTest() {
        LockFileManager.verifyLockFile();
        LockFileManager.deleteLockfile();
        var file = "%s/spLinker.lock".formatted(System.getProperty("user.dir"));
        var path = Path.of(file);
        var fileExists = Files.exists(path);
        assertEquals(false, fileExists);
      }

      @After
      public void tearDown(){
        LockFileManager.deleteLockfile();
      }
    
}
