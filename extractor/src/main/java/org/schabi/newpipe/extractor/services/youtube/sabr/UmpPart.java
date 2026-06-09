package org.schabi.newpipe.extractor.services.youtube.sabr;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class UmpPart {
    private final int type;
    private final int size;
    @Nonnull
    private final byte[] data;
    private final int offset;

    UmpPart(final int type, final int size, @Nonnull final byte[] data) {
        this(type, size, data, 0);
    }

    UmpPart(final int type, final int size, @Nonnull final byte[] data, final int offset) {
        this.type = type;
        this.size = size;
        this.data = data;
        this.offset = offset;
    }

    public int getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    @Nonnull
    public byte[] getData() {
        return Arrays.copyOfRange(data, offset, offset + size);
    }

    @Nonnull
    byte[] getRawData() {
        return getData();
    }

    int getUnsignedByteAt(final int index) throws SabrProtocolException {
        if (index < 0 || index >= size) {
            throw new SabrProtocolException("UMP part index out of bounds: " + index);
        }
        return data[offset + index] & 0xff;
    }

    void writeTo(@Nonnull final ByteArrayOutputStream output,
                 final int relativeOffset,
                 final int length) throws SabrProtocolException {
        if (relativeOffset < 0 || length < 0 || relativeOffset > size - length) {
            throw new SabrProtocolException("UMP part write range out of bounds");
        }
        output.write(data, offset + relativeOffset, length);
    }
}
