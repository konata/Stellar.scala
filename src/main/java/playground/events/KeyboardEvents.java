package playground.events;

public class KeyboardEvents implements Events {
    @Override
    public String foo(Events src) {
        return "keyboard";
    }
}
