# Dockerizing Virtuoso

To simplify deployment, Virtuoso Open Source Edition (VOS) can be run in a [Docker](https://www.docker.com) container. 
The following notes describe running VOS standalone under Docker. 

## Obtaining VOS from Docker Hub

A [prebuilt VOS image](https://hub.docker.com/r/openlink/virtuoso_opensource/) can be pulled from Docker Hub:

    docker pull openlink/virtuoso_opensource:vos

## Building a VOS Base Image

Two VOS images are available:

* A base image which provides a compiled, installed VOS binary.
* A deployment image, built from the base image, which starts a VOS instance on the specified ports using the given VOS virtuoso.ini configuration file and, optionally, an existing Virtuoso database.

To build the base image:

    docker build -f Dockerfile.vos_base -t openlink/virtuoso_opensource:vos_base .
    
Dockerfile.vos_base builds VOS from the VOS GitHub sources, as described in the [VOS Wiki](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSUbuntuNotes#Building%20from%20Upstream%20Source), and installs VOS in /opt/virtuoso-opensource.

## Building a VOS Deployment Image

To build the deployment image:

    docker build -f Dockerfile.vos -t openlink/virtuoso_opensource:vos .
    
The image created by Dockerfile.vos runs Virtuoso in the foreground and assumes Virtuoso listens for HTTP connections on port 8890 and SQL connections on port 1111. 

## Running VOS within Docker

### Initializing a new database

In order to retain changes to the Virtuoso database, the database should be held in the host file system. The database location on the host should reflect the installation directory used by the base image. Create directory /opt/virtuoso-opensource/database in the host file system and provide a virtuoso.ini configuration file.

    sudo mkdir -p /opt/virtuoso-opensource/database
    sudo cp ./virtuoso.ini.template /opt/virtuoso-opensource/database/virtuoso.ini
    sudo chmod +w /opt/virtuoso-opensource/database
    
virtuoso.ini.template assumes the installation directory is /opt/virtuoso-opensource, with the Virtuoso HTTP server listening on port 8890 and SQL client connections made through port 1111.

### Starting the VOS Container

Start a VOS container by running:

    sudo docker run --name vos -d -v /opt/virtuoso-opensource/database:/opt/virtuoso-opensource/database -t -p 1111:1111 -p 8890:8890 -i openlink/virtuoso_opensource:vos
    
If the db directory contains only a virtuoso.ini file, a new database will be created when the container is started for the first time. All subsequent changes to the database will be persisted to the host file system.

### Using an existing database

If the db directory in the host file system contains an existing Virtuoso database, that database will be used by the container. Again, all subsequent changes to the database will be persisted to the host file system.
