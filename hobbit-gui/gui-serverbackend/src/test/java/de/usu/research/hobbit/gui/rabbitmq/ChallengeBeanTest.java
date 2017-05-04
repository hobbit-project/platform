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
package de.usu.research.hobbit.gui.rabbitmq;

import org.apache.jena.rdf.model.Model;

import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;

public class ChallengeBeanTest extends AbstractRdfHelperTest {

    public ChallengeBeanTest() {
        super("de/usu/research/hobbit/gui/rabbitmq/challenge.ttl",
                "de/usu/research/hobbit/gui/rabbitmq/challengeBeanContent.ttl");
    }

    @Override
    protected Model performTransformation(Model model) {
        ChallengeBean challenge = RdfModelHelper.getChallengeBean(model,
                model.getResource("http://example.org/MyChallenge"));
        Model resultModel = RdfModelCreationHelper.createNewModel();
        RdfModelCreationHelper.addChallenge(challenge, resultModel);
        return resultModel;
    }

}
