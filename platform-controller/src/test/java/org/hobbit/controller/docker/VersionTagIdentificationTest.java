/**
 * This file is part of platform-controller.
 *
 * platform-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * platform-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with platform-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
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
