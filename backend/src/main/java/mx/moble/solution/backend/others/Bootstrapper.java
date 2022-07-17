package mx.moble.solution.backend.others;

import com.googlecode.objectify.ObjectifyService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import mx.moble.solution.backend.dataModel.Announcement;
import mx.moble.solution.backend.dataModel.ContributionData;
import mx.moble.solution.backend.dataModel.DatabaseObject;
import mx.moble.solution.backend.dataModel.LoginData;
import mx.moble.solution.backend.dataModel.RegistrationToken;

public class Bootstrapper implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        ObjectifyService.init();
        ObjectifyService.register(LoginData.class);
        ObjectifyService.register(DatabaseObject.class);
        ObjectifyService.register(RegistrationToken.class);
        ObjectifyService.register(Announcement.class);
        ObjectifyService.register(ContributionData.class);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
