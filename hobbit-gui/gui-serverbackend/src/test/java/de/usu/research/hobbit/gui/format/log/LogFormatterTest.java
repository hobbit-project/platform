package de.usu.research.hobbit.gui.format.log;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import junit.framework.Assert;

@RunWith(Parameterized.class)
public class LogFormatterTest {

    @Parameters
    public static List<Object[]> parameters() {
        List<Object[]> data = new ArrayList<>();

        String jsonLog = "[{\"_index\":\"logstash-2018.06.14\",\"_type\":\"other\",\"_source\":{\"image_name\":\"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest\",\"@timestamp\":\"2018-06-14T16:20:44.753Z\",\"container_name\":\"platform-benchmark-system-d85075cfcf79438d9297eee04d330c12.1.ovnxhbsl5830kw89bulhb06xg\",\"message\":\"PlatformBenchmarkingSystem started\",\"container_id\":\"30c6e798ed8ab2f58f92e0c77c5751e8bc8384a026b1dad44b9654f72c784f72\"},\"_id\":\"AWP_GWZZwRO_i8n6DRWC\",\"sort\":[1528993244753],\"_score\":null},{\"_index\":\"logstash-2018.06.14\",\"_type\":\"other\",\"_source\":{\"image_name\":\"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest\",\"@timestamp\":\"2018-06-14T16:20:43.857Z\",\"container_name\":\"platform-benchmark-system-86b9f288036f463f94ad9925aac2da4c.1.e4fva42vl8yhtrdjws1pq7f4y\",\"message\":\"PlatformBenchmarkingSystem started\",\"container_id\":\"a8d07f831a1c6be68e2d8720dc0465721c00a326c09be36e926c78b57dcbbabe\"},\"_id\":\"AWP_GWLYwRO_i8n6DRWB\",\"sort\":[1528993243857],\"_score\":null},{\"_index\":\"logstash-2018.06.14\",\"_type\":\"other\",\"_source\":{\"image_name\":\"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest\",\"@timestamp\":\"2018-06-14T16:20:32.263Z\",\"container_name\":\"platform-benchmark-system-cd3db0a3c95e49e7b41ce5c42aa9e70b.1.u42oyzkxzdtrfj5rjayd1vpu9\",\"message\":\"PlatformBenchmarkingSystem started\",\"container_id\":\"b07f03293ef4b4e5b60b67259e43b5227aa9df0d2c8304fef35857d0dda9f632\"},\"_id\":\"AWP_GTWOwRO_i8n6DRV1\",\"sort\":[1528993232263],\"_score\":null}]";
        JSONArray logData = new JSONArray(jsonLog);

        data.add(new Object[] { new JSONFormatter(), logData, jsonLog });

        String logFields[] = new String[] { "@timestamp", "image_name", "container_name", "message" };
        
        String csvLog = "\"@timestamp\",\"image_name\",\"container_name\",\"message\"\n"
                + "\"2018-06-14T16:20:44.753Z\",\"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest\",\"platform-benchmark-system-d85075cfcf79438d9297eee04d330c12.1.ovnxhbsl5830kw89bulhb06xg\",\"PlatformBenchmarkingSystem started\"\n"
                + "\"2018-06-14T16:20:43.857Z\",\"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest\",\"platform-benchmark-system-86b9f288036f463f94ad9925aac2da4c.1.e4fva42vl8yhtrdjws1pq7f4y\",\"PlatformBenchmarkingSystem started\"\n"
                + "\"2018-06-14T16:20:32.263Z\",\"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest\",\"platform-benchmark-system-cd3db0a3c95e49e7b41ce5c42aa9e70b.1.u42oyzkxzdtrfj5rjayd1vpu9\",\"PlatformBenchmarkingSystem started\"\n";
        data.add(new Object[] { new CSVFormatter(logFields), logData, csvLog });
        String txtLog = "@timestamp image_name container_name message\n"
                + "2018-06-14T16:20:44.753Z git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest platform-benchmark-system-d85075cfcf79438d9297eee04d330c12.1.ovnxhbsl5830kw89bulhb06xg PlatformBenchmarkingSystem started\n"
                + "2018-06-14T16:20:43.857Z git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest platform-benchmark-system-86b9f288036f463f94ad9925aac2da4c.1.e4fva42vl8yhtrdjws1pq7f4y PlatformBenchmarkingSystem started\n"
                + "2018-06-14T16:20:32.263Z git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest platform-benchmark-system-cd3db0a3c95e49e7b41ce5c42aa9e70b.1.u42oyzkxzdtrfj5rjayd1vpu9 PlatformBenchmarkingSystem started\n";
        data.add(new Object[] { new CSVFormatter(logFields, ' ', ' '), logData, txtLog });

        return data;
    }

    private LogFormatter formatter;
    private JSONArray logData;
    private String expectedLog;

    public LogFormatterTest(LogFormatter formatter, JSONArray logData, String expectedLog) {
        super();
        this.formatter = formatter;
        this.logData = logData;
        this.expectedLog = expectedLog;
    }

    @Test
    public void test() {
        Assert.assertEquals(expectedLog, formatter.format(logData));
    }
}
