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
package org.hobbit.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DockerBasedTestTest extends DockerBasedTest {

    private static final String ipv4 = "123.45.6.7";

    @Test
    public void getDockerHostTest() throws Exception {
        assertEquals("Should be localhost when not set", "localhost", getDockerHost(null));
        assertEquals("Should be as is when just the host is provided", ipv4, getDockerHost(ipv4));
        assertEquals("Should be just the host part when full URL is provided", ipv4, getDockerHost(String.format("tcp://%s:2376", ipv4)));
    }

}
