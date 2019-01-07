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
package org.hobbit.controller.data;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;
import org.hobbit.vocab.HobbitHardware;

/**
 * This class is used to store information about hardware the experiment runs on.
 *
 * @author Denis Kuchelev
 *
 */
public class SetupHardwareInformation {

    /**
     * Node hardware information.
     */
    private List<NodeHardwareInformation> nodes = new ArrayList<NodeHardwareInformation>();

    public void addNode(NodeHardwareInformation nodeInfo) {
        nodes.add(nodeInfo);
    }

    private String hash() {
        Model dummyModel = ModelFactory.createDefaultModel();
        Resource dummyRes = dummyModel.createResource(RdfHelper.HASH_SELF_URI);
        return RdfHelper.hashProperties(distinguishingProperties(dummyModel, dummyRes));
    }

    public String getURI() {
        return HobbitHardware.getClusterURI(hash());
    }

    public Resource addToModel(Model model) {
        Resource res = model.createResource(getURI(), HOBBIT.Hardware);
        model.add(distinguishingProperties(model, res));
        return res;
    }

    private StmtIterator distinguishingProperties(Model model, Resource self) {
        return new StmtIteratorImpl(nodes.stream().map(
                nodeInfo -> (Statement) new StatementImpl(
                        self, HOBBIT.comprises, nodeInfo.addToModel(model))
        ).iterator());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SetupHardwareInformation ").append(nodes);
        return builder.toString();
    }
}
