package playground.animals;

public class Dog extends Animal {

    @Override
    String sound() {
        return "Dog";
    }

    @Override
    Animal giveBirthTo() {
        return new Husky().giveBirthTo();
    }
}
