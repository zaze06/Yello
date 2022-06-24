package me.alien.yello.integration.events.tic.tac.toe;

public enum Mark {
    X('X'),
    O('O'),
    BLANK('-');

    private final char mark;

    Mark(char initMark) {
        this.mark = initMark;
    }

    public boolean isMarked() {
        return this != BLANK;
    }

    public char getMark() {
        return this.mark;
    }

    @Override
    public String toString() {
        return String.valueOf(mark);
    }
}