package playground.animals;

public class Cat extends Animal {
    @Override
    String sound() {
        return "Cat";
    }

    @Override
    Animal giveBirthTo() {
        return new BlackCat().giveBirthTo();
    }
}
