package playground.animals;

public class Husky extends Dog {
    @Override
    String sound() {
        return "Husky";
    }

    @Override
    Animal giveBirthTo() {
        return this;
    }
}
