package de.usu.research.hobbit.gui.rabbitmq;

import org.apache.jena.rdf.model.Model;

import de.usu.research.hobbit.gui.rest.ChallengeBean;

public class ChallengeBeanTest extends AbstractRdfHelperTest {

    public ChallengeBeanTest() {
        super("de/usu/research/hobbit/gui/rabbitmq/closedChallengeConfig.ttl",
                "de/usu/research/hobbit/gui/rabbitmq/closedChallengeConfig.ttl");
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
