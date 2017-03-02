/**
 * This file is part of gui-serverbackend.
 *
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.hobbit.core.Constants;
import org.hobbit.storage.client.StorageServiceClient;
import org.hobbit.storage.queries.SparqlQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.usu.research.hobbit.gui.rabbitmq.RabbitMQConnectionSingleton;
import de.usu.research.hobbit.gui.rabbitmq.RdfModelHelper;
import de.usu.research.hobbit.gui.rabbitmq.StorageServiceClientSingleton;
import de.usu.research.hobbit.gui.rest.beans.ExperimentBean;

public class TestClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestClass.class);

    public static void main(String[] args) {
        System.out.println("creating client");
        StorageServiceClient client = StorageServiceClientSingleton.getInstance();

        try {
            System.out.println("sending request");
            
            String experimentUri = Constants.EXPERIMENT_URI_NS + "1487768956587";
            String query = SparqlQueries.getExperimentGraphQuery(experimentUri, null);
            // TODO make sure that the user is allowed to see the
            // experiment!
            Model model = StorageServiceClientSingleton.getInstance().sendConstructQuery(query);
            System.out.println(model.toString());
            ExperimentBean eb = RdfModelHelper.createExperimentBean(model, model.getResource(experimentUri));
            System.out.println(eb.toString());
        } finally {
            IOUtils.closeQuietly(client);
            try {
                RabbitMQConnectionSingleton.getConnection().close();
            } catch (Exception e) {
            }
        }
    }
}
