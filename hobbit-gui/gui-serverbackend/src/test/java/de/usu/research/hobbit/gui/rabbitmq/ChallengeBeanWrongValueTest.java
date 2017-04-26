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

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import de.usu.research.hobbit.gui.rest.beans.ChallengeBean;

/**
 * This JUnit tests handles the special case in which the flags inside the given
 * RDF model are replaced by either "0" or "1" values. Simulates the problem
 * described in {@link https://github.com/hobbit-project/platform/issues/34}.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class ChallengeBeanWrongValueTest extends AbstractRdfHelperTest {

    public ChallengeBeanWrongValueTest() {
        super("de/usu/research/hobbit/gui/rabbitmq/challenge.ttl",
                "de/usu/research/hobbit/gui/rabbitmq/challengeBeanContent.ttl");
    }

    @Override
    protected Model performTransformation(Model model) {
        List<Statement> stmts = new ArrayList<Statement>();
        // replace all "true" values with "1"
        StmtIterator iterator = model.listLiteralStatements(null, null, true);
        while (iterator.hasNext()) {
            stmts.add(iterator.next());
        }
        model.remove(stmts);
        for (Statement stmt : stmts) {
            model.addLiteral(stmt.getSubject(), stmt.getPredicate(), 1);
        }
        // replace all "false" values with "0"
        iterator = model.listLiteralStatements(null, null, false);
        while (iterator.hasNext()) {
            stmts.add(iterator.next());
        }
        model.remove(stmts);
        for (Statement stmt : stmts) {
            model.addLiteral(stmt.getSubject(), stmt.getPredicate(), 1);
        }

        ChallengeBean challenge = RdfModelHelper.getChallengeBean(model,
                model.getResource("http://example.org/MyChallenge"));
        Model resultModel = RdfModelCreationHelper.createNewModel();
        RdfModelCreationHelper.addChallenge(challenge, resultModel);
        return resultModel;
    }

}
