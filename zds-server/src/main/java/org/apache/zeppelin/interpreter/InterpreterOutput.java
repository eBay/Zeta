package org.apache.zeppelin.interpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tatian on 2018/6/8.
 */
public class InterpreterOutput extends OutputStream {
    Logger logger = LoggerFactory.getLogger(InterpreterOutput.class);
    private final int NEW_LINE_CHAR = '\n';

    private List<InterpreterResultMessageOutput> resultMessageOutputs = new LinkedList<>();
    private InterpreterResultMessageOutput currentOut;
    private List<String> resourceSearchPaths = Collections.synchronizedList(new LinkedList<String>());

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final InterpreterOutputListener flushListener;
    private final InterpreterOutputChangeListener changeListener;

    private int size = 0;

    // change static var to set interpreter output limit
    // limit will be applied to all InterpreterOutput object.
    // so we can expect the consistent behavior
    public static int limit = 2 * 1014 * 1024 * 1024;

    public InterpreterOutput(InterpreterOutputListener flushListener) {
        this.flushListener = flushListener;
        changeListener = null;
        clear();
    }

    public InterpreterOutput(InterpreterOutputListener flushListener,
                             InterpreterOutputChangeListener listener)
            throws IOException {
        this.flushListener = flushListener;
        this.changeListener = listener;
        clear();
    }

    public void setType(InterpreterResult.Type type) throws IOException {
        InterpreterResultMessageOutput out = null;

        synchronized (resultMessageOutputs) {
            int index = resultMessageOutputs.size();
            InterpreterResultMessageOutputListener listener =
                    createInterpreterResultMessageOutputListener(index);

            if (changeListener == null) {
                out = new InterpreterResultMessageOutput(type, listener);
            } else {
                out = new InterpreterResultMessageOutput(type, listener, changeListener);
            }
            out.setResourceSearchPaths(resourceSearchPaths);

            buffer.reset();
            size = 0;

            if (currentOut != null) {
                currentOut.flush();
            }

            resultMessageOutputs.add(out);
            currentOut = out;
        }
    }

    public InterpreterResultMessageOutputListener createInterpreterResultMessageOutputListener(
            final int index) {

        return new InterpreterResultMessageOutputListener() {
            final int idx = index;

            @Override
            public void onAppend(InterpreterResultMessageOutput out, byte[] line) {
                if (flushListener != null) {
                    flushListener.onAppend(idx, out, line);
                }
            }

            @Override
            public void onUpdate(InterpreterResultMessageOutput out) {
                if (flushListener != null) {
                    flushListener.onUpdate(idx, out);
                }
            }
        };
    }

    public InterpreterResultMessageOutput getCurrentOutput() {
        synchronized (resultMessageOutputs) {
            return currentOut;
        }
    }

    public InterpreterResultMessageOutput getOutputAt(int index) {
        synchronized (resultMessageOutputs) {
            return resultMessageOutputs.get(index);
        }
    }

    public int size() {
        synchronized (resultMessageOutputs) {
            return resultMessageOutputs.size();
        }
    }

    public void clear() {
        size = 0;
        truncated = false;
        buffer.reset();

        synchronized (resultMessageOutputs) {
            for (InterpreterResultMessageOutput out : resultMessageOutputs) {
                out.clear();
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            // clear all ResultMessages
            resultMessageOutputs.clear();
            currentOut = null;
            startOfTheNewLine = true;
            firstCharIsPercentSign = false;
            updateAllResultMessages();
        }
    }

    private void updateAllResultMessages() {
        if (flushListener != null) {
            flushListener.onUpdateAll(this);
        }
    }


    int previousChar = 0;
    boolean startOfTheNewLine = true;
    boolean firstCharIsPercentSign = false;

    boolean truncated = false;

    @Override
    public void write(int b) throws IOException {
        InterpreterResultMessageOutput out;
        if (truncated) {
            return;
        }

        synchronized (resultMessageOutputs) {
            currentOut = getCurrentOutput();

            if (++size > limit) {
                if (b == NEW_LINE_CHAR && currentOut != null) {
                    InterpreterResult.Type type = currentOut.getType();
                    if (type == InterpreterResult.Type.TEXT || type == InterpreterResult.Type.TABLE) {

                        setType(InterpreterResult.Type.TEXT);
                        getCurrentOutput().write("Output exceeds " + limit + ". Truncated.\n");
                        truncated = true;
                        return;
                    }
                }
            }

            if (startOfTheNewLine) {
                if (b == '%') {
                    startOfTheNewLine = false;
                    firstCharIsPercentSign = true;
                    buffer.write(b);
                    previousChar = b;
                    return;
                } else if (b != NEW_LINE_CHAR) {
                    startOfTheNewLine = false;
                }
            }

            if (b == NEW_LINE_CHAR) {
                if (currentOut != null && currentOut.getType() == InterpreterResult.Type.TABLE) {
                    if (previousChar == NEW_LINE_CHAR) {
                        startOfTheNewLine = true;
                        return;
                    }
                } else {
                    startOfTheNewLine = true;
                }
            }

            boolean flushBuffer = false;
            if (firstCharIsPercentSign) {
                if (b == ' ' || b == NEW_LINE_CHAR || b == '\t') {
                    firstCharIsPercentSign = false;
                    String displaySystem = buffer.toString();
                    for (InterpreterResult.Type type : InterpreterResult.Type.values()) {
                        if (displaySystem.equals('%' + type.name().toLowerCase())) {
                            // new type detected
                            setType(type);
                            previousChar = b;
                            return;
                        }
                    }
                    // not a defined display system
                    flushBuffer = true;
                } else {
                    buffer.write(b);
                    previousChar = b;
                    return;
                }
            }

            out = getCurrentOutputForWriting();

            if (flushBuffer) {
                out.write(buffer.toByteArray());
                buffer.reset();
            }
            out.write(b);
            previousChar = b;
        }
    }

    private InterpreterResultMessageOutput getCurrentOutputForWriting() throws IOException {
        synchronized (resultMessageOutputs) {
            InterpreterResultMessageOutput out = getCurrentOutput();
            if (out == null) {
                // add text type result message
                setType(InterpreterResult.Type.TEXT);
                out = getCurrentOutput();
            }
            return out;
        }
    }

    @Override
    public void write(byte [] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte [] b, int off, int len) throws IOException {
        for (int i = off; i < len; i++) {
            write(b[i]);
        }
    }

    /**
     * In dev mode, it monitors file and update ZeppelinServer
     * @param file
     * @throws IOException
     */
    public void write(File file) throws IOException {
        InterpreterResultMessageOutput out = getCurrentOutputForWriting();
        out.write(file);
    }

    public void write(String string) throws IOException {
        write(string.getBytes());
    }

    /**
     * write contents in the resource file in the classpath
     * @param url
     * @throws IOException
     */
    public void write(URL url) throws IOException {
        InterpreterResultMessageOutput out = getCurrentOutputForWriting();
        out.write(url);
    }

    public void addResourceSearchPath(String path) {
        resourceSearchPaths.add(path);
    }

    public void writeResource(String resourceName) throws IOException {
        InterpreterResultMessageOutput out = getCurrentOutputForWriting();
        out.writeResource(resourceName);
    }

    public List<InterpreterResultMessage> toInterpreterResultMessage() throws IOException {
        List<InterpreterResultMessage> list = new LinkedList<>();
        synchronized (resultMessageOutputs) {
            for (InterpreterResultMessageOutput out : resultMessageOutputs) {
                list.add(out.toInterpreterResultMessage());
            }
        }
        return list;
    }

    public void flush() throws IOException {
        InterpreterResultMessageOutput out = getCurrentOutput();
        if (out != null) {
            out.flush();
        }
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        synchronized (resultMessageOutputs) {
            for (InterpreterResultMessageOutput m : resultMessageOutputs) {
                out.write(m.toByteArray());
            }
        }

        return out.toByteArray();
    }

    @Override
    public void close() throws IOException {
        synchronized (resultMessageOutputs) {
            for (InterpreterResultMessageOutput out : resultMessageOutputs) {
                out.close();
            }
        }
    }
}
