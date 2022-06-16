package mx.mobile.solution.nabia04.room_database.view_models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DBLoadingStatusViewModel extends ViewModel {
    private final MutableLiveData<LoadingStatus> loadingStatus;
    public DBLoadingStatusViewModel() {loadingStatus = new MutableLiveData<>();}
    public void setValue(LoadingStatus value){
        loadingStatus.setValue(value);
    }
    public LiveData<LoadingStatus> getValue() {return loadingStatus;}
}