package software.coley.recaf.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LargeOutputStream extends OutputStream {
    private final List<byte[]> data = new ArrayList<>();

    @Override
    public void write(int b) throws IOException {
        var x = new byte[1];
        x[0] = (byte) b;
        this.data.add(x);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.data.add(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.data.add(Arrays.copyOfRange(b, off, off + len));
    }

    public LargeByteArray toByteArray() {
        return LargeByteArray.from(this.data);
    }
}
