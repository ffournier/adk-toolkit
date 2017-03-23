package me.palazzetti.adktoolkit;

import me.palazzetti.adktoolkit.response.AdkMessage;
import java.io.IOException;

/**
 * Defines ADK interfaces
 */

public interface IAdkManager {
    AdkMessage read() throws IOException;
    void write(byte[] values);
    void write(byte value);
    void write(int value);
    void write(float value);
    void write(String text);

    // Activity related interfaces
    void close();
    boolean open();
}
