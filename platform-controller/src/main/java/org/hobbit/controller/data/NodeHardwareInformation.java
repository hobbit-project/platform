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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.apache.jena.sparql.vocabulary.DOAP;
import org.apache.jena.vocabulary.RDFS;
import org.hobbit.utils.rdf.TripleHashCalculator;
import org.hobbit.vocab.HobbitHardware;
import org.hobbit.vocab.MEXCORE;

/**
 * This class is used to store information about hardware the experiment runs
 * on.
 *
 * @author Denis Kuchelev
 *
 */
public class NodeHardwareInformation {

    /**
     * Formatted hardware information.
     */
    private String instance;
    private String cpu;
    private String memory;
    private String os;

    /**
     * Formats a frequency value.
     *
     * @param frequency the frequency value in Hz
     * @return formatted value with a unit of measure
     */
    private String formatFrequencyValue(Long frequency) {
        return String.format("%.1f GHz", 1.0 * frequency / 1024 / 1024 / 1024);
    }

    /**
     * Formats a memory amount value.
     *
     * @param memory the memory value in B
     * @return formatted value with a unit of measure
     */
    private String formatMemoryValue(Long memory) {
        return String.format("%.1f GiB", 1.0 * memory / 1024 / 1024 / 1024);
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public void setCpu(Long cores, List<Long> frequencies) {
        StringBuilder builder = new StringBuilder();

        if (cores != null) {
            builder.append(cores).append("-core");
        }

        if (cores != null && frequencies.size() != 0) {
            builder.append(' ');
        }

        if (frequencies.size() != 0) {
            builder.append("(")
                    .append(frequencies.stream().map(this::formatFrequencyValue).collect(Collectors.joining(", ")))
                    .append(")");
        }

        cpu = builder.toString();
    }

    public void setMemory(Long memory, Long swap) {
        StringBuilder builder = new StringBuilder();

        if (memory != null) {
            builder.append("Memory: ").append(formatMemoryValue(memory));
        }

        if (memory != null && swap != null) {
            builder.append(", ");
        }

        if (swap != null) {
            builder.append("Swap: ").append(formatMemoryValue(swap));
        }

        this.memory = builder.toString();
    }

    public void setOs(String os) {
        if (os != null) {
            this.os = os;
        } else {
            this.os = "Unknown";
        }
    }

    private String hash() {
        Model dummyModel = ModelFactory.createDefaultModel();
        Resource dummyRes = dummyModel.createResource(TripleHashCalculator.HASH_SELF_URI);
        return TripleHashCalculator.calculateHash(distinguishingProperties(dummyModel, dummyRes));
    }

    public String getURI() {
        return HobbitHardware.getNodeURI(hash());
    }

    public Resource addToModel(Model model) {
        Resource res = model.createResource(getURI(), MEXCORE.HardwareConfiguration);
        model.add(distinguishingProperties(model, res));
        return res;
    }

    private StmtIterator distinguishingProperties(Model model, Resource self) {
        return new StmtIteratorImpl(
                Stream.of((Statement) new StatementImpl(self, RDFS.label, model.createLiteral(instance)),
                        (Statement) new StatementImpl(self, MEXCORE.cpu, model.createLiteral(cpu)),
                        (Statement) new StatementImpl(self, MEXCORE.memory, model.createLiteral(memory)),
                        (Statement) new StatementImpl(self, DOAP.os, model.createLiteral(os))).iterator());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NodeHardwareInformation [").append("instance=").append(instance).append(", ").append("cpu=")
                .append(cpu).append(", ").append("memory=").append(memory).append(", ").append("os=").append(os)
                .append("]");
        return builder.toString();
    }
}
