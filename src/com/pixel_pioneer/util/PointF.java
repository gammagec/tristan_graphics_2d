package com.pixel_pioneer.util;

public class PointF extends Point<Float> {
    PointF(Float x, Float y) {
        super(x, y);
    }

    @Override
    public PointF delta(Float dx, Float dy) {
        return new PointF(getX() + dx, getY() + dy);
    }
}
