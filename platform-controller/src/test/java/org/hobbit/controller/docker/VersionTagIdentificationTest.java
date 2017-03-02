package org.hobbit.controller.docker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class VersionTagIdentificationTest {

    @Parameters
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> testConfigs = new ArrayList<Object[]>();

        testConfigs.add(new Object[] {"busybox", false});
        testConfigs.add(new Object[] {"busybox:latest", true});
        testConfigs.add(new Object[] {"busybox:buildroot-2013.08.1", true});
        testConfigs.add(new Object[] {"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system", false});
        testConfigs.add(new Object[] {"git.project-hobbit.eu:4567/gitadmin/platform-benchmark-system:latest", true});
        testConfigs.add(new Object[] {"jboss/keycloak:2.5.1.Final", true});
        
        return testConfigs;
    }

    private String imageName;
    private boolean expectedResult;

    public VersionTagIdentificationTest(String imageName, boolean expectedResult) {
        this.imageName = imageName;
        this.expectedResult = expectedResult;
    }

    @Test
    public void test() {
        Assert.assertEquals(expectedResult, ContainerManagerImpl.containsVersionTag(imageName));
    }
}
