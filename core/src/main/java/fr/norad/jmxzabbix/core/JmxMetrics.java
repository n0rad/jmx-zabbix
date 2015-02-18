/**
 *
 *     Copyright (C) norad.fr
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package fr.norad.jmxzabbix.core;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.norad.jmxzabbix.core.Config.JmxConfig;
import fr.norad.jmxzabbix.core.ZabbixRequest.ZabbixItem;

public class JmxMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(JmxMetrics.class);

    private final JmxConfig config;
    private final String serverName;
    private final MBeanServerConnection connection;

    public JmxMetrics(JmxConfig config, String serverName) throws Exception {
        this.config = config;
        this.serverName = serverName;
        JMXServiceURL jmxUrl = new JMXServiceURL(config.getUrl());
        Map<String, Object> env = new HashMap<>();
        String[] creds = {config.getUsername(), config.getPassword()};
        env.put(JMXConnector.CREDENTIALS, creds);
        JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, env);
        connection = jmxc.getMBeanServerConnection();
    }

    public ZabbixRequest getMetrics() throws Exception {
        ZabbixRequest request = new ZabbixRequest();

        for (String zabbixPrefix : config.getMetrics().keySet()) {
            try {
                ObjectName jmxObject = new ObjectName(config.getMetrics().get(zabbixPrefix));
                MBeanInfo info = connection.getMBeanInfo(jmxObject);
                if (config.getValuesCaptured().containsKey(info.getClassName())) {
                    readMbeanValues(request, config.getValuesCaptured().get(info.getClassName()),
                            config.preparedValuesCaptured().get(info.getClassName()), zabbixPrefix, jmxObject);
                }
            } catch (Exception e) {
                LOG.warn("cannot read metrics for prefix : " + zabbixPrefix, e);
            }
        }
        return request;
    }

    private void readMbeanValues(ZabbixRequest request, List<String> root, List<String[]> toCapture, String name, ObjectName gauge) throws Exception {
        for (int i = 0; i < toCapture.size(); i++) {
            String[] types = toCapture.get(i);
            String type = root.get(i);
            try {
                Object o = connection.getAttribute(gauge, types[0]);
                request.getData().add(new ZabbixItem<>(name + '[' + type +']', processObject(o, types, 0), serverName));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read metric for capture name : " + type, e);
            }
        }
    }

    public Object processObject(Object o, String[] type, int pos) {
        if (type.length - pos <= 1) {
            return o;
        }

        if (o.getClass().isAssignableFrom(CompositeDataSupport.class)) {
            return processObject(((CompositeDataSupport) o).get(type[pos + 1]), type, pos + 1);
        } else if (o.getClass().isAssignableFrom(Map.class)) {
            return processObject(((Map) o).get(type[1]), type, pos + 1);
        } //TODO process beans with getXXX ?
        return o;
    }

}
