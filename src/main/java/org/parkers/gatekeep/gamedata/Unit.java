package org.parkers.gatekeep.gamedata;

import java.awt.image.BufferedImage;

class Unit {
    BufferedImage portrait;

    int x, y;

    Unit(BufferedImage image, double tsize) {
        x = -1;
        y = -1;

        double w, h, scalar;
        w = image.getWidth();
        h = image.getHeight();

        if (w > h)
            scalar = w / tsize;
        else
            scalar = h / tsize;

        image.createGraphics().scale(scalar, scalar);
    }

    void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
    boolean isAt(int x, int y) {
        return (this.x == x) && (this.y == y);
    }
}
