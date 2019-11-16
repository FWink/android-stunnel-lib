package de.fwinkel.android_stunnel;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Builder class to setup Stunnel configurations.<br/>
 * A Stunnel configuration consists of one or more <i>services</i>. A service is one single proxy
 * redirecting traffic from a TCP server socket to a destination.
 * Services run either in client mode (incoming connections are plaintext, outgoing connections
 * are secured with TLS) or in server mode (the other way around).<br/>
 * You can add services to the builder's config by calling {@link #addService()} and {@link ServiceBuilder#apply()}
 * when you're done.<br/>
 * When your configuration is done you can start the Stunnel services by calling {@link #start()}.
 */
public class StunnelBuilder extends StunnelConfigBuilder<StunnelBuilder> {

    protected static final String KEY_FOREGROUND = "foreground";

    /**
     * Indicates, when false, that we did not clear the content of {@link #getTemporaryBaseDirectory()}
     * yet in {@link #getTemporaryDirectory()} (so it indicates that {@link #getTemporaryDirectory()}
     * is called for the first time since application start).
     */
    private static final AtomicBoolean housekeepingCleanedTempDirectory = new AtomicBoolean(false);

    @NonNull protected final Context context;

    /**
     * Directory where we store Stunnel config files: the primary config file
     * and for example PSKSecrets files.
     */
    private File tempDirectory;

    public StunnelBuilder(@NonNull Context context) {
        this.context = context;
    }

    //<editor-fold desc="Stunnel start">

    /**
     * Applies all given option by writing a config file in a temporary directory
     * and returns a prepared yet non-{@link Stunnel#start()}ed {@link Stunnel} object.
     * @return
     */
    public Stunnel create() throws IOException {
        File binaryPath = new File(context.getApplicationInfo().nativeLibraryDir, "libstunnel.so");
        File configFile = writeConfigFile();

        return new Stunnel(binaryPath, configFile) {
            @Override
            public void start() throws IOException {
                try {
                    super.start();
                }
                finally {
                    //delete our temp files. aren't needed anymore
                    //TODO not sure if this will always work with secondary config files (eg psk)
                    // (i.e. not sure if some files might be read at a later point)
                    deleteDirectory(getTemporaryDirectory());
                }
            }
            //            @Override
//            public void close() throws IOException {
//                try {
//                    super.close();
//                } finally {
//                    //delete temporary directory
//                    deleteDirectory(this.configPath.getParentFile());
//                }
//            }
        };
    }

    /**
     * Same as {@link #create()} but immediately {@link Stunnel#start()}s the {@link Stunnel} process.
     * @return
     */
    public Stunnel start() throws IOException {
        Stunnel stunnel = create();
        try {
            stunnel.start();
        } catch (IOException e) {
            try {
                stunnel.close();
            } catch (IOException ex) {
                //ignore
            }

            throw e;
        }

        return stunnel;
    }

    /**
     * Returns an additional set of config option which we add regardless of the otherwise given options
     * to ensure optimal execution in Android.
     * @return
     */
    protected List<String> getStaticConfig() {
        StunnelConfigBuilder<StunnelConfigBuilder> staticConfigBuilder = new StunnelConfigBuilder<StunnelConfigBuilder>() {
        };

        staticConfigBuilder.setOption(new StunnelOption<>(KEY_FOREGROUND, new StunnelBoolean(true)));

        return staticConfigBuilder.config;
    }

    /**
     * Writes {@link #getConfigFileContent()} into the temporary file
     * returned by {@link #newConfigFile()}.
     * @return
     * @throws IOException
     */
    protected File writeConfigFile() throws IOException {
        File configFile = newConfigFile(true);

        Writer writer = null;
        try {
            writer = new FileWriter(configFile);
            //write BOM
            writer.write('\uFEFF');
            //write file content
            writer.write(getConfigFileContent());
        }
        finally {
            if(writer != null) {
                try {
                    writer.close();
                }
                catch(IOException e) {
                    //ignore
                }
            }
        }

        return configFile;
    }

    /**
     * Writes the current {@link #config} content line-by-line into a {@link String} which
     * can then be written into a Stunnel config file to start the program.
     * @return
     */
    protected String getConfigFileContent() {
        StringBuilder str = new StringBuilder();

        //write the static config first
        for (String line : getStaticConfig()) {
            str.append(line).append('\n');
        }
        //now write the user defined config
        for (String line : config) {
            str.append(line).append('\n');
        }

        return str.toString();
    }

    /**
     * Returns an empty config file in a newly temporary directory exclusive to this {@link StunnelBuilder}.
     * @return
     */
    protected File newConfigFile() {
        return newConfigFile(false);
    }

    /**
     * Returns an empty config file in a newly temporary directory exclusive to this {@link StunnelBuilder}.
     * @param isMainFile For debugging mostly.
     *                   True: is the main Stunnel config file (includes services
     *                   with TLS settings etc).
     *                   False: is a secondary config file referenced by the main config file
     *                   (e.g. for PSKSecrects)
     * @return
     */
    protected File newConfigFile(boolean isMainFile) {
        File file;

        if(isMainFile)
            file = new File(getTemporaryDirectory(), "stunnel.conf");
        else
            file = new File(getTemporaryDirectory(), UUID.randomUUID().toString());
        file.deleteOnExit();

        return file;
    }

    /**
     * Creates an empty temporary directory for {@link #newConfigFile()}.
     * @return
     */
    protected final File getTemporaryDirectory() {
        if(tempDirectory == null) {
            File baseDirectory = getTemporaryBaseDirectory();

            synchronized (housekeepingCleanedTempDirectory) {
                if(!housekeepingCleanedTempDirectory.getAndSet(true)) {
                    //clean up old config files
                    deleteDirectory(baseDirectory);
                }
            }

            tempDirectory = new File(baseDirectory, UUID.randomUUID().toString());
            tempDirectory.mkdirs();
            //try to delete on exit
            tempDirectory.deleteOnExit();
        }

        return tempDirectory;
    }

    /**
     * Returns the base directory where each unique Stunnel process will get its own
     * temporary directory via {@link #getTemporaryDirectory()}.
     * @return
     */
    protected File getTemporaryBaseDirectory() {
        return new File(context.getFilesDir(), "stunnel/tmp");
    }

    /**
     * Deletes the given file or directory by recursively deleting all files in the directory if any.
     * @param directory
     */
    protected static void deleteDirectory(@Nullable File directory) {
        if(directory == null)
            return;
        else if(directory.isDirectory()) {
            //recursively delete the directory's content first
            File[] children = directory.listFiles();
            if(children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        directory.delete();
    }

    //</editor-fold>

    //<editor-fold desc="Services">
    /**
     * Adds a Stunnel service.
     * @param name Mostly for logging/debugging purposes
     * @return
     */
    public ServiceBuilder addService(@NonNull String name) {
        return new ServiceBuilder(name);
    }

    /**
     * Same as {@link #addService(String)} but uses a random name.
     * @return
     */
    public ServiceBuilder addService() {
        return addService(UUID.randomUUID().toString());
    }

    /**
     * Builder for a Stunnel service.<br/>
     * At the very least you should take a look at {@link #client(boolean)},
     * {@link #accept(int)} and {@link #connect(String, int)} to get started.<br/>
     * You need to call {@link #apply()} once you are done setting up the service's settings.<br/><br/>
     * Other methods of note:
     * <ul>
     *     <li>{@link #delay(boolean)} is required when using a dynamic DNS hostname with {@link #connect(String, int)}</li>
     * </ul>
     * @// TODO: 20.10.2019 Do we need to add service definitions after the global settings?
     */
    public class ServiceBuilder extends StunnelConfigBuilder<ServiceBuilder> {
        protected static final String KEY_SERVICE_ACCEPT = "accept";
        protected static final String KEY_SERVICE_CLIENT = "client";
        protected static final String KEY_SERVICE_CONNECT = "connect";
        protected static final String KEY_SERVICE_CIPHERS = "ciphers";
        protected static final String KEY_SERVICE_CIPHERSUITES = "ciphersuites";
        protected static final String KEY_SERVICE_DELAY = "delay";
        protected static final String KEY_SERVICE_PSK_IDENTITY = "PSKidentity";
        protected static final String KEY_SERVICE_PSK_SECRETS = "PSKsecrets";
        protected static final String KEY_SERVICE_SSL_VERSION = "sslVersion";
        protected static final String KEY_SERVICE_SSL_VERSION_MAX = "sslVersionMax";
        protected static final String KEY_SERVICE_SSL_VERSION_MIN = "sslVersionMin";

        @NonNull protected final String name;

        protected ServiceBuilder(@NonNull String name) {
            this.name = name;
        }

        /**
         * <pre>{@code
         *

        accept connections on specified address

        If no host specified, defaults to all IPv4 addresses for the local host.

        To listen on all IPv6 addresses use:

        accept = :::PORT


         * }</pre>
         * @param host Most likely you'll want to pass "localhost" to discard connections from other devices
         *             to your Stunnel. See also {@link #acceptLocal(int)}
         * @param port
         * @return
         */
        public ServiceBuilder accept(@Nullable String host, int port) {
            String value = "";
            if(host != null && !host.isEmpty())
                value += host + ":";
            value += port;

            return setOption(KEY_SERVICE_ACCEPT, value);
        }

        /**
         * Calls {@link #accept(String, int)} with an empty host to listen to all IPv4 addresses.
         * @param port
         * @return
         */
        public ServiceBuilder accept(int port) {
            return accept(null, port);
        }

        /**
         * Calls {@link #accept(String, int)} with an empty host to listen to all IPv6 addresses.
         * @param port
         * @return
         */
        public ServiceBuilder acceptIPv6(int port) {
            return accept("::", port);
        }

        /**
         * Calls {@link #accept(String, int)} and allows connection only from the local device.
         * @param port
         * @return
         */
        public ServiceBuilder acceptLocal(int port) {
            return accept("localhost", port);
        }

        /**
         *
         * @param ciphers
         * @return
         * @see #ciphers(SSLCipher...)
         */
        public ServiceBuilder ciphers(List<SSLCipher> ciphers) {
            return setOption(new StunnelOption<>(KEY_SERVICE_CIPHERS, new StunnelList<>(ciphers)));
        }

        /**
         * <pre>{@code
         *

        select permitted TLS ciphers (TLSv1.2 and below)

        This option does not impact TLSv1.3 ciphersuites.

        A colon-delimited list of the ciphers to allow in the TLS connection, for example DES-CBC3-SHA:IDEA-CBC-MD5.

         * }</pre>
         * @param ciphers
         * @return
         * @see #ciphers(List)
         */
        public ServiceBuilder ciphers(SSLCipher... ciphers) {
            return ciphers(Arrays.asList(ciphers));
        }

        /**
         * <pre>{@code
         *

        select permitted TLSv1.3 ciphersuites

        A colon-delimited list of TLSv1.3 ciphersuites names in order of preference.

        This option requires OpenSSL 1.1.1 or later.

        default: TLS_CHACHA20_POLY1305_SHA256:TLS_AES_256_GCM_SHA384:TLS_AES_128_GCM_SHA256

         * }</pre>
         * @param ciphersuites
         * @return
         */
        public ServiceBuilder ciphersuites(List<TLS1_3CipherSuite> ciphersuites) {
            return setOption(new StunnelOption<>(KEY_SERVICE_CIPHERSUITES, new StunnelList<>(ciphersuites)));
        }

        /**
         *
         * @param ciphersuites
         * @return
         * @see #ciphersuites(List)
         */
        public ServiceBuilder ciphersuites(TLS1_3CipherSuite... ciphersuites) {
            return ciphersuites(Arrays.asList(ciphersuites));
        }

        /**
         * <pre>{@code
         *

        client mode (remote service uses TLS)

        default: no (server mode)

         * }</pre>
         * @param isClient
         * @return
         */
        public ServiceBuilder client(boolean isClient) {
            return setOption(new StunnelOption<>(KEY_SERVICE_CLIENT, new StunnelBoolean(isClient)));
        }

        /**
         * <pre>{@code
         *

        connect to a remote address

        If no host is specified, the host defaults to localhost.

        Multiple connect options are allowed in a single service section.

        If host resolves to multiple addresses and/or if multiple connect options are specified, then the remote address is chosen using a round-robin algorithm.

         * }</pre>
         * @param host
         * @param port
         * @return
         */
        public ServiceBuilder connect(@Nullable String host, int port) {
            String value = "";
            if(host != null && !host.isEmpty())
                value += host + ":";
            value += port;

            return setOption(KEY_SERVICE_CONNECT, value);
        }

        /**
         * Calls {@link #connect(String, int)} with an empty host to listen to connect to localhost.
         * @param port
         * @return
         */
        public ServiceBuilder connect(int port) {
            return connect(null, port);
        }

        /**
         * <pre>{@code
         *

        delay DNS lookup for the connect option

        This option is useful for dynamic DNS, or when DNS is not available during stunnel startup (road warrior VPN, dial-up configurations).

        Delayed resolver mode is automatically engaged when stunnel fails to resolve on startup any of the connect targets for a service.

        Delayed resolver inflicts failover = prio.

        default: no

         * }</pre>
         * @param delay
         * @return
         */
        public ServiceBuilder delay(boolean delay) {
            return setOption(new StunnelOption<>(KEY_SERVICE_DELAY, new StunnelBoolean(delay)));
        }

        /**
         * <pre>{@code
         *

        PSK identity for the PSK client

        PSKidentity can be used on stunnel clients to select the PSK identity used for authentication. This option is ignored in server sections.

        default: the first identity specified in the PSKsecrets file.

         * }</pre>
         * @param identity
         * @return
         */
        public ServiceBuilder pskIdentity(String identity) {
            return setOption(KEY_SERVICE_PSK_IDENTITY, identity);
        }

        /**
         * <pre>{@code
         *

        file with PSK identities and corresponding keys

        Each line of the file in the following format:

        IDENTITY:KEY

        Hexadecimal keys are automatically converted to binary form. Keys are required to be at least 16 bytes long, which implies at least 32 characters for hexadecimal keys. The file should neither be world-readable nor world-writable.

         * }</pre>
         * @param pskSecrets
         * @return
         * @throws IOException When there are errors writing the required config file to disk.
         * @see #pskIdentity(String)
         */
        public ServiceBuilder pskSecrets(List<PreSharedKey> pskSecrets) throws IOException {
            File file = newConfigFile();

            Writer writer = null;
            try {
                //we must NOT use a BOM here
                writer = new FileWriter(file);
                writer.write(PreSharedKey.makePskSecretsFile(pskSecrets));
                Util.close(writer);
            }
            catch(IOException e) {
                Util.close(writer);
                //delete the temporary file
                deleteDirectory(file);
                throw e;
            }

            return setOption(KEY_SERVICE_PSK_SECRETS, file.getAbsolutePath());
        }

        /**
         *
         * @param pskSecrets
         * @return
         * @throws IOException
         * @see #pskSecrets(List)
         */
        public ServiceBuilder pskSecrets(PreSharedKey... pskSecrets) throws IOException {
            return pskSecrets(Arrays.asList(pskSecrets));
        }

        /**
         * <pre>{@code
         *

        select the TLS protocol version

        Supported versions: all, SSLv2, SSLv3, TLSv1, TLSv1.1, TLSv1.2, TLSv1.3

        Availability of specific protocols depends on the linked OpenSSL library. Older versions of OpenSSL do not support TLSv1.1, TLSv1.2 and TLSv1.3. Newer versions of OpenSSL do not support SSLv2.

        Obsolete SSLv2 and SSLv3 are currently disabled by default.

        Setting the option

        sslVersion = SSL_VERSION

        is equivalent to options

        sslVersionMax = SSL_VERSION
        sslVersionMin = SSL_VERSION

        when compiled with OpenSSL 1.1.0 and later.

         * }</pre>
         * @param sslVersion
         * @return
         */
        public ServiceBuilder sslVersion(@NonNull SSLVersion sslVersion) {
            return setOption(KEY_SERVICE_SSL_VERSION, sslVersion.toStunnelValue());
        }

        /**
         * <pre>{@code
         *

        maximum supported protocol versions

        Supported versions: all, SSLv3, TLSv1, TLSv1.1, TLSv1.2, TLSv1.3

        all enable protocol versions up to the highest version supported by the linked OpenSSL library.

        Availability of specific protocols depends on the linked OpenSSL library.

        The sslVersionMax option is only available when compiled with OpenSSL 1.1.0 and later.

        default: all

         * }</pre>
         * @param sslVersion
         * @return
         */
        public ServiceBuilder sslVersionMax(@NonNull SSLVersion sslVersion) {
            return setOption(KEY_SERVICE_SSL_VERSION_MAX, sslVersion.toStunnelValue());
        }

        /**
         * <pre>{@code
         *

        minimum supported protocol versions

        Supported versions: all, SSLv3, TLSv1, TLSv1.1, TLSv1.2, TLSv1.3

        all enable protocol versions down to the lowest version supported by the linked OpenSSL library.

        Availability of specific protocols depends on the linked OpenSSL library.

        The sslVersionMin option is only available when compiled with OpenSSL 1.1.0 and later.

        default: TLSv1

         * }</pre>
         * @param sslVersion
         * @return
         */
        public ServiceBuilder sslVersionMin(@NonNull SSLVersion sslVersion) {
            return setOption(KEY_SERVICE_SSL_VERSION_MIN, sslVersion.toStunnelValue());
        }

        /**
         * Finalizes this Stunnel service and returns the original {@link StunnelBuilder}.
         * @return
         */
        public StunnelBuilder apply() {
            //write the service name
            StunnelBuilder.this.addConfig(makeServiceName());

            //write the service settings
            for (String line : config) {
                StunnelBuilder.this.addConfig(line);
            }

            return StunnelBuilder.this;
        }

        /**
         * Returns the service's {@link #name} as it needs to be written into the Stunnel config file.
         * @return
         */
        protected String makeServiceName() {
            //TODO escaping?
            return "[" + name + "]";
        }
    }
    //</editor-fold>
}
