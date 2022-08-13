package mx.moble.solution.backend.others

import com.googlecode.objectify.ObjectifyService
import mx.moble.solution.backend.model.*
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

class Bootstrapper : ServletContextListener {
    override fun contextInitialized(event: ServletContextEvent) {
        ObjectifyService.init()
        ObjectifyService.register(LoginData::class.java)
        ObjectifyService.register(DatabaseObject::class.java)
        ObjectifyService.register(RegistrationToken::class.java)
        ObjectifyService.register(Announcement::class.java)
        ObjectifyService.register(ContributionData::class.java)
        ObjectifyService.register(DuesBackup::class.java)
    }

    override fun contextDestroyed(servletContextEvent: ServletContextEvent) {}
}