/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.adaptivealerting.metrics.functions;

import com.expedia.adaptivealerting.metrics.functions.service.graphite.GraphiteQueryService;
import com.expedia.adaptivealerting.metrics.functions.sink.MetricFunctionsPublish;
import com.expedia.adaptivealerting.metrics.functions.source.MetricFunctionsSpec;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static com.expedia.adaptivealerting.anomdetect.util.AssertUtil.notNull;

@Slf4j
public class MetricFunctionsTask implements Runnable {
    private Config metricStoreConfig;
    private MetricFunctionsSpec spec;
    private MetricFunctionsPublish publisher;
    private GraphiteQueryService graphiteQueryService;

    public MetricFunctionsTask(Config metricStoreConfig, MetricFunctionsSpec spec, MetricFunctionsPublish publisher) {
        notNull(metricStoreConfig, "metricStoreConfig can't be null");
        notNull(spec, "spec can't be null");
        notNull(publisher, "publisher can't be null");

        this.metricStoreConfig = metricStoreConfig;
        this.spec = spec;
        this.publisher = publisher;
        this.graphiteQueryService = new GraphiteQueryService();
    }

    public void run() {
        try {
            val metricData = graphiteQueryService.queryMetricSource(metricStoreConfig, spec);
            publisher.publishMetrics(metricData);
        }
        catch (Exception e) {
            log.error("Exception while processing metrics function", e);

        }
    }

}