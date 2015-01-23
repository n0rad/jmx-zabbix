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


import com.google.common.collect.EvictingQueue;
import lombok.Data;

@Data
public class JmxToZabbixDaemon implements Runnable {

    private final Config config;
    private boolean interruptFlag = false;

    public JmxToZabbixDaemon(Config config) {
        this.config = config;
    }

    @Override
    public void run() {
        EvictingQueue<ZabbixRequest> queue = EvictingQueue.create(config.getInMemoryMaxQueueSize());
        while (!interruptFlag) {
            try {
                Thread.sleep(config.getPushIntervalSecond() * 1000);
                queue.add(new JmxMetrics(config.getJmx(), config.getServerName()).getMetrics());
                new ZabbixClient(config.getZabbix()).send(queue);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

    }
}
