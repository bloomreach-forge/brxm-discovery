package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrispResourceServiceBrokerFactoryTest {

    @AfterEach
    void tearDown() {
        HstServices.setComponentManager(null);
    }

    @Test
    void getObject_returnsBrokerFromHstComponentManager() {
        ResourceServiceBroker broker = mock(ResourceServiceBroker.class);
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getComponent(ResourceServiceBroker.class.getName())).thenReturn(broker);
        HstServices.setComponentManager(componentManager);

        ResourceServiceBroker resolved = new CrispResourceServiceBrokerFactory().getObject();

        assertSame(broker, resolved);
    }

    @Test
    void getObject_withoutRegisteredBroker_throwsHelpfulConfigurationException() {
        HstServices.setComponentManager(mock(ComponentManager.class));

        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> new CrispResourceServiceBrokerFactory().getObject());

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("CRISP ResourceServiceBroker not found"));
    }
}
