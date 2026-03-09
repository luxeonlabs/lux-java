package com.remy.iso.networking.incoming.generic;

import com.remy.iso.networking.Packet;

public class ClientConfig extends Packet {
    public ClientConfigItem[] configs;

    public ClientConfig() {

    }

    @Override
    public void parse() {
        this.configs = readArray(ClientConfigItem.class, () -> {
            ClientConfigItem item = new ClientConfigItem();
            item.key = readString();
            item.value = readString();

            return item;
        });
    }

    public static class ClientConfigItem {
        public String key;
        public String value;
    }
}
