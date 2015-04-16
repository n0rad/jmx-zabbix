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


import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import fr.norad.jmxzabbix.core.JmxZabbixConfig.JmxConfig;
import fr.norad.jmxzabbix.core.ZabbixRequest.ZabbixItem;

public class JmxMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(JmxMetrics.class);

    private final JmxConfig config;
    private final String serverName;

    public JmxMetrics(JmxConfig config, String serverName) throws Exception {
        this.config = config;
        this.serverName = serverName;
    }

    public ZabbixRequest getMetrics() throws Exception {
        if (Strings.isNullOrEmpty(config.getUrl())) {
            return getMetrics(new MbeanServer(getPlatformMBeanServer()));
        } else {
            JMXServiceURL jmxUrl = new JMXServiceURL(config.getUrl());
            Map<String, Object> env = new HashMap<>();
            String[] creds = {config.getUsername(), config.getPassword()};
            env.put(JMXConnector.CREDENTIALS, creds);

            try (JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl, env)) {
                MBeanServerConnection conn = jmxc.getMBeanServerConnection();
                return getMetrics(new MbeanServer(conn));
            } catch (ConnectException e) {
                LOG.warn("Cannot connect to jmx server to get metrics : " + config.getUrl(), e);
            }
        }
        return null;
    }

    private ZabbixRequest getMetrics(MbeanServer server) {
        ZabbixRequest request = new ZabbixRequest();
        for (String zabbixPrefix : config.getMetrics().keySet()) {
            try {
                ObjectName jmxObject = new ObjectName(config.getMetrics().get(zabbixPrefix));
                MBeanInfo info = server.getMBeanInfo(jmxObject);
                if (config.getValuesCaptured().containsKey(info.getClassName())) {
                    readMbeanValues(server, request, config.getValuesCaptured().get(info.getClassName()),
                            config.preparedValuesCaptured().get(info.getClassName()), zabbixPrefix, jmxObject);
                }
            } catch (Exception e) {
//                LOG.warn("cannot read metrics for prefix : " + zabbixPrefix, e);
            }
        }
        return request;
    }


    private void readMbeanValues(MbeanServer conn, ZabbixRequest request, List<String> root,
                                 List<String[]> toCapture, String name, ObjectName gauge) throws Exception {
        for (int i = 0; i < toCapture.size(); i++) {
            String[] types = toCapture.get(i);
            String type = root.get(i);
            try {
                Object o = conn.getAttribute(gauge, types[0]);
                request.getData().add(new ZabbixItem<>(name + '[' + type + ']', processObject(o, types, 0), serverName));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read metric for capture name : " + type, e);
            }
        }
    }

    private Object processObject(Object o, String[] type, int pos) {
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

    public class MbeanServer {
        private final MBeanServer localServer;
        private final MBeanServerConnection remoteServer;

        public MbeanServer(MBeanServer server) {
            this.localServer = server;
            this.remoteServer = null;
        }

        public MbeanServer(MBeanServerConnection server) {
            this.remoteServer = server;
            this.localServer = null;
        }

        public MBeanInfo getMBeanInfo(ObjectName jmxObject) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
            if (localServer == null) {
                return remoteServer.getMBeanInfo(jmxObject);
            } else {
                return localServer.getMBeanInfo(jmxObject);
            }
        }

        public Object getAttribute(ObjectName gauge, String type) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException {
            if (localServer == null) {
                return remoteServer.getAttribute(gauge, type);
            } else {
                return localServer.getAttribute(gauge, type);
            }
        }
    }
}
