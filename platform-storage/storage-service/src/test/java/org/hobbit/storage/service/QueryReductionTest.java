package org.hobbit.storage.service;

import org.junit.Assert;
import org.junit.Test;

public class QueryReductionTest {

    @Test
    public void test(){
        String query = "PREFIX hobbit: <http://w3id.org/hobbit/vocab#>"
                + "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
                + "\n\nCONSTRUCT {"
                + "      ?v0 a hobbit:Challenge ."
                + "      ?v0 ?challengeProp ?challengeObj ."
                + "      ?challengeTask a hobbit:ChallengeTask ."
                + "      ?challengeTask ?taskProp ?taskObj ."
                + "      ?challengeTask hobbit:isTaskOf ?v0 ."
                + "      ?challengeTask hobbit:involvesBenchmark ?benchmark ."
                + "      ?challengeTask hobbit:involvesSystemInstance ?system ."
                + "      ?challengeTask ?parameterProp ?parameterValue ."
                + "      ?benchmark hobbit:hasParameter ?parameterProp ."
                + "}"
                + "WHERE {"
                + "  GRAPH <http://hobbit.org/graphs/ChallengeDefinitions> {"
                + "    ?v0 a hobbit:Challenge ."
                + "      ?v0 ?challengeProp ?challengeObj ."
                + "      OPTIONAL {"
                + "              ?challengeTask a hobbit:ChallengeTask ."
                + "              ?challengeTask ?taskProp ?taskObj ."
                + "              ?challengeTask hobbit:isTaskOf ?v0 ."
                + "              ?challengeTask hobbit:involvesBenchmark ?benchmark ."
                + "              OPTIONAL {"
                + "                      ?challengeTask hobbit:involvesSystemInstance ?system ."
                + "              }"
                + "              OPTIONAL {"
                + "                      ?challengeTask ?parameterProp ?parameterValue ."
                + "                      ?benchmark hobbit:hasParameter ?parameterProp ."
                + "                      {?parameterProp a hobbit:Parameter} UNION {?parameterProp a hobbit:ConfigurableParameter} UNION {?parameterProp a hobbit:FeatureParameter}."
                + "              }"
                + "      }"
                + "  }"
                + "}";
        String expectedReduction = "PREFIX hobbit: "
                + "\nPREFIX rdfs: "
                + "\n\nCONSTRUCT "
                + "WHERE ";
        Assert.assertEquals(expectedReduction, StorageService.reduceQueryToKeyWords(query));
    }
}
