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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.junit.Test;
import fr.norad.jmxzabbix.core.JmxZabbixConfig.JmxConfig;
import fr.norad.jmxzabbix.core.utils.CassandraEmbeddedUtils.CassandraEmbedded;

public class JmxMetricsTest {


    public String jmxServer() throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, platformMBeanServer);
        cs.start();
        return cs.getAddress().toString();
    }

    @Test
    public void should_find_metrics_in_jmx() throws Exception {
        new CassandraEmbedded().start();
        JmxConfig config = new JmxConfig();
        config.setUrl(jmxServer());
        config.setPassword("");
        config.setUsername("");
        config.getValuesCaptured().put("com.yammer.metrics.reporting.JmxReporter$Meter", asList("Count", "MeanRate", "OneMinuteRate"));
        config.getMetrics().put("drop", "org.apache.cassandra.metrics:type=DroppedMessage,scope=BINARY,name=Dropped");

        JmxMetrics jmxMetrics = new JmxMetrics(config, "superServer");
        ZabbixRequest metrics = jmxMetrics.getMetrics();

        assertThat(metrics.getData()).hasSize(3);
        assertThat(metrics.getData().get(0).getKey()).isEqualTo("drop[Count]");
        assertThat(metrics.getData().get(0).getValue()).isEqualTo(0L);
        assertThat(metrics.getData().get(0).getHost()).isEqualTo("superServer");
    }

    @Test
    public void should_support_embedded_object() throws Exception {
        new CassandraEmbedded().start();
        JmxConfig config = new JmxConfig();
        config.setUrl(jmxServer());
        config.setPassword("");
        config.setUsername("");
        config.getValuesCaptured().put("sun.management.MemoryImpl", asList("HeapMemoryUsage.max", "NonHeapMemoryUsage.used"));
        config.getMetrics().put("mem", "java.lang:type=Memory");

        JmxMetrics jmxMetrics = new JmxMetrics(config, "superServer");
        ZabbixRequest metrics = jmxMetrics.getMetrics();

        assertThat(metrics.getData()).hasSize(2);
        assertThat(metrics.getData().get(0).getKey()).isEqualTo("mem[HeapMemoryUsage.max]");
        assertThat((long) metrics.getData().get(0).getValue()).isGreaterThan(10000);
        assertThat(metrics.getData().get(0).getHost()).isEqualTo("superServer");
    }

}