# DESCRIPTION:

Service that load jmx metrics periodically and push to zabbix in one shot

# REQUIREMENTS:

* a jmx server to connect to
* a zabbix server to connect to
* java >= 7

# ATTRIBUTES:

see configuration file example cli/src/dev/conf/jmx-zabbix.yaml

# USAGE:

in cli :

``
$ java -jar jmx-zabbix.jar config.yaml
``
