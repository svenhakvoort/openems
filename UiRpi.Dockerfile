FROM arm32v7/node:14-slim

# disable angular analytics
ENV NG_CLI_ANALYTICS=false
RUN npm install -g @angular/cli
RUN apt-get update && apt-get install -y curl wget

# Install dependencies
RUN apt-get update && apt-get install -y -q --no-install-recommends \
    build-essential \
    libfontconfig1-dev \
    libfreetype6-dev \
    libjpeg-dev \
    libpng-dev \
    libssl-dev \
    libx11-dev \
    libxext-dev \
    libxrender-dev \
    python \
    zlib1g-dev \
    xfonts-75dpi \
    xfonts-base \
    && rm -rf /var/lib/apt/lists/*
RUN wget http://ftp.nl.debian.org/debian/pool/main/libj/libjpeg-turbo/libjpeg62-turbo_1.5.2-2+deb10u1_armhf.deb
RUN dpkg -i libjpeg62-turbo_1.5.2-2+deb10u1_armhf.deb

# Install wkhtmltopdf
ENV WKHTMLTOPDF_VERSION 0.12.6-1
ENV WKHTMLTOPDF_RELEASE raspberrypi.buster_armhf
RUN curl -o wkhtmltox.deb -sSL https://github.com/wkhtmltopdf/packaging/releases/download/${WKHTMLTOPDF_VERSION}/wkhtmltox_${WKHTMLTOPDF_VERSION}.${WKHTMLTOPDF_RELEASE}.deb \
    && apt-get update \
    && apt-get install -y ./wkhtmltox.deb \
    && rm -rf /var/lib/apt/lists/* wkhtmltox.deb

WORKDIR /app

COPY ui .
RUN npm install

CMD ng serve -c openems-backend-prod --host 0.0.0.0 --disable-host-check