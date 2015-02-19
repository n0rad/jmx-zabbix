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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class JmxZabbixConfig {

    private JmxConfig jmx = new JmxConfig();
    private ZabbixConfig zabbix = new ZabbixConfig();

    private String serverName;
    private long pushIntervalSecond = 60;
    private Integer inMemoryMaxQueueSize = 5000;

    //////////////////////////////////////

    @Data
    public static class JmxConfig {
        private String url;
        private String username;
        private String password;

        private Map<String, List<String>> valuesCaptured = new HashMap<>();
        private Map<String, String> metrics = new HashMap<>();

        private Map<String, List<String[]>> preparedValuesCaptured;

        public Map<String, List<String[]>> preparedValuesCaptured() {
            if (preparedValuesCaptured == null) {
                preparedValuesCaptured = new HashMap<>();
                for (String key : valuesCaptured.keySet()) {
                    ArrayList<String[]> strings = new ArrayList<>();
                    for (String name : valuesCaptured.get(key)) {
                        strings.add(name.split("\\."));
                    }
                    preparedValuesCaptured.put(key, strings);
                }
            }
            return preparedValuesCaptured;
        }

    }

    @Data
    public static class ZabbixConfig {
        private String host;
        private Integer port = 10051;
        private int timeoutSecond = 5;
    }

}
