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
package fr.norad.jmxzabbix.cli;

import java.io.File;
import fr.norad.jmxzabbix.core.Config;
import fr.norad.jmxzabbix.core.ConfigLoader;
import fr.norad.jmxzabbix.core.JmxToZabbixDaemon;

public class Main {

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                throw new IllegalArgumentException("Usage: jmx-zabbix config.yaml");
            }
            Config config = ConfigLoader.loadConfig(new File(args[0]));
            new Thread(new JmxToZabbixDaemon(config)).start();
        } catch (Exception e) {
              e.printStackTrace(System.err);
            System.exit(1);
        }
    }

}
