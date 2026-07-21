package com.novadb.storage;

import com.novadb.exception.StorageException;
import com.novadb.model.Column;
import com.novadb.model.DataType;
import com.novadb.model.Row;
import com.novadb.model.TableInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Handles encoding and decoding of a Row to/from binary format based on TableInfo schema.
 */
public class RowCodec {

    public static byte[] encode(Row row, TableInfo tableInfo) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
             
            for (Column col : tableInfo.columns()) {
                Object value = row.get(col.name());
                if (value == null) {
                    dos.writeBoolean(false); // is present indicator
                } else {
                    dos.writeBoolean(true);
                    writeColumnValue(dos, value, col.type());
                }
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new StorageException("Failed to encode row", e);
        }
    }

    public static Row decode(byte[] data, TableInfo tableInfo) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {
             
            Row row = new Row();
            for (Column col : tableInfo.columns()) {
                boolean isPresent = dis.readBoolean();
                if (isPresent) {
                    Object value = readColumnValue(dis, col.type());
                    row.put(col.name(), value);
                } else {
                    row.put(col.name(), null);
                }
            }
            return row;
        } catch (IOException e) {
            throw new StorageException("Failed to decode row", e);
        }
    }

    private static void writeColumnValue(DataOutputStream dos, Object value, DataType type) throws IOException {
        switch (type) {
            case INT -> dos.writeInt(((Number) value).intValue());
            case LONG -> dos.writeLong(((Number) value).longValue());
            case DOUBLE -> dos.writeDouble(((Number) value).doubleValue());
            case FLOAT -> dos.writeFloat(((Number) value).floatValue());
            case BOOLEAN -> dos.writeBoolean((Boolean) value);
            case STRING, DATE -> dos.writeUTF(value.toString());
        }
    }

    private static Object readColumnValue(DataInputStream dis, DataType type) throws IOException {
        return switch (type) {
            case INT -> dis.readInt();
            case LONG -> dis.readLong();
            case DOUBLE -> dis.readDouble();
            case FLOAT -> dis.readFloat();
            case BOOLEAN -> dis.readBoolean();
            case STRING, DATE -> dis.readUTF();
        };
    }
}
