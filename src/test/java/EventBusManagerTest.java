import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

import br.org.cria.splinkerapp.managers.EventBusManager;

public class EventBusManagerTest {
    
    @Test
    public void getEventBusTest() 
    {
        var bus = EventBusManager.getEvent("TestEvent");
        assertEquals(bus.getClass(), EventBus.class);
    }
}
