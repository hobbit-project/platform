/**
 * This file is part of gui-serverbackend.
 * <p>
 * gui-serverbackend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * gui-serverbackend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with gui-serverbackend.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.usu.research.hobbit.gui.rest.beans;

import de.usu.research.hobbit.gui.rest.Datatype;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ConfigurationParamBean extends NamedEntityBean {
    private Datatype datatype;
    private String range;
    private String defaultValue;
    private List<SelectOptionBean> options;

    private boolean isFeature = false;

    private boolean required;
    private Integer min;
    private Integer max;

    public ConfigurationParamBean() {
    }

    public ConfigurationParamBean(String name, Datatype datatype) {
        this(name, datatype, false, null);
    }

    public ConfigurationParamBean(String name, Datatype datatype, boolean required, String defaultValue) {
        this.name = name;
        this.datatype = datatype;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public void setDatatype(Datatype datatype) {
        this.datatype = datatype;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public List<SelectOptionBean> getOptions() {
        return options;
    }

    public void setOptions(List<SelectOptionBean> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public boolean isFeature() {
        return isFeature;
    }

    public void setFeature(boolean isFeature) {
        this.isFeature = isFeature;
    }
}
