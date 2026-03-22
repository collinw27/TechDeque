package oop.practical.techdeque;

import oop.practical.techdeque.game.GameManager;

public final class Main {

    static void main() {
        System.out.println("Welcome to TechDeque! Enter -h for help.");
        new GameManager().loop();
    }

}
