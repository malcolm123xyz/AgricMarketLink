package mx.mobile.solution.nabia04.data.view_models;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.mobile.solution.nabia04.data.entities.EntityDues;

public class YearlyDuesViewModel extends ViewModel {
    private final MutableLiveData<List<EntityDues>> dues;

    public YearlyDuesViewModel() {
        dues = new MutableLiveData<>();
    }

    public void setData(List<EntityDues> data) {
        dues.setValue(data);
    }

    public LiveData<List<EntityDues>> getData() {
        return dues;
    }
}