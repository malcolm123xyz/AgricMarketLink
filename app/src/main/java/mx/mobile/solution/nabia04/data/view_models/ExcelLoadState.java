package mx.mobile.solution.nabia04.data.view_models;

public class ExcelLoadState {

    private final boolean state;

    public ExcelLoadState(boolean status) {
        state = status;
    }

    public boolean isTrue() {
        return state;
    }

}
