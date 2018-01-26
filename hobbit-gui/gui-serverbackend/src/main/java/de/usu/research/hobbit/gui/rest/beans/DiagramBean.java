package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.hobbit.utils.rdf.RdfHelper;

@XmlRootElement
public class DiagramBean extends KeyPerformanceIndicatorBean {

    private Point[] data;
    private String label;

    /**
     * @return the data
     */
    public Point[] getData() {
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(Point[] data) {
        this.data = data;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public static class Point implements Comparable<Point> {

        public static Point createFromObservation(Model model, Resource observation, Property dimensionProperty,
                Property measureProperty) {
            String dimensionString = RdfHelper.getStringValue(model, observation, dimensionProperty);
            if (dimensionString == null) {
                return null;
            }
            String measureString = RdfHelper.getStringValue(model, observation, measureProperty);
            if (measureString == null) {
                return null;
            }
            try {
                return new Point(Double.parseDouble(dimensionString), Double.parseDouble(measureString));
            } catch (Exception e) {
                return null;
            }
        }

        public double x;
        public double y;

        public Point() {
        }

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            builder.append(x);
            builder.append(',');
            builder.append(y);
            builder.append(')');
            return builder.toString();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Point other = (Point) obj;
            if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
                return false;
            if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
                return false;
            return true;
        }

        @Override
        public int compareTo(Point p) {
            if (this.x < p.x) {
                return -1;
            }
            if (this.x > p.x) {
                return 1;
            }
            if (this.y < p.y) {
                return -1;
            }
            if (this.y > p.y) {
                return 1;
            }
            return 0;
        }
    }
}
