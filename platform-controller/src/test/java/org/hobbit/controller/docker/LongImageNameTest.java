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

import static org.junit.Assert.assertNotNull;

import org.hobbit.core.Constants;
import org.junit.Test;


public class LongImageNameTest extends ContainerManagerBasedTest {

    private static final String imageName = "hobbitproject/nonexisting_image_name_thats_too_long";

    @Test
    public void startContainer() throws Exception {
        String id = manager.startContainer(imageName, Constants.CONTAINER_TYPE_SYSTEM, null);
        assertNotNull("ID of started container is not null", id);
        services.add(id);
    }

}
