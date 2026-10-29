package com.sharedmemory.face;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Converter
public class FloatArrayConverter implements AttributeConverter<float[], byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(attribute.length * Float.BYTES);
        for (float value : attribute) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    @Override
    public float[] convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        FloatBuffer floatBuffer = ByteBuffer.wrap(dbData).asFloatBuffer();
        float[] result = new float[floatBuffer.remaining()];
        floatBuffer.get(result);
        return result;
    }
}
