package playground.events;

public class MouseEvents implements Events {
    @Override
    public String foo(Events src) {
        return "mouse";
    }
}
