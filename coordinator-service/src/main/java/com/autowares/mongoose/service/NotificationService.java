package com.autowares.mongoose.service;

import org.springframework.stereotype.Component;

import com.autowares.notification.client.NotificationClient;
import com.autowares.servicescommon.client.DiscoverService;

@Component
@DiscoverService(name="notification", path="/notification/v2")
public class NotificationService extends NotificationClient {


}
