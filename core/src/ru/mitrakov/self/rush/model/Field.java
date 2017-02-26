package ru.mitrakov.self.rush.model;

import java.util.HashMap;
import java.util.Map;

import ru.mitrakov.self.rush.model.object.CellObject;

/**
 * Created by mitrakov on 23.02.2017
 */

public class Field {
    public static final int WIDTH = 51;
    public static final int HEIGHT = 5;

    interface NextNumber {
        int next();
    }

    public final Cell cells[] = new Cell[WIDTH * HEIGHT]; // .... public
    private final Map<Integer, CellObject> objects = new HashMap<Integer, CellObject>(32);
    private int objectNumber = 0;

    Field(int[] fieldData) {
        if (fieldData.length != WIDTH * HEIGHT) throw new IllegalArgumentException("Incorrect field length");

        NextNumber nextNumber = new NextNumber() {
            @Override
            public int next() {
                return ++objectNumber;
            }
        };
        for (int i = 0; i < fieldData.length; i++) {
            cells[i] = Cell.newCell(fieldData[i], i, nextNumber);
            assert cells[i] != null;
            for (CellObject object : cells[i].objects) {
                objects.put(object.getNumber(), object);
            }
        }
    }

    void appendObject(final int number, int id, int xy) {
        if (xy < 0 || xy >= Field.WIDTH * Field.HEIGHT) throw new IllegalArgumentException("Incorrect xy");

        CellObject object = Cell.newObject(id, xy, new Field.NextNumber() {
            @Override
            public int next() {
                return number;
            }
        });
        if (object != null) {
            cells[xy].objects.add(object);
            objects.put(object.getNumber(), object);
        }
    }

    void setXy(int number, int newXy) {
        if (newXy < 0 || newXy > 0xFF) throw new IllegalArgumentException("Incorrect xy");

        CellObject object = objects.get(number);
        if (object != null) {
            int oldXy = object.getXy();
            if (newXy < 0xFF) {                      // replace object
                object.setXy(newXy);
                cells[oldXy].objects.remove(object);
                cells[newXy].objects.add(object);
            } else {                                 // remove object
                cells[oldXy].objects.remove(object);
                objects.remove(number);
            }
        }
    }
}
