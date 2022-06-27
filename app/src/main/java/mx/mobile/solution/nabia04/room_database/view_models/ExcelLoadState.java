package mx.mobile.solution.nabia04.room_database.view_models;

public class ExcelLoadState {

    private final boolean state;

    public ExcelLoadState(boolean status) {
        state = status;
    }

    public boolean isTrue() {
        return state;
    }

}
