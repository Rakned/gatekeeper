package org.parkers.gatekeep.gamedata;

import java.awt.image.BufferedImage;

class Unit {
    MyImage portrait;
    private int x, y;

    Unit(MyImage image, int tsize) {
        x = -1;
        y = -1;

        portrait = image.copyResize(tsize);
    }

    void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
    boolean isAt(int x, int y) {
        return (this.x == x) && (this.y == y);
    }
}
