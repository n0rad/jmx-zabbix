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

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import fr.norad.jmxzabbix.core.utils.CassandraEmbeddedUtils.CassandraEmbedded;
import fr.norad.jmxzabbix.core.utils.JmxUtils;
import fr.norad.jmxzabbix.core.utils.ZabbixStub;

public class JmxToZabbixDaemonIT {


    private String jmxUrl = JmxUtils.startServerAndGetServiceURL();
    private JmxToZabbixDaemon daemon;
    private Thread jmxToZabbixThread;


    @Before
    public void before() throws Exception {
        // start cassandra
        new CassandraEmbedded().start();
        // config
        JmxZabbixConfig config = ConfigLoader.loadConfig(new File(JmxToZabbixDaemonIT.class.getResource("/jmx-zabbix.yaml").getPath()));
        config.getJmx().setUrl(jmxUrl);

        daemon = new JmxToZabbixDaemon(config);
        jmxToZabbixThread = new Thread(daemon);
    }

    @Test
    public void should_receive_request() throws Exception {
        // start daemon
        jmxToZabbixThread.start();

        // start zabbix stub
        ZabbixStub zabbixStub = new ZabbixStub(10051, 1);
        zabbixStub.run();

        daemon.setInterruptFlag(true);
        jmxToZabbixThread.interrupt();

        assertThat(zabbixStub.getRequests()).hasSize(1);
        assertThat(zabbixStub.getRequests().get(0)).isNotNull();
    }

    @Test
    public void should_read_2_request_when_zabbix_is_unreachable() throws Exception {
        // start daemon
        jmxToZabbixThread.start();

        // zabbix is unreachable for 2s
        Thread.sleep(2000);

        // start zabbix stub
        ZabbixStub zabbixStub = new ZabbixStub(10051, 2);
        zabbixStub.run();

        daemon.setInterruptFlag(true);
        jmxToZabbixThread.interrupt();

        assertThat(zabbixStub.getRequests()).hasSize(2);
        assertThat(zabbixStub.getRequests().get(0)).isNotNull();
        assertThat(zabbixStub.getRequests().get(1)).isNotNull();
    }

    @Test
    public void should_not_crash_daemon_and_resend_data_if_zabbix_server_is_stupid() throws Exception {
        // start daemon
        jmxToZabbixThread.start();

        // start zabbix stub which will timeout
        ZabbixStub zabbixStub = new ZabbixStub(10051, 1);
        zabbixStub.setWaitBeforeReadSecond(2);
        zabbixStub.run();

        assertThat(zabbixStub.getRequests()).hasSize(1);
        ZabbixRequest requestWithoutHack = zabbixStub.getRequests().get(0);
        assertThat(requestWithoutHack).isNotNull();

        System.out.println("restarting");
        // start zabbix stub
        ZabbixStub zabbixStub2 = new ZabbixStub(10051, 2);
        zabbixStub2.run();

        daemon.setInterruptFlag(true);
        jmxToZabbixThread.interrupt();

        assertThat(zabbixStub2.getRequests()).hasSize(2);
        assertThat(zabbixStub2.getRequests()).contains(requestWithoutHack);
    }
}