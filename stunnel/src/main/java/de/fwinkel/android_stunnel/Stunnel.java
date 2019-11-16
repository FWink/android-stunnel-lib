package de.fwinkel.android_stunnel;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.annotation.NonNull;

public class Stunnel implements Closeable {

    static final String LOG_TAG = "Stunnel";

    @NonNull
    protected final File binaryPath;
    @NonNull
    protected final File configPath;

    private Process process;

    public Stunnel(@NonNull File binaryPath, @NonNull File configPath) {
        this.binaryPath = binaryPath;
        this.configPath = configPath;
    }

    /**
     * Starts the Stunnel process and does not return until the configuration has been fully applied.
     * @throws IOException When there is an error starting the Stunnel process or while reading from
     * the Stunnel process' output.
     */
    public void start() throws IOException {
        this.process = new ProcessBuilder(binaryPath.getAbsolutePath(), configPath.getAbsolutePath()).start();

        //wait until Stunnel is fully initialized
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));

        String line;
        while((line = reader.readLine()) != null) {
//            Log.d(LOG_TAG, line);

            if(line.contains("Configuration successful"))
                break;
        }
    }

    @Override
    public void close() throws IOException {
        if(process != null)
            process.destroy();
    }
}
