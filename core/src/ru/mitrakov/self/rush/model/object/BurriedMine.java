package ru.mitrakov.self.rush.model.object;

/**
 * Created by mitrakov on 23.02.2017
 */

public class BurriedMine extends CellObject {
    public BurriedMine(int xy, int number) {
        super(0x15, xy);
        this.number = number;
    }
}
