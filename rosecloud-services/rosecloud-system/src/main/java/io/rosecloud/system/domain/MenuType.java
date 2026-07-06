package io.rosecloud.system.domain;

/** Menu node type (stored as a tinyint code). */
public enum MenuType {

    DIRECTORY(0),
    MENU(1),
    BUTTON(2);

    private final int code;

    MenuType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static MenuType of(int code) {
        for (MenuType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown menu type: " + code);
    }
}
