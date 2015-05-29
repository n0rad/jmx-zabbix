[![Build Status](https://travis-ci.org/n0rad/jmx-zabbix.png)](https://travis-ci.org/n0rad/jmx-zabbix)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/fr.norad.jmxzabbix/jmx-zabbix-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.norad.jmxzabbix/jmx-zabbix-core)

# DESCRIPTION:

Service that load jmx metrics periodically and push to zabbix in one shot

# REQUIREMENTS:

* a jmx server to connect to
* a zabbix server to connect to
* java >= 7

# ATTRIBUTES:

see configuration file example core/src/test/resources/jmx-zabbix.yaml

# Build

You need java and maven

``
$ mvn clean verify
``

# USAGE:

in cli :

``
$ java -jar jmx-zabbix.jar config.yaml
``

Or you can include the core library directly in your application : 

```
import java.io.File;
import java.io.FileInputStream;
import org.yaml.snakeyaml.Yaml;
import fr.norad.jmxzabbix.core.JmxToZabbixDaemon;
import fr.norad.jmxzabbix.core.JmxZabbixConfig;

public class Main {

    public static void main(String[] args) throws Exception {
        File configFile = new File("/my/configuration/file.yaml");
        Yaml yaml = new Yaml();
        try (FileInputStream input = new FileInputStream(configFile)) {
            JmxZabbixConfig config = yaml.loadAs(input, JmxZabbixConfig.class);
            JmxToZabbixDaemon jmxToZabbixDaemon = new JmxToZabbixDaemon(config);
            Thread thread = new Thread(jmxToZabbixDaemon);
            thread.setName("jmxzabbix");
            thread.start();
        }

    }

}
```

