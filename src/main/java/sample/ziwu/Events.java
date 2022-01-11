package sample.ziwu;

public interface Events {
    default String foo(Events src) {
        return "call from : " + getClass();
    }
}
