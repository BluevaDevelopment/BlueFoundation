package net.blueva.foundation.config;

interface ConfigCodec {
    ConfigDocument read(String text);

    String write(ConfigDocument document);

    String extension();
}
