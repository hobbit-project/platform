package org.hobbit.controller.kubernetes.networkAttachmentDefinitionCustomResources;


public class Routes {

    private String dst;

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    @Override
    public String toString() {
        return "Routes{" +
            "dst='" + dst + '\'' +
            '}';
    }
}
