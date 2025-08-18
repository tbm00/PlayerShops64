package dev.tbm00.spigot.playershops64.utils;

import java.io.*;
import java.util.Base64;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class ItemSerializer {

    /**
     * Converts an ItemStack to a Base64 string.
     */
    public static String itemStackToBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize ItemStack.", e);
        }
    }

    /**
     * Converts a Base64 string back into an ItemStack.
     */
    public static ItemStack itemStackFromBase64(String base64) {
        if (base64 == null) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();

        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException("Unable to deserialize ItemStack.", e);
        }
    }
}