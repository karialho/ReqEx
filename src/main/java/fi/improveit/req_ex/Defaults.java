/*
 * Copyright 2021. ImproveIt Oy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fi.improveit.req_ex;

import java.util.Properties;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Defaults {

    private static Properties prop;
    private static final String DEFAULTS_PATH = System.getProperty("user.home") + File.separator + ".reqex";
    protected static final Logger logger = LoggerFactory.getLogger(Defaults.class.getName());

    static String get(String key) {
        return prop.getProperty(key);
    }

    static void set(String key, String value) {
        prop.setProperty(key, value);
    }

    static void load() {
        try {
            prop = new Properties();
            FileInputStream in = new FileInputStream(DEFAULTS_PATH);
            prop.load(in);
            PrintWriter writer = new PrintWriter(System.out);
            prop.list(writer);
            writer.flush();
        } catch (IOException e) {
            logger.warn("Could not read defaults file", e);
        }
    }

    static void store() {
        try {
            FileOutputStream out = new FileOutputStream(DEFAULTS_PATH);
            prop.store(out, "ReqEx Application Defaults File");
        } catch (IOException e) {
            logger.error("Could not store defaults file", e);
        }
    }

}
