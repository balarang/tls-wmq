
import java.security.KeyStore;
import java.util.ResourceBundle;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class WMQWriter {
    private ResourceBundle res = null;

    public static void main( String[] args ) throws Exception {
        WMQWriter writer = new WMQWriter();
        writer.writeToQueue();
    }

    public void writeToQueue() throws Exception {
        loadProperties();

        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setTransportType( WMQConstants.WMQ_CM_CLIENT );
        factory.setQueueManager( res.getString( "wmq.queuemanager" ) );
        factory.setHostName( res.getString( "wmq.host" ) );
        factory.setPort( Integer.parseInt( res.getString( "wmq.port" ) ) );
        factory.setChannel( res.getString( "wmq.channel" ) );

        SSLContext ctx = SSLContext.getInstance( res.getString( "wmq.tls.protocol" ) );
        ctx.init( getKeyManagers(), getTrustManagers(), null );
        SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();

        // List of supported CipherSuites
        // http://129.33.205.81/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.dev.doc/q113220_.htm?lang=en
        factory.setSSLCipherSuite( res.getString( "wmq.tls.ciphersuite" ) );
        factory.setSSLSocketFactory( sslSocketFactory );

        // Connect to queue manager using credentials
        Connection conn = factory.createConnection( res.getString( "wmq.user" ), res.getString( "wmq.password" ) );
        Session session = conn.createSession( true, Session.AUTO_ACKNOWLEDGE );
        Queue q = session.createQueue( res.getString( "wmq.queue" ) );
        MessageProducer sender = session.createProducer( q );
        System.out.println( "WMQ Session created" );

        // Send a test message to queue
        for ( int i = 0; i < 2; i++ ) {
            TextMessage msg = session.createTextMessage();
            msg.setText( "My message body " + i );

            sender.send( msg );
            session.commit();
        }
        System.out.println( "Messages sent. closing session" );
        session.close();
        sender.close();
    }

    private void loadProperties() {
        res = ResourceBundle.getBundle( "wmq" );
    }

    //Get KeyManagers from key store
    private KeyManager[] getKeyManagers() throws Exception {
        KeyManager[] keyManagers = null;
        KeyStore keyStore = KeyStore.getInstance( res.getString( "wmq.tls.keystoretype" ) );
        char[] pwd = res.getString( "wmq.tls.keystorepass" ).toCharArray();
        String alias = res.getString( "wmq.tls.keystorealias" );
        String keystoreFileName = res.getString( "wmq.tls.keystorefile" );
        keyStore.load( new FileInputStream( keystoreFileName ), pwd );
        if ( keyStore != null ) {
            KeyManagerFactory factory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
            keyManagers = factory.getKeyManagers();
        }
        return keyManagers;
    }

    //Get KeyManagers from trust store
    private TrustManager[] getTrustManagers() throws Exception {
        TrustManager[] trustManagers = null;
        KeyStore trustStore = KeyStore.getInstance( res.getString( "wmq.tls.truststoretype" ) );
        char[] pwd = res.getString( "wmq.tls.truststorepass" ).toCharArray();
        String keystoreFileName = res.getString( "wmq.tls.truststorefile" );
        trustStore.load( new FileInputStream( keystoreFileName ), pwd );
        if ( trustStore != null ) {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            trustManagerFactory.init( trustStore );
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        return trustManagers;
    }
}
