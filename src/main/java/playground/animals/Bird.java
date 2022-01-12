package playground.animals;

public class Bird extends Animal {
    @Override
    String sound() {
        return "Bird";
    }

    @Override
    Animal giveBirthTo() {
        return new Sparrow().giveBirthTo();
    }
}
