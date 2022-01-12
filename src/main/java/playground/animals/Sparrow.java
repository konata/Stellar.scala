package playground.animals;

public class Sparrow extends Bird {

    @Override
    String sound() {
        return "Sparrow";
    }

    @Override
    Animal giveBirthTo() {
        return this;
    }
}

