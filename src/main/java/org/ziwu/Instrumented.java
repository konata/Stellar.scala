package org.ziwu;

public class Instrumented {

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
}
