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
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

public class JmxZabbix {

    private static String jmxZabbixVersion;

    public static final String JMXZABBIX_VERSION_KEY = "jmxzabbix.version";
    public static final String JMXZABBIX_CHECK_KEY = "jmxzabbix.check";

    public static final int JMXZABBIX_CHECK_SUCCESS = 0;
    public static final int JMXZABBIX_CHECK_JMXZABBIX_ERROR = 1;
    public static final int JMXZABBIX_CHECK_JMX_CONNECTION_ERROR = 2;
    public static final int JMXZABBIX_CHECK_FORMAT_ERROR = 3;
    public static final int JMXZABBIX_CHECK_ZABBIX_CONNECTION_ERROR = 4;

    public static String getJmxZabbixVersion() {
        if (jmxZabbixVersion == null) {
            try {
                Enumeration<URL> resources = JmxZabbix.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (resources.hasMoreElements()) {
                    try {
                        Manifest manifest = new Manifest(resources.nextElement().openStream());
                        jmxZabbixVersion = manifest.getMainAttributes().getValue("jmxzabbix-version");
                        if (jmxZabbixVersion != null) {
                            return jmxZabbixVersion;
                        }
                    } catch (IOException E) {
                        // nothing to do
                    }
                }
            } catch (IOException e) {
                // nothing to do
            }
            jmxZabbixVersion = "UNKNOWN";
        }
        return jmxZabbixVersion;
    }
}
