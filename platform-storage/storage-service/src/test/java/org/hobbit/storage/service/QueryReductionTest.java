/**
 * This file is part of storage-service.
 *
 * storage-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * storage-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with storage-service.  If not, see <http://www.gnu.org/licenses/>.
 */
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
