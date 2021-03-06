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
package com.expedia.adaptivealerting.anomdetect.detect.outlier.algo.forecasting;

import com.expedia.adaptivealerting.anomdetect.detect.AnomalyLevel;
import com.expedia.adaptivealerting.anomdetect.detect.outlier.OutlierDetectorResult;
import com.expedia.adaptivealerting.anomdetect.detect.AnomalyType;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecast;
import com.expedia.adaptivealerting.anomdetect.forecast.interval.IntervalForecaster;
import com.expedia.adaptivealerting.anomdetect.forecast.point.PointForecast;
import com.expedia.adaptivealerting.anomdetect.forecast.point.PointForecaster;
import com.expedia.adaptivealerting.anomdetect.util.TestObjectMother;
import com.expedia.metrics.MetricData;
import com.expedia.metrics.MetricDefinition;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

public class ForecastingDetectorTest {
    private ForecastingDetector detectorUnderTest;

    @Mock
    private PointForecaster pointForecaster;

    @Mock
    private IntervalForecaster intervalForecaster;

    private UUID detectorUuid;
    private AnomalyType anomalyType;
    private boolean trusted;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.detectorUuid = UUID.randomUUID();
        this.anomalyType = AnomalyType.TWO_TAILED;
        this.trusted = true;
        this.detectorUnderTest =
                new ForecastingDetector(detectorUuid, pointForecaster, intervalForecaster, anomalyType, trusted, "test");
    }

    @Test
    public void testAccessors() {
        initDependencies(false);
        assertEquals(detectorUuid, detectorUnderTest.getUuid());
        assertEquals(pointForecaster, detectorUnderTest.getPointForecaster());
        assertEquals(intervalForecaster, detectorUnderTest.getIntervalForecaster());
        assertEquals(anomalyType, detectorUnderTest.getAnomalyType());
        assertEquals(trusted, detectorUnderTest.isTrusted());

        assertNotNull(detectorUnderTest.getName());
        assertEquals("test", detectorUnderTest.getName());
    }

    @Test
    public void testDetect() {
        initDependencies(false);
        val metricDef = TestObjectMother.metricDefinition();
        val metricData = new MetricData(metricDef, 100.0, Instant.now().getEpochSecond());
        val result = (OutlierDetectorResult) detectorUnderTest.detect(metricData);
        assertFalse(result.isWarmup());
        assertNotNull(result);
    }

    @Test
    public void testDetect_nullForecast() {
        initDependencies(false);
        val metricDef = new MetricDefinition("some-key");
        val metricData = new MetricData(metricDef, 100.0, Instant.now().getEpochSecond());

        when(pointForecaster.forecast(metricData)).thenReturn(null);

        val result = (OutlierDetectorResult) detectorUnderTest.detect(metricData);
        assertNotNull(result);
        assertFalse(result.isWarmup());
        assertEquals(AnomalyLevel.UNKNOWN, result.getAnomalyLevel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetect_nullMetricData() {
        detectorUnderTest.detect(null);
    }

    @Test
    public void testDetect_warmingUpForecast() {
        initDependencies(true);
        val metricDef = TestObjectMother.metricDefinition();
        val metricData = new MetricData(metricDef, 100.0, Instant.now().getEpochSecond());
        val result = (OutlierDetectorResult) detectorUnderTest.detect(metricData);
        assertTrue(result.isWarmup());
        assertNotNull(result);
    }

    private void initDependencies(boolean warmup) {
        when(pointForecaster.forecast(any(MetricData.class)))
                .thenReturn(new PointForecast(50.0, warmup));
        when(intervalForecaster.forecast(any(MetricData.class), anyDouble()))
                .thenReturn(new IntervalForecast(100.0, 90.0, 20.0, 10.0));
    }

}
