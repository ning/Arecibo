package com.ning.arecibo.alert.objects;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.enums.NotificationType;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.email.EmailManager;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;



public class NotifConfig extends ConfDataNotifConfig implements ConfigurableObject
{
    private final static Logger log = Logger.getLogger(NotifConfig.class);

    private final ConcurrentSkipListSet<NotifMapping> notifMappings;

    private volatile Person person;
    private volatile EmailManager emailManager;

    public NotifConfig() {

        this.notifMappings = new ConcurrentSkipListSet<NotifMapping>(ConfigurableObjectComparator.getInstance());
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);

        if(notifMappings.size() > 0) {
            sb.append("    linked notifMapping ids:\n");
            for(NotifMapping notifMapping : notifMappings) {
                sb.append(String.format("         %s\n", notifMapping.getLabel()));
            }
        }
    }

    @Override
    public synchronized boolean isValid(ConfigManager confManager) {
        // make sure our person exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getPerson(this.personId), confManager))
            return false;

        return true;
    }

    @Override
    public synchronized boolean configure(ConfigManager confManager,AlertManager alertManager, LoggingManager loggingManager) {

        this.emailManager = confManager.getEmailManager();

        this.person = confManager.getPerson(this.personId);
        if (!ConfigurableObjectUtils.checkNonNullAndLog(this.person, this.personId, "person", confManager))
            return false;
        
        this.person.addNotifConfig(this);
        
        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager,AlertManager alertManager) {

        if (this.person != null) {
            this.person.removeNotifConfig(this);
            this.person = null;
        }

        for(NotifMapping notifMapping : notifMappings) {
            notifMapping.unconfigure(confManager,alertManager);
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager,AlertManager alertManager, ConfigurableObject newConfig) {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }

    public void addNotificationMapping(NotifMapping notifMapping) {
        if(!this.notifMappings.contains(notifMapping))
            this.notifMappings.add(notifMapping);
    }

    public void removeNotificationMapping(NotifMapping notifMapping) {
        this.notifMappings.remove(notifMapping);
    }

    public Set<NotifMapping> getNotificationMappings() {
        return this.notifMappings;
    }

    public boolean sendNotification(Notification notification) {

        boolean addSignatureOk = false;
        String subject = notification.getSubject();
        String msg = notification.getMessage();

        if(this.getNotifType().equals(NotificationType.REGULAR_EMAIL)) {
            addSignatureOk = true;
        }
        else if(this.getNotifType().equals(NotificationType.SMS_VIA_EMAIL)) {
            // send only the subject, limit to 140 characters
            if(subject.length() > EmailManager.MAX_CHARS_FOR_SMS)
                subject = subject.substring(0,EmailManager.MAX_CHARS_FOR_SMS-1);

            // send subject as message body
            msg = subject;
        }

        log.debug("sending message to <%s>", this.address);
        log.debug("notification type = %s", this.getNotifType().toString());
        log.debug("Subject: %s", subject);
        log.debug("Description:\n%s", msg);

        boolean success = this.emailManager.sendEmail(this.address, emailManager.getFrom(), subject, msg, addSignatureOk);

        /*
        ** TODO: update notification log
         */


        return success;
    }
}
