package mx.mobile.solution.nabia04.room_database.view_models;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.mobile.solution.nabia04.room_database.entities.EntityAnnouncement;

public class AnnViewModel extends ViewModel {
    private final MutableLiveData<List<EntityAnnouncement>> announcementMutableObj;
    public AnnViewModel() {announcementMutableObj = new MutableLiveData<>();}
    public void setData (List<EntityAnnouncement> data){
        announcementMutableObj.setValue(data);
    }
    public LiveData<List<EntityAnnouncement>> getData() {
        return announcementMutableObj;
    }
}