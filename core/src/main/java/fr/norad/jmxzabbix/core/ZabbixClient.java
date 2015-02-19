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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import fr.norad.jmxzabbix.core.JmxZabbixConfig.ZabbixConfig;

public class ZabbixClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZabbixClient.class);

    private final ZabbixConfig config;
    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);

    public ZabbixClient(ZabbixConfig config) {
        this.config = config;
    }

    public void send(EvictingQueue<ZabbixRequest> dataQueue) throws IOException {
        if (dataQueue.isEmpty()) {
            return;
        }
        try (Socket zabbix = new Socket(config.getHost(), config.getPort());
             OutputStream out = zabbix.getOutputStream();
             InputStream in = zabbix.getInputStream()) {

            zabbix.setSoTimeout(config.getTimeoutSecond() * 1000);

            ZabbixRequest current = null;
            while (!dataQueue.isEmpty()) {
                try {
                    current = dataQueue.poll();
                    LOGGER.debug("sending request" + current);
                    asClientWriteRequest(out, current);
                    out.flush();
                    LOGGER.debug("request sent");
                    LOGGER.debug("reading response");
                    ZabbixResponse zabbixResponse = asClientReadResponse(in);
                    LOGGER.debug("response read" + zabbixResponse);
                } catch (Exception e) {
                    if (current != null) {
                        dataQueue.add(current);
                    }
                    throw new IllegalStateException("Communication with Zabbix goes wrong", e);
                }
            }
        } catch (ConnectException e) {
            throw new IOException("Cannot connect to " + config.getHost() + ':' + config.getPort());
        }
    }

    private void asClientWriteRequest(OutputStream out, ZabbixRequest ZabbixRequest) throws IOException {
        ZabbixProtocol.write(out, mapper.writeValueAsBytes(ZabbixRequest));
    }

    private ZabbixResponse asClientReadResponse(InputStream in) throws IOException {
        return mapper.readValue(ZabbixProtocol.read(in), ZabbixResponse.class);
    }

}
