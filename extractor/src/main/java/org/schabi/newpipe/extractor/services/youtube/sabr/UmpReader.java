package org.schabi.newpipe.extractor.services.youtube.sabr;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for YouTube's UMP envelope. UMP uses its own compact integer format, not protobuf varints.
 */
public final class UmpReader {
    private static final int MAX_UMP_PARTS = 4096;
    private static final int MAX_UMP_PART_SIZE = 64 * 1024 * 1024;

    private UmpReader() {
    }

    @Nonnull
    public static List<UmpPart> readAll(@Nonnull final byte[] data) throws SabrProtocolException {
        final Cursor cursor = new Cursor(data);
        final List<UmpPart> parts = new ArrayList<>();
        while (!cursor.isDone()) {
            if (parts.size() >= MAX_UMP_PARTS) {
                throw new SabrProtocolException("Too many UMP parts");
            }
            final int type = cursor.readUmpInt();
            final int size = cursor.readUmpInt();
            if (type < 0 || size < 0) {
                throw new SabrProtocolException("Invalid UMP part header");
            }
            parts.add(cursor.readPart(type, size));
        }
        return parts;
    }

    private static final class Cursor {
        private final byte[] data;
        private int offset;

        private Cursor(@Nonnull final byte[] data) {
            this.data = data;
        }

        boolean isDone() {
            return offset >= data.length;
        }

        int readUmpInt() throws SabrProtocolException {
            final int first = readUnsignedByte();
            final long value;
            if (first < 128) {
                value = first;
            } else if (first < 192) {
                value = (first & 0x3f) + 64L * readUnsignedByte();
            } else if (first < 224) {
                value = (first & 0x1f) + 32L * (readUnsignedByte()
                        + 256L * readUnsignedByte());
            } else if (first < 240) {
                value = (first & 0x0f) + 16L * (readUnsignedByte()
                        + 256L * (readUnsignedByte() + 256L * readUnsignedByte()));
            } else {
                value = readUnsignedByte()
                        + 256L * (readUnsignedByte()
                        + 256L * (readUnsignedByte() + 256L * readUnsignedByte()));
            }
            if (value > Integer.MAX_VALUE) {
                throw new SabrProtocolException("UMP integer too large: " + value);
            }
            return (int) value;
        }

        @Nonnull
        UmpPart readPart(final int type, final int length) throws SabrProtocolException {
            if (length < 0 || length > MAX_UMP_PART_SIZE || length > data.length - offset) {
                throw new SabrProtocolException("Unexpected EOF while reading UMP part data");
            }
            final UmpPart part = new UmpPart(type, length, data, offset);
            offset += length;
            return part;
        }

        private int readUnsignedByte() throws SabrProtocolException {
            if (offset >= data.length) {
                throw new SabrProtocolException("Unexpected EOF in UMP integer");
            }
            return data[offset++] & 0xff;
        }
    }
}
