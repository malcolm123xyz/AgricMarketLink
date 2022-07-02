package mx.mobile.solution.nabia04.data.view_models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DuesLoadingStatusViewModel extends ViewModel {
    private final MutableLiveData<State> loadingStatus;

    public DuesLoadingStatusViewModel() {
        loadingStatus = new MutableLiveData<>();
    }

    public void setValue(State value) {
        loadingStatus.setValue(value);
    }

    public LiveData<State> getValue() {
        return loadingStatus;
    }
}