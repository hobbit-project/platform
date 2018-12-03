package org.hobbit.controller.docker;

import com.spotify.docker.client.messages.mount.BindOptions;
import com.spotify.docker.client.messages.mount.Mount;
import com.spotify.docker.client.messages.mount.TmpfsOptions;
import com.spotify.docker.client.messages.mount.VolumeOptions;

/**
 * @author Pavel Smirnov. (psmirnov@agtinternational.com / smirnp@gmail.com)
 */
public class MountImpl extends Mount {


    private String type;
    private String source;
    private String target;
    private Boolean readOnly;
    private BindOptions bindOptions;
    private VolumeOptions volumeOptions;
    private TmpfsOptions tmpfsOptions;

//    public MountImpl(Builder builder){
//        type = builder.type;
//        source = builder.source;
//        target = builder.target;
//        readOnly = builder.readOnly;
//        bindOptions = builder.bindOptions;
//        volumeOptions = builder.volumeOptions;
//        tmpfsOptions = builder.tmpfsOptions;
//    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public String target() {
        return target;
    }

    @Override
    public Boolean readOnly() {
        return readOnly;
    }

    @Override
    public BindOptions bindOptions() {
        return bindOptions;
    }

    @Override
    public VolumeOptions volumeOptions() {
        return volumeOptions;
    }

    @Override
    public TmpfsOptions tmpfsOptions() {
        return tmpfsOptions;
    }

//    public static class Builder {
//        private String type;
//        private String source;
//        private String target;
//        private Boolean readOnly;
//        private BindOptions bindOptions;
//        private VolumeOptions volumeOptions;
//        private TmpfsOptions tmpfsOptions;
//
//
//        public Builder type(String value) {
//            type = value;
//            return this;
//        }
//
//        public Builder source(String value) {
//            source=value;
//            return this;
//        }
//
//        public Builder target(String value) {
//            target=value;
//            return this;
//        }
//
//        public Builder readOnly(Boolean value) {
//            readOnly=value;
//            return this;
//        }
//
//        public Builder bindOptions(BindOptions value) {
//            bindOptions=value;
//            return this;
//        }
//
//        public Builder volumeOptions(VolumeOptions value) {
//            volumeOptions=value;
//            return this;
//        }
//
//        public Builder tmpfsOptions(TmpfsOptions value){
//            tmpfsOptions=value;
//            return this;
//        }
//
//        public MountImpl build(Builder builder){
//            return new MountImpl(builder);
//        }
//
//    }
}
