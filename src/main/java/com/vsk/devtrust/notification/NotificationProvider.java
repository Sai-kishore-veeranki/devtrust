package com.vsk.devtrust.notification;

import com.vsk.devtrust.entity.IncidentEntity;

public interface NotificationProvider {

    NotificationChannel getChannel();

    boolean isEnabled();

    void sendAlert(IncidentEntity incident) throws Exception;
}
