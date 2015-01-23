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


import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * it's thread safe
 */
public class ZabbixProtocol {

    private static  final Logger LOGGER = LoggerFactory.getLogger(ZabbixProtocol.class);

    public static void write(OutputStream out, byte[] bytes) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Writing bytes : " + new String(bytes));
        }
        out.write(new byte[]{
                'Z', 'B', 'X', 'D', '\1',
                (byte) (bytes.length & 0xFF),
                (byte) ((bytes.length >> 8) & 0x00FF),
                (byte) ((bytes.length >> 16) & 0x0000FF),
                (byte) ((bytes.length >> 24) & 0x000000FF),
                '\0', '\0', '\0', '\0'});
        out.write(bytes);
    }

    public static byte[] read(InputStream in) throws IOException {
        LOGGER.debug("Try to read bytes");
        DataInputStream din = new DataInputStream(in);
        byte[] headerBuffer = new byte[13];
        din.readFully(headerBuffer);

        if (headerBuffer[0] != 'Z' || headerBuffer[1] != 'B' || headerBuffer[2] != 'X' || headerBuffer[3] != 'D' || headerBuffer[4] != '\1'
                || headerBuffer[9] != '\0' || headerBuffer[10] != '\0' || headerBuffer[11] != '\0' || headerBuffer[12] != '\0') {
            throw new IllegalStateException("Wrong header received");
        }

        int dataLength = 0;
        dataLength |= headerBuffer[5] & 0xFF;
        dataLength |= (headerBuffer[6] & 0xFF) << 8;
        dataLength |= (headerBuffer[7] & 0xFF) << 16;
        dataLength |= (headerBuffer[8] & 0xFF) << 24;

        if (dataLength > 128_000_000) {
            throw new IllegalStateException("Zabbix buffer does not support > 128 Mb. Something is wrong");
        }

        byte[] bytes = new byte[dataLength];
        din.readFully(bytes);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bytes read : " + new String(bytes));
        }
        return bytes;
    }
}
