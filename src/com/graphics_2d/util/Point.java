package com.graphics_2d.util;

public class Point<T> {
    private final T x;
    private final T y;

    public Point(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public T getX() {
        return x;
    }

    public T getY() {
        return y;
    }
}
