package org.ziwu;

public interface Events {
    default void foo(Events src) {
        System.out.println("call from : " + getClass());
    }
}
