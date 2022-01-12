package playground.animals;

public class BlackCat extends Cat {

    @Override
    String sound() {
        return "BlackCat";
    }

    @Override
    Animal giveBirthTo() {
        return this;
    }
}
