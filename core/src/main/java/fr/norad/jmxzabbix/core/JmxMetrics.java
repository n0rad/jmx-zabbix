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
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import fr.norad.jmxzabbix.core.Config.JmxConfig;
import fr.norad.jmxzabbix.core.ZabbixRequest.ZabbixItem;

public class JmxMetrics {

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
                    readMbeanValues(request, config.getValuesCaptured().get(info.getClassName()), zabbixPrefix, jmxObject);
                }
            } catch (MalformedObjectNameException e) {
                e.printStackTrace(System.err);
            }
        }
        return request;
    }

    private void readMbeanValues(ZabbixRequest request, List<String> toCapture, String name, ObjectName gauge) throws Exception {
        for (String type : toCapture) {
            request.getData().add(new ZabbixItem<>(name + '[' + type + ']', connection.getAttribute(gauge, type), serverName));
        }
    }

}
