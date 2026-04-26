import net.minecraftforge.event.ServerChatEvent;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        for (Method m : ServerChatEvent.class.getMethods()) {
            System.out.println(m.getName());
        }
    }
}
