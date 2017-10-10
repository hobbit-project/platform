package de.usu.research.hobbit.gui.rest.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SystemMetaFileBean extends SystemBean {

    private String benchmarkApi;
    private String dockerImage;

    public SystemMetaFileBean() {
        super();
    }

    public SystemMetaFileBean(String id, String name, String benchmarkApi, String dockerImage, String description) {
        super(id, name, description);
        this.benchmarkApi = benchmarkApi;
        this.dockerImage = dockerImage;
    }

    public String getBenchmarkApi() {
        return benchmarkApi;
    }

    public void setBenchmarkApi(String benchmarkApi) {
        this.benchmarkApi = benchmarkApi;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }
}
