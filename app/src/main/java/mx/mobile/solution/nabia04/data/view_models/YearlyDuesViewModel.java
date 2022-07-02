package mx.mobile.solution.nabia04.data.view_models;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.mobile.solution.nabia04.data.entities.EntityYearlyDues;

public class YearlyDuesViewModel extends ViewModel {
    private final MutableLiveData<List<EntityYearlyDues>> dues;
    public YearlyDuesViewModel() {
        dues = new MutableLiveData<>();
    }
    public void setData (List<EntityYearlyDues> data){
        dues.setValue(data);
    }
    public LiveData<List<EntityYearlyDues>> getData() {
        return dues;
    }
}