package playground.samples;


public class Prototype {
    public static void main(String[] args) {
        Prototype a = new Prototype();
        Prototype b = new Inherited();
        Prototype c = b.foo(a);
        Prototype d = a.foo(b);

        Prototype stage = new Stage();
        Prototype _state = stage.foo(stage);
        Prototype _a = stage.foo(a);
        Prototype _b = stage.foo(b);
    }

    Prototype foo(Prototype source) {
        return source;
    }
}

class Inherited extends Prototype {
    @Override
    Prototype foo(Prototype source) {
        Prototype newly = new Prototype();
        System.out.println(newly);
        return newly;
    }
}


class Stage extends Prototype {
    boolean alwaysFalse() {
        return false;
    }

    @Override
    Prototype foo(Prototype source) {
        if (alwaysFalse()) {
            return source;
        } else {
            return new Inherited();
        }
    }
}

