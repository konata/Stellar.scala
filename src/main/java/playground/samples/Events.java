package playground.samples;

public interface Events {
    default String foo(Events src) {
        return "call from : " + getClass();
    }
}
