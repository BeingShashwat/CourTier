package com.courtier.courtier.notification.dispatcher;

import com.courtier.courtier.polling.CaseUpdatedEvent;

public interface NotificationDispatcher {

    void dispatch(CaseUpdatedEvent event) throws Exception;

}