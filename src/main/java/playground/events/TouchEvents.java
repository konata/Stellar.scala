package playground.events;

public class TouchEvents implements Events {
    @Override
    public String foo(Events src) {
        return "touch";
    }
}
