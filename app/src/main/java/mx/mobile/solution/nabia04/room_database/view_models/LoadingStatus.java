package mx.mobile.solution.nabia04.room_database.view_models;

public class LoadingStatus {

    private final boolean loading;

    public LoadingStatus(boolean status){
        loading = status;
    }

    public boolean isLoading() {
        return loading;
    }

}
