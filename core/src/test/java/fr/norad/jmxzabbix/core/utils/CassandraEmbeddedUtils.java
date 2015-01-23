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

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.cassandra.config.Config.CommitLogSync;
import org.apache.cassandra.config.ConfigurationLoader;
import org.apache.cassandra.config.SeedProviderDef;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.service.CassandraDaemon;
import org.apache.cassandra.thrift.Cassandra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fr.norad.core.io.PortFinder;

public class CassandraEmbeddedUtils {

    public static class CassandraEmbedded extends Cassandra {

        private final Logger log = LoggerFactory.getLogger(getClass());

        private ExecutorService executor;
        private File home;

        public CassandraEmbedded() throws IOException {
            home = Files.createTempDirectory("Test-cassandra-zabbix").toFile();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    FileUtils.deleteRecursive(home);
                }
            });

        }

        public CassandraEmbedded start() {
            if (isAlreadyRunning()) {
                log.warn("Cassandra is already running, not starting new one");
                return this;
            }

            log.info("Starting Embedded Cassandra...");
            prepare();
            run();
            return this;
        }


        private void run() {
            final CountDownLatch startupLatch = new CountDownLatch(1);
            executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    CassandraDaemon cassandraDaemon = new CassandraDaemon();
                    cassandraDaemon.activate();
                    startupLatch.countDown();
                }
            });
            try {
                startupLatch.await(30, SECONDS);
            } catch (InterruptedException e) {
                log.error("Timeout starting Cassandra embedded", e);
                throw new IllegalStateException("Timeout starting Cassandra embedded", e);
            }
        }

        private void prepare() {
            CassandraEmbeddedConfigLoader.homeFolder = home;
            org.apache.cassandra.config.Config config = new CassandraEmbeddedConfigLoader().loadConfig();

            new File(config.data_file_directories[0]).mkdirs();
            new File(config.saved_caches_directory).mkdirs();
            new File(config.commitlog_directory).mkdirs();

            System.setProperty("cassandra.config.loader", CassandraEmbeddedConfigLoader.class.getName());
            System.setProperty("cassandra-foreground", "true");
        }


        private boolean isAlreadyRunning() {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                MBeanInfo mBeanInfo = mbs.getMBeanInfo(new ObjectName("org.apache.cassandra.db:type=StorageService"));
                if (mBeanInfo != null) {
                    return true;
                }
                return false;
            } catch (InstanceNotFoundException e) {
                return false;
            } catch (IntrospectionException | MalformedObjectNameException | ReflectionException e) {
                throw new IllegalStateException("Cannot check if cassandra is already running", e);
            }
        }

    }

    public static class CassandraEmbeddedConfigLoader implements ConfigurationLoader {

        private static org.apache.cassandra.config.Config config;
        protected static File homeFolder;

        @Override
        public synchronized org.apache.cassandra.config.Config loadConfig() {
            if (config == null) {
                config = defaultValues();
                // ports
                config.storage_port = PortFinder.randomAvailable();
                config.ssl_storage_port = PortFinder.randomAvailable();
                config.native_transport_port = PortFinder.randomAvailable();
                config.rpc_port = PortFinder.randomAvailable();


                // paths
                String absolutePath = homeFolder.getAbsolutePath();
                config.client_encryption_options.keystore = absolutePath + "/keystore";
                config.data_file_directories = new String[]{absolutePath + "/data"};
                config.commitlog_directory = absolutePath + "/commitlog";
                config.server_encryption_options.keystore = absolutePath + "/keystore";
                config.server_encryption_options.truststore = absolutePath + "/truststore";
                config.client_encryption_options.keystore = absolutePath + "/keystore";
                config.saved_caches_directory = absolutePath + "/saved_caches";
            }
            return config;
        }

        private org.apache.cassandra.config.Config defaultValues() {
            org.apache.cassandra.config.Config config = new org.apache.cassandra.config.Config();
            config.start_native_transport = true;
            config.commitlog_sync = CommitLogSync.periodic;
            config.commitlog_sync_period_in_ms = 10000;
            config.partitioner = org.apache.cassandra.dht.Murmur3Partitioner.class.getName();
            config.endpoint_snitch = "SimpleSnitch";
            LinkedHashMap<String, Object> p = new LinkedHashMap<>();
            p.put("class_name", org.apache.cassandra.locator.SimpleSeedProvider.class.getName());
            p.put("parameters", newArrayList(of("seeds", "127.0.0.1")));
            config.seed_provider = new SeedProviderDef(p);
            return config;
        }
    }
}
