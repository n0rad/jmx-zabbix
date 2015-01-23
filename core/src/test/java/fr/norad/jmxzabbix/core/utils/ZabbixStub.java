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
package fr.norad.jmxzabbix.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import fr.norad.jmxzabbix.core.ZabbixProtocol;
import fr.norad.jmxzabbix.core.ZabbixRequest;
import fr.norad.jmxzabbix.core.ZabbixResponse;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class ZabbixStub {
    private final int port;
    private final int requestsToRead;
    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
    private final List<ZabbixRequest> requests = new ArrayList<>();
    private int waitBeforeReadSecond = 0;

    public ZabbixStub(int port, int requestsToRead) throws Exception {
        this.port = port;
        this.requestsToRead = requestsToRead;
    }

    ZabbixRequest asServerReadRequest(InputStream in) throws IOException {
        byte[] read = ZabbixProtocol.read(in);
        return mapper.readValue(read, ZabbixRequest.class);
    }

    void asServerWriteResponse(OutputStream out, ZabbixResponse response) throws IOException {
        ZabbixProtocol.write(out, mapper.writeValueAsBytes(response));
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            try (Socket clientSocket = serverSocket.accept();
                 OutputStream out = clientSocket.getOutputStream();
                 InputStream in = clientSocket.getInputStream()) {
                int i = 0;
                Thread.sleep(waitBeforeReadSecond * 1000);
                while (i++ < requestsToRead) {
                    requests.add(asServerReadRequest(in));
                    System.out.println("received : " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requests.get(requests.size() - 1)));
                    asServerWriteResponse(out, new ZabbixResponse("ok", "cool"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create socket on port " + port);
        }
    }
}
