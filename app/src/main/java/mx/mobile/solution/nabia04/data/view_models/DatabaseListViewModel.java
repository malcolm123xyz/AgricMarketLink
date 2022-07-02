package mx.mobile.solution.nabia04.data.view_models;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import mx.mobile.solution.nabia04.data.entities.EntityUserData;

public class DatabaseListViewModel extends ViewModel {

    private final MutableLiveData<List<EntityUserData>> userData;

    public DatabaseListViewModel() {
        userData = new MutableLiveData<>();
    }

    public void setData (List<EntityUserData> data){
        userData.setValue(data);
    }
    public LiveData<List<EntityUserData>> getData() {
        return userData;
    }

    public int getFilteredDataSize(){
        return userData.getValue().size();
    }
//    List<EntityAnnouncement> getAnnDataObjects(List<Announcement> list) {
//        List<EntityAnnouncement> entityAnn = new ArrayList<>();
//        for (Announcement ann : list) {
//            EntityAnnouncement entity = new EntityAnnouncement();
//            entity.setId(ann.getId());
//            entity.setHeading(ann.getHeading());
//            entity.setMessage(ann.getMessage());
//            entity.setType(ann.getType());
//            entity.setImageUri(ann.getImageUri());
//            entity.setAlarm(ann.getAlarm());
//            entity.setEventDate(ann.getEventDate());
//            entity.setPriority(ann.getPriority());
//            entity.setRowNum(ann.getRowNum());
//            entity.setVenue(ann.getVenue());
//            entity.setIsAboutWho(ann.getIsAboutWho());
//            entity.setArelative(ann.getArelative());
//            entityAnn.add(entity);
//        }
//        return entityAnn;
//    }
}