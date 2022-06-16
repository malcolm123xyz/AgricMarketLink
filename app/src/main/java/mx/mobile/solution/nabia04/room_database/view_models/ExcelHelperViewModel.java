package mx.mobile.solution.nabia04.room_database.view_models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.mobile.solution.nabia04.main.fragment.welfare.ExcelHelper;

public class ExcelHelperViewModel extends ViewModel {
    private final MutableLiveData<ExcelHelper> loadingStatus;

    public ExcelHelperViewModel() {
        loadingStatus = new MutableLiveData<>();
    }

    public void setValue(ExcelHelper value) {
        loadingStatus.setValue(value);
    }

    public LiveData<ExcelHelper> getValue() {
        return loadingStatus;
    }
}