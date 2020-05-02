package org.parkers.gatekeep.gamedata;

import discord4j.core.object.util.Snowflake;

class Unit {
    MyImage portrait;
    int x, y;
    private Snowflake user;

    Unit(MyImage image, int tsize) {
        x = -1;
        y = -1;

        portrait = image.copyResize(tsize);
    }

    Unit(MyImage image, int tsize, Snowflake owner) {
        this(image, tsize);
        user = owner;
    }

    void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
    boolean isAt(int x, int y) {
        return (this.x == x) && (this.y == y);
    }
    boolean isUser(Snowflake s) {
        return user == null || user.equals(s);
    }
}
