// Copyright 2017 JanusGraph Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.janusgraph.diskstorage.hbase2;

import org.apache.hadoop.hbase.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseCompatLoader {

    private static final Logger log = LoggerFactory.getLogger(HBaseCompatLoader.class);

    private static final String DEFAULT_HBASE_COMPAT_VERSION = "1.2";

    private static final String HBASE_VERSION_2_STRING = "2.";

    private static final String DEFAULT_HBASE_COMPAT_CLASS_NAME =
        "org.janusgraph.diskstorage.hbase2.HBaseCompat1_1";

    private static final String[] HBASE_SUPPORTED_VERSIONS =
        new String[] { "0.98", "1.0", "1.1", "1.2", "1.3", "2.0" };

    private static HBaseCompat cachedCompat;

    public synchronized static HBaseCompat getCompat(String classOverride) {

        if (null != cachedCompat) {
            log.debug("Returning cached HBase compatibility layer: {}", cachedCompat);
            return cachedCompat;
        }

        HBaseCompat compat;
        String className = null;
        String classNameSource = null;

        if (null != classOverride) {
            className = classOverride;
            classNameSource = "from explicit configuration";
        } else {
            String hbaseVersion = VersionInfo.getVersion();
            for (String supportedVersion : HBASE_SUPPORTED_VERSIONS) {
                if (hbaseVersion.startsWith(supportedVersion + ".")) {
                    if (hbaseVersion.startsWith(HBASE_VERSION_2_STRING)) {
                        // All HBase 2.x maps to HBaseCompat1_1.
                        className = DEFAULT_HBASE_COMPAT_CLASS_NAME;
                    }
                    else {
                        className = "org.janusgraph.diskstorage.hbase2.HBaseCompat" + supportedVersion.replaceAll("\\.", "_");
                    }
                    classNameSource = "supporting runtime HBase version " + hbaseVersion;
                    break;
                }
            }
            if (null == className) {
                log.info("The HBase version {} is not explicitly supported by JanusGraph.  " +
                    "Loading JanusGraph's compatibility layer for its most recent supported HBase version ({})",
                    hbaseVersion, DEFAULT_HBASE_COMPAT_VERSION);
                className = DEFAULT_HBASE_COMPAT_CLASS_NAME;
                classNameSource = " by default";
            }
        }

        final String errTemplate = " when instantiating HBase compatibility class " + className;

        try {
            compat = (HBaseCompat)Class.forName(className).newInstance();
            log.info("Instantiated HBase compatibility layer {}: {}", classNameSource, compat.getClass().getCanonicalName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getClass().getSimpleName() + errTemplate, e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getClass().getSimpleName() + errTemplate, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getClass().getSimpleName() + errTemplate, e);
        }

        return cachedCompat = compat;
    }
}
