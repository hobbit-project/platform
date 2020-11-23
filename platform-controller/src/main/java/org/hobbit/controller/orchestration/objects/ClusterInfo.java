package org.hobbit.controller.orchestration.objects;

import com.google.common.collect.ImmutableList;

import java.util.Date;

public class ClusterInfo {

    private String architecture;

    private String clusterStore;

    private String cgroupDriver;

    private Integer containers;

    private Integer containersRunning;

    private Integer containersStopped;

    private Integer containersPaused;

    private Boolean cpuCfsPeriod;

    private Boolean cpuCfsQuota;

    private Boolean debug;

    private String dockerRootDir;

    private String storageDriver;

    private ImmutableList<ImmutableList<String>> driverStatus;

    private Boolean experimentalBuild;

    private String httpProxy;

    private String httpsProxy;

    private String id;

    private Boolean ipv4Forwarding;

    private Integer images;

    private String indexServerAddress;

    private String initPath;

    private String initSha1;

    private Boolean kernelMemory;

    private String kernelVersion;

    private ImmutableList<String> labels;

    private Long memTotal;

    private Boolean memoryLimit;

    private Integer cpus;

    private Integer eventsListener;

    private Integer fileDescriptors;

    private Integer goroutines;

    private String name;

    private String noProxy;

    private Boolean oomKillDisable;

    private String operatingSystem;

    private String osType;

    private ImmutableList<ImmutableList<String>> systemStatus;

    private Date systemTime;

    public ClusterInfo() {
    }

    public ClusterInfo(String architecture, String clusterStore, String cgroupDriver, Integer containers, Integer containersRunning,
                       Integer containersStopped, Integer containersPaused, Boolean cpuCfsPeriod, Boolean cpuCfsQuota, Boolean debug,
                       String dockerRootDir, String storageDriver, ImmutableList<ImmutableList<String>> driverStatus, Boolean experimentalBuild,
                       String httpProxy, String httpsProxy, String id, Boolean ipv4Forwarding, Integer images, String indexServerAddress, String initPath,
                       String initSha1, Boolean kernelMemory, String kernelVersion, ImmutableList<String> labels, Long memTotal, Boolean memoryLimit, Integer cpus,
                       Integer eventsListener, Integer fileDescriptors, Integer goroutines, String name, String noProxy, Boolean oomKillDisable, String operatingSystem,
                       String osType, ImmutableList<ImmutableList<String>> systemStatus, Date systemTime) {
        this.architecture = architecture;
        this.clusterStore = clusterStore;
        this.cgroupDriver = cgroupDriver;
        this.containers = containers;
        this.containersRunning = containersRunning;
        this.containersStopped = containersStopped;
        this.containersPaused = containersPaused;
        this.cpuCfsPeriod = cpuCfsPeriod;
        this.cpuCfsQuota = cpuCfsQuota;
        this.debug = debug;
        this.dockerRootDir = dockerRootDir;
        this.storageDriver = storageDriver;
        this.driverStatus = driverStatus;
        this.experimentalBuild = experimentalBuild;
        this.httpProxy = httpProxy;
        this.httpsProxy = httpsProxy;
        this.id = id;
        this.ipv4Forwarding = ipv4Forwarding;
        this.images = images;
        this.indexServerAddress = indexServerAddress;
        this.initPath = initPath;
        this.initSha1 = initSha1;
        this.kernelMemory = kernelMemory;
        this.kernelVersion = kernelVersion;
        this.labels = labels;
        this.memTotal = memTotal;
        this.memoryLimit = memoryLimit;
        this.cpus = cpus;
        this.eventsListener = eventsListener;
        this.fileDescriptors = fileDescriptors;
        this.goroutines = goroutines;
        this.name = name;
        this.noProxy = noProxy;
        this.oomKillDisable = oomKillDisable;
        this.operatingSystem = operatingSystem;
        this.osType = osType;
        this.systemStatus = systemStatus;
        this.systemTime = systemTime;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getClusterStore() {
        return clusterStore;
    }

    public void setClusterStore(String clusterStore) {
        this.clusterStore = clusterStore;
    }

    public String getCgroupDriver() {
        return cgroupDriver;
    }

    public void setCgroupDriver(String cgroupDriver) {
        this.cgroupDriver = cgroupDriver;
    }

    public Integer getContainers() {
        return containers;
    }

    public void setContainers(Integer containers) {
        this.containers = containers;
    }

    public Integer getContainersRunning() {
        return containersRunning;
    }

    public void setContainersRunning(Integer containersRunning) {
        this.containersRunning = containersRunning;
    }

    public Integer getContainersStopped() {
        return containersStopped;
    }

    public void setContainersStopped(Integer containersStopped) {
        this.containersStopped = containersStopped;
    }

    public Integer getContainersPaused() {
        return containersPaused;
    }

    public void setContainersPaused(Integer containersPaused) {
        this.containersPaused = containersPaused;
    }

    public Boolean getCpuCfsPeriod() {
        return cpuCfsPeriod;
    }

    public void setCpuCfsPeriod(Boolean cpuCfsPeriod) {
        this.cpuCfsPeriod = cpuCfsPeriod;
    }

    public Boolean getCpuCfsQuota() {
        return cpuCfsQuota;
    }

    public void setCpuCfsQuota(Boolean cpuCfsQuota) {
        this.cpuCfsQuota = cpuCfsQuota;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public String getDockerRootDir() {
        return dockerRootDir;
    }

    public void setDockerRootDir(String dockerRootDir) {
        this.dockerRootDir = dockerRootDir;
    }

    public String getStorageDriver() {
        return storageDriver;
    }

    public void setStorageDriver(String storageDriver) {
        this.storageDriver = storageDriver;
    }

    public ImmutableList<ImmutableList<String>> getDriverStatus() {
        return driverStatus;
    }

    public void setDriverStatus(ImmutableList<ImmutableList<String>> driverStatus) {
        this.driverStatus = driverStatus;
    }

    public Boolean getExperimentalBuild() {
        return experimentalBuild;
    }

    public void setExperimentalBuild(Boolean experimentalBuild) {
        this.experimentalBuild = experimentalBuild;
    }

    public String getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(String httpProxy) {
        this.httpProxy = httpProxy;
    }

    public String getHttpsProxy() {
        return httpsProxy;
    }

    public void setHttpsProxy(String httpsProxy) {
        this.httpsProxy = httpsProxy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIpv4Forwarding() {
        return ipv4Forwarding;
    }

    public void setIpv4Forwarding(Boolean ipv4Forwarding) {
        this.ipv4Forwarding = ipv4Forwarding;
    }

    public Integer getImages() {
        return images;
    }

    public void setImages(Integer images) {
        this.images = images;
    }

    public String getIndexServerAddress() {
        return indexServerAddress;
    }

    public void setIndexServerAddress(String indexServerAddress) {
        this.indexServerAddress = indexServerAddress;
    }

    public String getInitPath() {
        return initPath;
    }

    public void setInitPath(String initPath) {
        this.initPath = initPath;
    }

    public String getInitSha1() {
        return initSha1;
    }

    public void setInitSha1(String initSha1) {
        this.initSha1 = initSha1;
    }

    public Boolean getKernelMemory() {
        return kernelMemory;
    }

    public void setKernelMemory(Boolean kernelMemory) {
        this.kernelMemory = kernelMemory;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public ImmutableList<String> getLabels() {
        return labels;
    }

    public void setLabels(ImmutableList<String> labels) {
        this.labels = labels;
    }

    public Long getMemTotal() {
        return memTotal;
    }

    public void setMemTotal(Long memTotal) {
        this.memTotal = memTotal;
    }

    public Boolean getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Boolean memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Integer getCpus() {
        return cpus;
    }

    public void setCpus(Integer cpus) {
        this.cpus = cpus;
    }

    public Integer getEventsListener() {
        return eventsListener;
    }

    public void setEventsListener(Integer eventsListener) {
        this.eventsListener = eventsListener;
    }

    public Integer getFileDescriptors() {
        return fileDescriptors;
    }

    public void setFileDescriptors(Integer fileDescriptors) {
        this.fileDescriptors = fileDescriptors;
    }

    public Integer getGoroutines() {
        return goroutines;
    }

    public void setGoroutines(Integer goroutines) {
        this.goroutines = goroutines;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNoProxy() {
        return noProxy;
    }

    public void setNoProxy(String noProxy) {
        this.noProxy = noProxy;
    }

    public Boolean getOomKillDisable() {
        return oomKillDisable;
    }

    public void setOomKillDisable(Boolean oomKillDisable) {
        this.oomKillDisable = oomKillDisable;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public ImmutableList<ImmutableList<String>> getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(ImmutableList<ImmutableList<String>> systemStatus) {
        this.systemStatus = systemStatus;
    }

    public Date getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(Date systemTime) {
        this.systemTime = systemTime;
    }
}
