# Base
FROM java:openjdk-8u66-jdk

# Makes sure all tools for the build are installed
RUN apt-get update && apt-get install -y \
    git \
    subversion \
    zip \
    && rm -rf /var/lib/apt/lists/*

# Git configuration
RUN git config --global user.email "jenkins@nemerosa.net" \
    && git config --global user.name "Jenkins"
