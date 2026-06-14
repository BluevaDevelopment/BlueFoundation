package net.blueva.api.config;

interface ConfigCodec {
    ConfigDocument read(String text);

    String write(ConfigDocument document);

    String extension();
}
