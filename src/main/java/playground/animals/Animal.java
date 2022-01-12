package playground.animals;

public class Animal {

    String sound() {
        return "Animal Sound";
    }

    Animal giveBirthTo() {
        return new Cat().giveBirthTo();
    }

    public static void main(String[] args) {
        Animal bird = new Bird();
        Cat blackCat = new BlackCat();
        blackCat.giveBirthTo().sound();
    }
}
