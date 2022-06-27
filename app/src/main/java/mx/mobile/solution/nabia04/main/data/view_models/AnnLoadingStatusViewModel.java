package mx.mobile.solution.nabia04.main.data.view_models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AnnLoadingStatusViewModel extends ViewModel {
    private final MutableLiveData<State> loadingStatus;

    public AnnLoadingStatusViewModel() {
        loadingStatus = new MutableLiveData<>();
    }

    public void setValue(State value) {
        loadingStatus.setValue(value);
    }

    public LiveData<State> getValue() {
        return loadingStatus;
    }
}