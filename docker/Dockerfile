# ---------- Stage 1: fmusim 0.0.5 from Maven Central ----------
FROM maven:3.9-eclipse-temurin-25 AS jars
WORKDIR /build

COPY pom.xml .
RUN mvn -B package -DskipTests

# ---------- Stage 2: FMI++ v2.1 and SUNDIALS 3.2.1 ----------
# Ubuntu 26.04 ships SUNDIALS 7. FMI++ v2.1 calls the SUNDIALS 3 CVODE API
# (CVodeCreate with two arguments, CVDlsSetLinearSolver, SUNDenseLinearSolver).
FROM eclipse-temurin:25-jdk-resolute AS native

ARG SUNDIALS_VERSION=3.2.1
# Tag v2.1. The Java bindings in imp 2.1.0 match this revision.
ARG FMIPP_REF=v2.1

# CMake 4 rejects the cmake_minimum_required() in the SUNDIALS 3 sources.
ENV CMAKE_POLICY_VERSION_MINIMUM=3.5

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
         build-essential \
         ca-certificates \
         cmake \
         git \
         libboost-date-time-dev \
         libboost-filesystem-dev \
         swig \
         wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /tmp/sundials
RUN wget -qO sundials.tar.gz \
      "https://github.com/LLNL/sundials/archive/refs/tags/v${SUNDIALS_VERSION}.tar.gz" \
    && tar -xzf sundials.tar.gz \
    && cmake -S "sundials-${SUNDIALS_VERSION}" -B build \
         -DCMAKE_INSTALL_PREFIX=/usr/local \
         -DCMAKE_INSTALL_LIBDIR=lib \
         -DBUILD_SHARED_LIBS=ON \
         -DEXAMPLES_ENABLE=OFF \
         -DCMAKE_C_FLAGS="-fcommon -Wno-error=implicit-function-declaration -std=gnu99" \
    && cmake --build build --parallel \
    && cmake --install build \
    && rm -rf /tmp/sundials

# v2.1 builds as C++17 and accepts Java's file:/ URIs. The export library does
# not compile against Boost 1.90 (ipcdetail::thread_sleep, filesystem::copy_option).
# This image only needs the import library.
WORKDIR /tmp/fmipp
RUN git init \
    && git remote add origin https://github.com/fmipp/fmipp.git \
    && git fetch --depth 1 origin tag "${FMIPP_REF}" \
    && git checkout FETCH_HEAD \
    && sed -i '/add_subdirectory( export )/s/^/# /' CMakeLists.txt \
    && cmake -S . -B build \
         -DINCLUDE_SUNDIALS=ON \
         -DSUNDIALS_INCLUDEDIR=/usr/local/include \
         -DSUNDIALS_LIBRARYDIR=/usr/local/lib \
         -DBUILD_SWIG=ON \
         -DBUILD_SWIG_JAVA=ON \
         -DBUILD_SWIG_PYTHON=OFF \
         -DBUILD_TESTS=OFF \
         -DCMAKE_BUILD_TYPE=Release \
    && cmake --build build --parallel \
    && find build -name 'libfmipp*.so' -exec cp -a {} /usr/local/lib/ \; \
    && rm -rf /tmp/fmipp

# ---------- Stage 3: runtime (JDK 25, Ubuntu 26.04) ----------
FROM eclipse-temurin:25-jdk-resolute

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
         libboost-filesystem-dev \
         unzip \
    && rm -rf /var/lib/apt/lists/*

COPY --from=native /usr/local/lib /usr/local/lib
RUN ldconfig

WORKDIR /app
COPY --from=jars /build/target/dependency/fmusim-0.0.5-jar-with-dependencies.jar /app/lib/fmusim-0.0.5-jar-with-dependencies.jar

# Connection string for configuration server. Override at runtime with -e LLCONFIG=...
# LOGGER_CONFIG and CONFIG_FILE_URI are derived from this in entrypoint.sh.
ENV LLCONFIG=http://lablink-config:10101/get?id=

# FMU simulator configuration id (appended to LLCONFIG).
ENV FMUSIM_CONFIG=lablink-fmusim.config

# Path to class implementing the main routine.
ENV FMUSIM_CLASS=at.ac.ait.lablink.clients.fmusim.DynamicFmuModelExchangeAsync

# Path to Java JAR file of the FMU simulator.
ENV FMUSIM_JAR_FILE=/app/lib/fmusim-0.0.5-jar-with-dependencies.jar

# Specify the maximum size (in bytes) of the memory allocation pool.
ENV MEMORY_FLAG=-Xmx512m

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
