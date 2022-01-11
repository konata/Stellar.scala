package sample.ziwu;

import java.util.Random;

public class Instrumented {

    String name;
    Events first;
    Events second;

    public static void entry(String[] args) {
        Instrumented app = new Instrumented();
        app.first = new KeyboardEvents(); // first allocation
        app.second = new MouseEvents(); // second allocation

        Instrumented app1 = new Instrumented();
        app1.first = new KeyboardEvents(); // third allocation
        app1.second = new MouseEvents(); // fourth allocation

        Instrumented app2 = app1;
        app2.second = app1.first;
    }

    public Object foo(Instrumented bar) {
        Instrumented app = bar;
        app.first = new KeyboardEvents(); // first allocation
        app.second = new MouseEvents(); // second allocation
        Instrumented app1 = new Instrumented();
        app1.first = new KeyboardEvents(); // third allocation
        app1.second = new MouseEvents(); // fourth allocation
        if (new Random().nextBoolean()) {
            return this;
        } else if (new Random().nextBoolean()) {
            return app1.first;
        }

        Instrumented app2 = app1;
        app2.second = app1.first;
        if (new Random().nextBoolean()) {
            return app2;
        } else {
            return app1;
        }
    }

    public static <T> T id(T foo) {
        return foo;
    }

    public void assignWithCall(Instrumented from) {
        from.name = from.second.foo(from.first);
    }


    public void relatives(Instrumented app2) {
        Instrumented app = new Instrumented();
        Events mouseEvents = new MouseEvents();
        Events keyboardEvents = new KeyboardEvents();
        app.first = mouseEvents;
        keyboardEvents = app.second;
        app.second = app2.second;
        app2.first = app.second;
        app2.second = id(app.first);
    }


}
