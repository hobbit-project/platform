package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean that represents the license information of the dataset.
 */
@XmlRootElement
public class LicenseBean {
    private String attributionURL;
    private String attributionName;
    private String licenseURL;
    private String licenseLabel;
    private String licenseIconURL;

    public LicenseBean() {
        super();
    }

    public LicenseBean(
        String attributionURL,
        String attributionName,
        String licenseURL,
        String licenseLabel,
        String licenseIconURL
    ) {
        super();
        this.attributionURL = attributionURL;
        this.attributionName = attributionName;
        this.licenseURL = licenseURL;
        this.licenseLabel = licenseLabel;
        this.licenseIconURL = licenseIconURL;
    }

    public String getAttributionURL() {
        return attributionURL;
    }

    public void setAttributionURL(String attributionURL) {
        this.attributionURL = attributionURL;
    }

    public String getAttributionName() {
        return attributionName;
    }

    public void setAttributionName(String attributionName) {
        this.attributionName = attributionName;
    }

    public String getLicenseURL() {
        return licenseURL;
    }

    public void setLicenseURL(String licenseURL) {
        this.licenseURL = licenseURL;
    }

    public String getLicenseLabel() {
        return licenseLabel;
    }

    public void setLicenseLabel(String licenseLabel) {
        this.licenseLabel = licenseLabel;
    }

    public String getLicenseIconURL() {
        return licenseIconURL;
    }

    public void setLicenseIconURL(String licenseIconURL) {
        this.licenseIconURL = licenseIconURL;
    }
}
