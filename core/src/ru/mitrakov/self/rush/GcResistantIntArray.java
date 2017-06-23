package ru.mitrakov.self.rush;

import com.badlogic.gdx.utils.IntArray;

import java.io.UnsupportedEncodingException;

import ru.mitrakov.self.rush.utils.collections.IIntArray;

/**
 * Created by mitrakov on 19.05.2017
 */
public final class GcResistantIntArray implements IIntArray {
    private final IntArray array;
    private final byte[] bytes;

    public GcResistantIntArray(int bufSize) {
        array = new IntArray(bufSize);
        bytes = new byte[bufSize];
    }

    @Override
    public int get(int idx) {
        return array.get(idx);
    }

    @Override
    public IIntArray add(int item) {
        array.add(item);
        return this;
    }

    @Override
    public IIntArray prepend(int item) {
        array.insert(0, item);
        return this;
    }

    @Override
    public IIntArray remove(int startPos, int endPos) {
        array.removeRange(startPos, endPos-1);
        return this;
    }

    @Override
    public IIntArray clear() {
        array.clear();
        return this;
    }

    @Override
    public int length() {
        return array.size;
    }

    /**
     * ...
     * if data.length() or length is larger than bufSize it's OK (internal buffer will be resized)
     * @param data
     * @param length
     * @return
     */
    @Override
    public IIntArray copyFrom(IIntArray data, int length) {
        array.clear();
        for (int i = 0; i < Math.min(data.length(), length); i++) {
            array.add(data.get(i));
        }
        return this;
    }

    @Override
    public IIntArray fromByteArray(byte[] data, int length) {
        array.clear();
        for (int i = 0; i < Math.min(data.length, length); i++) {
            array.add(data[i] >= 0 ? data[i] : data[i] + 256);
        }
        return this;
    }

    @Override
    public byte[] toByteArray() {
        for (int i = 0; i < Math.min(bytes.length, array.size) ; i++) {
            bytes[i] = (byte) array.get(i);
        }
        return bytes;
    }

    @Override
    public String toUTF8() {
        try {
            return new String(toByteArray(), 0, array.size, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    @Override
    public String toString() {
        return array.toString();
    }
}
